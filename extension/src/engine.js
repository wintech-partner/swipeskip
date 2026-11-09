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

  // Skip-button labels in the 16 UI languages of the Android app (same list as YouTubeMatchers.kt). Fallback only: the
  // CSS classes are tried first. Whole-string match after trimming, so a creator's text containing "skip ad" never counts.
  // Only German was ever seen on a device; the rest are unverified.
  const SKIP_LABELS = new Set([
    "skip ad", "skip ads", "skip", // en
    "reklamı atla", "atla", // tr
    "anzeige überspringen", "werbung überspringen", "überspringen", // de
    "omitir anuncio", "omitir", // es
    "passer l'annonce", "ignorer l'annonce", "passer", // fr
    "pular anúncio", "pular", // pt-BR
    "пропустить рекламу", "пропустить", // ru
    "تخطي الإعلان", "تخطي", // ar
    "lewati iklan", "lewati", // id
    "विज्ञापन छोड़ें", "छोड़ें", // hi
    "広告をスキップ", "スキップ", // ja
    "광고 건너뛰기", "건너뛰기", // ko
    "bỏ qua quảng cáo", "bỏ qua", // vi
    "ข้ามโฆษณา", "ข้าม", // th
    "i-skip ang ad", "i-skip", // fil
    "বিজ্ঞাপন এড়িয়ে যান", "এড়িয়ে যান", // bn
    "اشتہار چھوڑیں", "چھوڑیں", // ur
  ]);

  function isSkipLabel(text) {
    if (typeof text !== "string") return false;
    return SKIP_LABELS.has(text.replace(/\s+/g, " ").trim().toLowerCase());
  }

  const api = { LIMITS, DEFAULTS, sanitize, decide, SKIP_LABELS, isSkipLabel };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.SwipeSkipEngine = api;
})(typeof self !== "undefined" ? self : this);
