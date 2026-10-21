const assert = require("node:assert/strict");
const { test } = require("node:test");
const E = require("../src/engine.js");

const base = () => ({ now: 1000, startedAt: 0, settings: E.sanitize({}), scrolls: 0, lastAdvanceAt: 0, paused: false, ended: false });

test("sanitize clamps and defaults", () => {
  const s = E.sanitize({ intervalSec: 1, durationMin: 9999, maxScrolls: -5, mode: "x", tapSkip: "yes" });
  assert.deepEqual(s, { mode: "videoEnd", intervalSec: 3, durationMin: 240, maxScrolls: 0, tapSkip: false });
});
test("tapSkip is off by default", () => assert.equal(E.sanitize().tapSkip, false));
test("videoEnd advances only when the video ended", () => {
  assert.equal(E.decide(base()), "wait");
  assert.equal(E.decide({ ...base(), ended: true }), "advance");
});
test("timer advances after the interval", () => {
  const settings = E.sanitize({ mode: "timer", intervalSec: 5 });
  assert.equal(E.decide({ ...base(), settings, now: 4999 }), "wait");
  assert.equal(E.decide({ ...base(), settings, now: 5000 }), "advance");
});
test("paused never advances", () => {
  assert.equal(E.decide({ ...base(), ended: true, paused: true }), "wait");
});
test("limits stop the session even when paused", () => {
  assert.equal(E.decide({ ...base(), paused: true, now: 30 * 60000 }), "stop-duration");
  const settings = E.sanitize({ maxScrolls: 3 });
  assert.equal(E.decide({ ...base(), settings, scrolls: 3, paused: true }), "stop-scrolls");
});
test("custom session length is kept inside 15..240", () => {
  assert.equal(E.sanitize({ durationMin: 45 }).durationMin, 45);
  assert.equal(E.sanitize({ durationMin: 5 }).durationMin, 15);
  assert.equal(E.sanitize({ durationMin: 999 }).durationMin, 240);
});
