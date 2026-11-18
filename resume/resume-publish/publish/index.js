// 목록 페이지 : catalog.js 의 그룹과 페이지를 카드로 그림
(function(){
    function el(tag, className, text){
        var node = document.createElement(tag);
        if (className) node.className = className;
        if (text) node.textContent = text;
        return node;
    }

    var groups = document.getElementById("groups");
    window.PUBLISH_CATALOG.forEach(function(group){
        var section = el("section", "pub-group");
        var head = el("div", "section-head");
        head.append(el("span", "section-eyebrow", group.eyebrow), el("h2", null, group.name), el("p", "pub-note", group.desc));

        var cards = el("div", "pub-cards");
        group.pages.forEach(function(page){
            var card = el("a", "pub-card");
            card.href = "pages/" + page.path + ".html";
            var meta = el("span", "pub-card-meta");
            page.css.forEach(function(css){ meta.append(el("span", "pub-card-css", css)); });
            meta.append(el("span", "pub-card-date", page.updated));
            card.append(el("span", "pub-card-title", page.title), el("span", "pub-card-desc", page.desc), meta);
            cards.append(card);
        });

        section.append(head, cards);
        groups.append(section);
    });
})();
