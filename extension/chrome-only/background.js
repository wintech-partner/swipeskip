// Chrome build only. Sends one small anonymous event per message, and only after the user opted in.
// No retry queue, no stored events, no identifier, cookies omitted.
const TELEMETRY_URL = "__TELEMETRY_URL__";
const VERSION = chrome.runtime.getManifest().version;

function browserName() {
  const brands = (navigator.userAgentData && navigator.userAgentData.brands || []).map((b) => b.brand);
  if (brands.includes("Microsoft Edge")) return "edge";
  return brands.includes("Google Chrome") || brands.includes("Chromium") ? "chrome" : "other";
}

chrome.runtime.onMessage.addListener((msg, sender) => {
  if (!msg || msg.type !== "telemetry" || !sender.tab) return;
  chrome.storage.local.get("telemetryConsent").then((r) => {
    if (r.telemetryConsent !== true) return;
    const body = JSON.stringify({
      v: 1, ev: msg.ev, ext: VERSION, browser: browserName(),
      locale: (chrome.i18n.getUILanguage() || "en").slice(0, 8), data: msg.data,
    });
    fetch(TELEMETRY_URL + "/v1/event", {
      method: "POST", body, headers: { "Content-Type": "application/json" },
      credentials: "omit", referrerPolicy: "no-referrer", keepalive: true,
    }).catch(() => {});
  });
});
