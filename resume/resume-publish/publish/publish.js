// data-partial 자리에 공통 조각(목록 이동 버튼 등)을 넣음
(function(){
    var root = document.currentScript.src.replace(/publish\/publish\.js.*$/, "");
    var pagePath = (location.pathname.match(/pages\/(.+)\.html$/) || [])[1] || "";
    var embedded = window.self !== window.top;   // 다른 화면의 iframe 안 (미리보기)

    document.querySelectorAll("[data-partial]").forEach(function(slot){
        if (embedded && slot.dataset.partial === "publishBar") { slot.remove(); return; }
        var template = document.createElement("template");
        template.innerHTML = window.PUBLISH_PARTIALS[slot.dataset.partial].split("{root}").join(root).trim();
        slot.replaceWith(template.content);
    });

    var path = document.querySelector("[data-page-path]");
    if (path) path.textContent = pagePath;

    // A4 미리보기 iframe 을 칸 너비에 맞춰 축소 (사이트에서는 화면 JS 가 하는 일)
    function fitPreview(){
        document.querySelectorAll(".preview-page iframe").forEach(function(frame){
            var page = frame.parentElement, stage = page.parentElement;
            var style = getComputedStyle(stage);
            var room = stage.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight);
            var scale = Math.min(1, room / 794);
            frame.style.transform = "scale(" + scale + ")";
            page.style.width = 794 * scale + "px";
            page.style.height = 1123 * scale + "px";
        });
    }
    fitPreview();
    window.addEventListener("resize", fitPreview);
})();
