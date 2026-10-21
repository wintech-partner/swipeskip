// Edge build only: the opt-in checkbox. Off until the user ticks it; unticking stops sending at once.
(function () {
  const box = document.getElementById("telemetry");
  chrome.storage.local.get("telemetryConsent").then((r) => { box.checked = r.telemetryConsent === true; });
  box.addEventListener("change", () => chrome.storage.local.set({ telemetryConsent: box.checked }));
})();
