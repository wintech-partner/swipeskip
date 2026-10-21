(function () {
  const ext = typeof browser !== "undefined" ? browser : chrome;
  const E = self.SwipeSkipEngine;
  const $ = (id) => document.getElementById(id);
  const t = (k, subs) => ext.i18n.getMessage(k, subs) || k;
  let running = false;
  let poll = null;

  document.querySelectorAll("[data-i18n]").forEach((el) => { el.textContent = t(el.dataset.i18n); });

  async function activeTab() {
    const tabs = await ext.tabs.query({ active: true, currentWindow: true });
    return tabs[0];
  }

  async function send(msg) {
    const tab = await activeTab();
    if (!tab || !/^https?:\/\/www\.youtube\.com\//.test(tab.url || "")) throw new Error("notYoutube");
    return ext.tabs.sendMessage(tab.id, msg);
  }

  function readForm() {
    return E.sanitize({
      mode: $("mode").value, intervalSec: $("intervalSec").value, durationMin: $("durationMin").value,
      maxScrolls: $("maxScrolls").value, tapSkip: !!$("tapSkip") && $("tapSkip").checked, skipAdShorts: $("skipAdShorts").checked,
    });
  }

  function fillForm(s) {
    $("mode").value = s.mode; $("intervalSec").value = s.intervalSec; $("durationMin").value = s.durationMin;
    $("maxScrolls").value = s.maxScrolls; if ($("tapSkip")) $("tapSkip").checked = s.tapSkip; $("skipAdShorts").checked = s.skipAdShorts;
    $("intervalRow").hidden = s.mode !== "timer";
  }

  function render(st) {
    running = !!(st && st.running);
    const b = $("toggle");
    b.textContent = running ? t("stop") : t("start");
    b.classList.toggle("stop", running);
    document.querySelectorAll("#f select, #f input").forEach((el) => { el.disabled = running; });
    let msg = "";
    if (running) msg = t("running", [String(Math.ceil(st.remainingSec / 60)), String(st.scrolls)]) + (st.pause ? " · " + t("pause_" + st.pause) : "");
    else if (st && st.endReason) msg = t("end_" + String(st.endReason).replace(/-/g, "_"));
    $("state").textContent = msg;
  }

  async function refresh() {
    try { render(await send({ type: "status" })); }
    catch (_) { render(null); $("state").textContent = t("openYoutube"); }
  }

  $("mode").addEventListener("change", () => { $("intervalRow").hidden = $("mode").value !== "timer"; });

  $("f").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      if (running) render(await send({ type: "stop" }));
      else {
        const s = readForm();
        await ext.storage.local.set({ settings: s });
        render(await send({ type: "start", settings: s }));
      }
    } catch (_) { $("state").textContent = t("openYoutube"); }
  });

  ext.storage.local.get("settings").then((r) => { fillForm(E.sanitize(r.settings)); refresh(); });
  poll = setInterval(refresh, 1000);
  window.addEventListener("unload", () => clearInterval(poll));
})();
