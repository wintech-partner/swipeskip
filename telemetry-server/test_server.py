import json, tempfile, threading, unittest, urllib.request, urllib.error, os
from http.server import ThreadingHTTPServer
import server

OK_END = {"v": 1, "ev": "session_end", "ext": "0.2.0", "browser": "chrome", "locale": "tr",
          "data": {"reason": "user", "mode": "timer", "durationMin": 30, "scrolls": 12, "tapSkip": False}}
OK_BREAK = {"v": 1, "ev": "breakage", "ext": "0.2.0", "browser": "chrome", "locale": "en-US", "data": {"kind": "next_button_missing"}}


class T(unittest.TestCase):
    def test_validate_accepts_and_buckets(self):
        pairs = dict(server.validate(OK_END))
        self.assertEqual(pairs["scrolls"], "10-49")
        self.assertEqual(pairs["tapSkip"], "false")

    def test_validate_rejects_extra_or_bad_fields(self):
        bad = json.loads(json.dumps(OK_END)); bad["data"]["url"] = "https://youtube.com/shorts/abc"
        self.assertIsNone(server.validate(bad))
        bad = json.loads(json.dumps(OK_END)); bad["data"]["reason"] = "x"
        self.assertIsNone(server.validate(bad))
        bad = json.loads(json.dumps(OK_END)); bad["data"]["scrolls"] = True
        self.assertIsNone(server.validate(bad))
        bad = dict(OK_BREAK, locale="en; DROP TABLE")
        self.assertIsNone(server.validate(bad))
        self.assertIsNone(server.validate("x"))

    def test_http_roundtrip(self):
        server.ADMIN_TOKEN = "tok"
        store = server.Store(os.path.join(tempfile.mkdtemp(), "s.db"))
        srv = ThreadingHTTPServer(("127.0.0.1", 0), server.make_handler(store, server.Limiter(1000)))
        threading.Thread(target=srv.serve_forever, daemon=True).start()
        base = f"http://127.0.0.1:{srv.server_port}"

        def post(doc):
            req = urllib.request.Request(base + "/v1/event", json.dumps(doc).encode(), {"Content-Type": "application/json"})
            try:
                return urllib.request.urlopen(req).status
            except urllib.error.HTTPError as e:
                return e.code
        self.assertEqual(post(OK_END), 204)
        self.assertEqual(post(OK_END), 204)
        self.assertEqual(post(OK_BREAK), 204)
        self.assertEqual(post({"v": 1}), 400)
        big = urllib.request.Request(base + "/v1/event", b"x" * 600)
        with self.assertRaises(urllib.error.HTTPError) as c:
            urllib.request.urlopen(big)
        self.assertEqual(c.exception.code, 413)
        with self.assertRaises(urllib.error.HTTPError) as c:
            urllib.request.urlopen(base + "/stats")
        self.assertEqual(c.exception.code, 404)
        req = urllib.request.Request(base + "/stats", headers={"Authorization": "Bearer tok"})
        stats = json.load(urllib.request.urlopen(req))
        self.assertEqual(stats["end_reason"], {"user": 2})
        self.assertEqual(stats["breakage"], {"next_button_missing": 1})
        # slow-read client: promises a body, sends none; must be cut off with 408 within the timeout
        import socket as so, time
        server.SOCKET_TIMEOUT  # exists
        c = so.create_connection(("127.0.0.1", srv.server_port))
        c.sendall(b"POST /v1/event HTTP/1.1\r\nHost: x\r\nContent-Length: 100\r\n\r\n")
        c.settimeout(server.SOCKET_TIMEOUT + 3)
        t0 = time.time()
        self.assertIn(b" 408 ", c.recv(200))
        self.assertLess(time.time() - t0, server.SOCKET_TIMEOUT + 2)
        c.close()
        # trickle client: one byte every second never trips the per-read timeout, but must hit the absolute deadline
        server.DEADLINE = 3
        c = so.create_connection(("127.0.0.1", srv.server_port))
        c.sendall(b"POST /v1/event HTTP/1.1\r\nHost: x\r\nContent-Length: 100\r\n\r\n")
        t0 = time.time(); closed = False
        while time.time() - t0 < 8:
            try:
                c.sendall(b"x"); time.sleep(1)
            except OSError:
                closed = True; break
        self.assertTrue(closed and time.time() - t0 < 6)
        c.close()
        srv.shutdown()


unittest.main()
