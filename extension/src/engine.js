// Pure session logic, shared by the content script and the tests. No DOM access.
(function (root) {
  const LIMITS = { intervalMin: 3, intervalMax: 120, durationMin: 15, durationMax: 240, scrollsMax: 10000 };

  const DEFAULTS = {
    mode: "videoEnd", // "videoEnd" | "timer"
    intervalSec: 10,
    durationMin: 30,
    maxScrolls: 0, // 0 = no cap
    tapSkip: false, // opt-in: click YouTube's own Skip button
    skipAdShorts: false, // opt-in: swipe past an ad Short (never clicks the ad)
  };

  const clamp = (n, lo, hi) => Math.min(hi, Math.max(lo, n));

  function sanitize(raw) {
    const s = Object.assign({}, DEFAULTS, raw || {});
    s.mode = s.mode === "timer" ? "timer" : "videoEnd";
    s.intervalSec = clamp(Math.round(Number(s.intervalSec) || DEFAULTS.intervalSec), LIMITS.intervalMin, LIMITS.intervalMax);
    s.durationMin = clamp(Math.round(Number(s.durationMin) || DEFAULTS.durationMin), LIMITS.durationMin, LIMITS.durationMax);
    s.maxScrolls = clamp(Math.round(Number(s.maxScrolls) || 0), 0, LIMITS.scrollsMax);
    s.tapSkip = s.tapSkip === true;
    s.skipAdShorts = s.skipAdShorts === true;
    return s;
  }

  // Decides what a running session should do on one tick.
  // ctx: { now, startedAt, settings, scrolls, lastAdvanceAt, paused, ended, hasTimeLeftOnVideo }
  // returns "stop-duration" | "stop-scrolls" | "wait" | "advance"
  function decide(ctx) {
    const s = ctx.settings;
    if (ctx.now - ctx.startedAt >= s.durationMin * 60000) return "stop-duration";
    if (s.maxScrolls > 0 && ctx.scrolls >= s.maxScrolls) return "stop-scrolls";
    if (ctx.paused) return "wait";
    if (s.mode === "timer") return ctx.now - ctx.lastAdvanceAt >= s.intervalSec * 1000 ? "advance" : "wait";
    return ctx.ended ? "advance" : "wait";
  }

  const api = { LIMITS, DEFAULTS, sanitize, decide };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.SwipeSkipEngine = api;
})(typeof self !== "undefined" ? self : this);
