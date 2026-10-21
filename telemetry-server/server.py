"""SwipeSkip usage-stats collector (Chrome build, opt-in).

Stores only per-day counters for each field separately (never a joint record, never an
IP, never a user id). Anything outside the allowlists below is rejected.
"""
import json
import os
import re
import socket
import sqlite3
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DB_PATH = os.environ.get("DB_PATH", "/data/stats.db")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "")
MAX_BODY = 512
RATE_PER_SEC = 20  # global, all senders together
SOCKET_TIMEOUT = 5  # seconds; a slow-read client is dropped instead of holding a thread
MAX_CONCURRENT = 32
slots = threading.BoundedSemaphore(MAX_CONCURRENT)

REASONS = {"user", "stop-duration", "stop-scrolls", "away"}
MODES = {"videoEnd", "timer"}
KINDS = {"next_button_missing", "next_fallback_used", "video_not_found", "skip_not_clickable"}
BROWSERS = {"chrome", "edge", "other"}
VERSION_RE = re.compile(r"^\d{1,3}\.\d{1,3}\.\d{1,3}$")
LOCALE_RE = re.compile(r"^[a-z]{2,3}(-[A-Za-z]{2,4})?$")
DURATIONS = {15, 30, 60, 120, 240}


def bucket_scrolls(n):
    for hi, label in ((0, "0"), (9, "1-9"), (49, "10-49"), (199, "50-199")):
        if n <= hi:
            return label
    return "200+"


def validate(doc):
    """Returns a list of (field, value) pairs to count, or None if the event is invalid."""
    if not isinstance(doc, dict) or doc.get("v") != 1:
        return None
    ev, ext, browser, locale = doc.get("ev"), doc.get("ext"), doc.get("browser"), doc.get("locale")
    data = doc.get("data")
    if not (isinstance(ext, str) and VERSION_RE.match(ext)) or browser not in BROWSERS:
        return None
    if not (isinstance(locale, str) and LOCALE_RE.match(locale)) or not isinstance(data, dict):
        return None
    common = [("ext", ext), ("browser", browser), ("locale", locale)]
    if ev == "breakage":
        if set(data) != {"kind"} or data["kind"] not in KINDS:
            return None
        return [("breakage", data["kind"])] + common
    if ev == "session_end":
        if set(data) != {"reason", "mode", "durationMin", "scrolls", "tapSkip"}:
            return None
        scrolls = data["scrolls"]
        if data["reason"] not in REASONS or data["mode"] not in MODES or data["durationMin"] not in DURATIONS:
            return None
        if not (isinstance(scrolls, int) and not isinstance(scrolls, bool) and 0 <= scrolls <= 10000):
            return None
        if not isinstance(data["tapSkip"], bool):
            return None
        return [("end_reason", data["reason"]), ("mode", data["mode"]), ("duration", str(data["durationMin"])),
                ("scrolls", bucket_scrolls(scrolls)), ("tapSkip", str(data["tapSkip"]).lower())] + common
    return None


class Store:
    def __init__(self, path):
        os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
        self.db = sqlite3.connect(path, check_same_thread=False)
        self.db.execute("CREATE TABLE IF NOT EXISTS counts (day TEXT, field TEXT, value TEXT, n INTEGER, PRIMARY KEY (day, field, value))")
        self.lock = threading.Lock()

    def add(self, pairs, day=None):
        day = day or datetime.now(timezone.utc).strftime("%Y-%m-%d")
        with self.lock, self.db:
            for field, value in pairs:
                self.db.execute(
                    "INSERT INTO counts VALUES (?,?,?,1) ON CONFLICT(day, field, value) DO UPDATE SET n = n + 1",
                    (day, field, value))

    def summary(self, days):
        with self.lock:
            rows = self.db.execute(
                "SELECT field, value, SUM(n) FROM counts WHERE day >= date('now', ?) GROUP BY field, value ORDER BY field, 3 DESC",
                (f"-{int(days)} days",)).fetchall()
        out = {}
        for field, value, n in rows:
            out.setdefault(field, {})[value] = n
        return out


class Limiter:
    def __init__(self, per_sec):
        self.per_sec, self.tokens, self.last, self.lock = per_sec, float(per_sec), time.monotonic(), threading.Lock()

    def allow(self):
        with self.lock:
            now = time.monotonic()
            self.tokens = min(self.per_sec, self.tokens + (now - self.last) * self.per_sec)
            self.last = now
            if self.tokens >= 1:
                self.tokens -= 1
                return True
            return False


def make_handler(store, limiter):
    class H(BaseHTTPRequestHandler):
        timeout = SOCKET_TIMEOUT  # applied to the socket by StreamRequestHandler

        def handle(self):
            if not slots.acquire(blocking=False):  # too many open connections: drop
                return
            try:
                super().handle()
            finally:
                slots.release()

        def log_message(self, *_):  # no access log: it would contain client addresses
            pass

        def _send(self, code, body=b"", ctype="application/json"):
            self.send_response(code)
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(body)

        def do_POST(self):
            if self.path != "/v1/event":
                return self._send(404)
            if not limiter.allow():
                return self._send(429)
            try:
                length = int(self.headers.get("Content-Length", "0"))
            except ValueError:
                return self._send(400)
            if length <= 0 or length > MAX_BODY:
                return self._send(413 if length > MAX_BODY else 400)
            try:
                pairs = validate(json.loads(self.rfile.read(length)))
            except (socket.timeout, TimeoutError):
                return self._send(408)
            except (ValueError, UnicodeDecodeError):
                pairs = None
            if pairs is None:
                return self._send(400)
            store.add(pairs)
            self._send(204)

        def do_GET(self):
            if self.path == "/healthz":
                return self._send(200, b"ok", "text/plain")
            if self.path.split("?")[0] == "/stats" and ADMIN_TOKEN and \
                    self.headers.get("Authorization") == f"Bearer {ADMIN_TOKEN}":
                m = re.search(r"days=(\d{1,3})", self.path)
                return self._send(200, json.dumps(store.summary(int(m.group(1)) if m else 7), indent=1).encode())
            self._send(404)

    return H


if __name__ == "__main__":
    srv = ThreadingHTTPServer(("0.0.0.0", int(os.environ.get("PORT", "8000"))), make_handler(Store(DB_PATH), Limiter(RATE_PER_SEC)))
    srv.serve_forever()
