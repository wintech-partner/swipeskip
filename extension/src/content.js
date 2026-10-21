// SwipeSkip content script: runs a bounded, visible, user-started session on youtube.com.
// It reads no page text, stores no history and makes no network requests.
(function () {
  const ext = typeof browser !== "undefined" ? browser : chrome;
  const E = self.SwipeSkipEngine;
  const TICK_MS = 250;
  const USER_GRACE_MS = 8000;
  const AWAY_END_MS = 60000;
  const SKIP_MAX_TAPS = 3;
  const SKIP_GAP_MS = 2000;
  const SKIP_SELECTOR = ".ytp-skip-ad-button, .ytp-ad-skip-button-modern, .ytp-ad-skip-button, .ytp-skip-ad-button__text";

  let session = null;
  let badge = null;

  const onShorts = () => location.pathname.startsWith("/shorts/");

  function activeVideo() {
    return (
      document.querySelector("ytd-reel-video-renderer[is-active] video") ||
      document.querySelector("#shorts-player video") ||
      document.querySelector("video.html5-main-video") ||
      document.querySelector("video")
    );
  }

  function adLikely() {
    const root = document.querySelector("ytd-reel-video-renderer[is-active]") || document;
    return !!(root.querySelector("ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer, .ytp-ad-player-overlay-layout") ||
      document.querySelector(".ad-showing"));
  }

  function commentsOpen() {
    return !!document.querySelector('ytd-engagement-panel-section-list-renderer[visibility="ENGAGEMENT_PANEL_VISIBILITY_EXPANDED"]');
  }

  function pauseReason(now) {
    if (!onShorts()) return "notShorts";
    if (document.hidden) return "hidden";
    if (now < session.userUntil) return "touched";
    if (commentsOpen()) return "comments";
    if (adLikely()) return "ad";
    const v = activeVideo();
    if (!v || v.paused) return "videoPaused";
    return null;
  }

  function videoEnded(v) {
    if (!v || !(v.duration > 0) || !isFinite(v.duration)) return false;
    const t = v.currentTime;
    const wrapped = session.lastTime > v.duration * 0.6 && t < session.lastTime - 0.5;
    session.lastTime = t;
    return wrapped || t >= v.duration - 0.25 || v.ended;
  }

  function goNext() {
    const before = location.href;
    const btn = document.querySelector("#navigation-button-down button, #navigation-button-down ytd-button-renderer button");
    if (btn) btn.click();
    else document.dispatchEvent(new KeyboardEvent("keydown", { key: "ArrowDown", code: "ArrowDown", keyCode: 40, bubbles: true }));
    session.scrolls += 1;
    session.lastAdvanceAt = Date.now();
    session.lastTime = 0;
    setTimeout(() => {
      if (session && location.href === before && onShorts()) {
        const c = document.querySelector("#shorts-container");
        if (c && c.scrollBy) c.scrollBy({ top: c.clientHeight, behavior: "smooth" });
      }
    }, 1200);
  }

  function maybeTapSkip(now) {
    if (!session.settings.tapSkip) return;
    const btn = document.querySelector(SKIP_SELECTOR);
    if (!btn) { session.skipTaps = 0; return; }
    if (session.skipTaps >= SKIP_MAX_TAPS || now - session.lastSkipAt < SKIP_GAP_MS) return;
    const target = btn.closest("button") || btn;
    if (target.offsetParent === null) return;
    target.click();
    session.skipTaps += 1;
    session.lastSkipAt = now;
  }

  function tick() {
    if (!session) return;
    const now = Date.now();
    maybeTapSkip(now);
    if (onShorts()) session.lastOnShorts = now;
    else if (now - session.lastOnShorts >= AWAY_END_MS) return stop("away");
    const reason = pauseReason(now);
    session.pause = reason;
    const v = activeVideo();
    const verdict = E.decide({
      now, startedAt: session.startedAt, settings: session.settings, scrolls: session.scrolls,
      lastAdvanceAt: session.lastAdvanceAt, paused: reason !== null,
      ended: reason === null && videoEnded(v),
    });
    if (verdict === "stop-duration" || verdict === "stop-scrolls") return stop(verdict);
    if (verdict === "advance") goNext();
    renderBadge();
  }

  function start(raw) {
    stop(null);
    const now = Date.now();
    session = {
      settings: E.sanitize(raw), startedAt: now, lastAdvanceAt: now, lastOnShorts: now,
      scrolls: 0, lastTime: 0, userUntil: 0, pause: null, skipTaps: 0, lastSkipAt: 0, timer: null, endReason: null,
    };
    session.timer = setInterval(tick, TICK_MS);
    renderBadge();
  }

  function stop(reason) {
    if (!session) return;
    clearInterval(session.timer);
    session = null;
    if (badge) { badge.remove(); badge = null; }
    try { ext.runtime.sendMessage({ type: "ended", reason }).catch(() => {}); } catch (_) { /* popup closed */ }
    lastEnd = reason;
  }
  let lastEnd = null;

  function status() {
    if (!session) return { running: false, endReason: lastEnd };
    const left = Math.max(0, Math.round((session.startedAt + session.settings.durationMin * 60000 - Date.now()) / 1000));
    return { running: true, pause: session.pause, scrolls: session.scrolls, remainingSec: left, onShorts: onShorts() };
  }

  // Visible indicator with Stop, shown for the whole session.
  function renderBadge() {
    if (!session) return;
    if (!badge) {
      badge = document.createElement("div");
      badge.setAttribute("data-swipeskip", "1");
      badge.style.cssText = "position:fixed;left:12px;bottom:12px;z-index:2147483647;display:flex;gap:8px;align-items:center;" +
        "background:#1a56db;color:#fff;font:600 13px system-ui,sans-serif;padding:6px 10px;border-radius:16px;box-shadow:0 2px 8px #0006";
      const label = document.createElement("span");
      const b = document.createElement("button");
      b.textContent = ext.i18n.getMessage("stop");
      b.style.cssText = "all:unset;cursor:pointer;background:#fff;color:#1a56db;border-radius:10px;padding:2px 8px";
      b.addEventListener("click", () => stop("user"));
      badge.append(label, b);
      document.documentElement.appendChild(badge);
    }
    const s = status();
    badge.firstChild.textContent = "SwipeSkip · " + Math.ceil(s.remainingSec / 60) + " min · " + s.scrolls + (s.pause ? " · ⏸" : "");
  }

  // Only real user input (isTrusted) pauses; our own synthetic key events are ignored.
  ["wheel", "keydown", "pointerdown", "touchstart"].forEach((t) =>
    window.addEventListener(t, (e) => {
      if (session && e.isTrusted && !(badge && badge.contains(e.target))) session.userUntil = Date.now() + USER_GRACE_MS;
    }, { capture: true, passive: true })
  );

  ext.runtime.onMessage.addListener((msg, _sender, reply) => {
    if (msg && msg.type === "start") { start(msg.settings); reply(status()); }
    else if (msg && msg.type === "stop") { stop("user"); reply(status()); }
    else if (msg && msg.type === "status") reply(status());
  });
})();
