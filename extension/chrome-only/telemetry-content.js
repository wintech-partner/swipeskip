// Chrome build only. Hands anonymous events to the background worker, which checks consent.
(function () {
  const ext = chrome;
  self.SwipeSkipTelemetry = {
    emit(ev, data) {
      try { ext.runtime.sendMessage({ type: "telemetry", ev, data }).catch(() => {}); } catch (_) { /* extension reloaded */ }
    },
  };
})();
