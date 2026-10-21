// Firefox build only (ADR 12). A script cannot send a trusted click, and YouTube ignores synthetic ones on its Skip
// button, so once the Skip button is showing and a click did not skip, the ad video is moved to its end instead.
(function () {
  self.SwipeSkipAdSeek = function () {
    if (!document.querySelector(".ad-showing")) return;
    const v = document.querySelector("#movie_player video.html5-main-video") || document.querySelector("video.html5-main-video");
    if (v && v.duration > 0 && isFinite(v.duration)) v.currentTime = v.duration;
  };
})();
