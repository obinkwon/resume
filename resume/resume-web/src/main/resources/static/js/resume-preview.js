/**
 * 이력서 미리보기 렌더러 (템플릿 선택 화면 · 템플릿 편집 화면 · PDF 출력이 함께 씀)
 *  - body[data-template-html] / body[data-template-css] 를 읽어 #resume 에 그린다.
 *  - 템플릿 HTML 안의 치환자 {{profile}} 등을 #sectionPool 의 섹션 원본으로 바꾼다.
 *  - 부모 창(iframe 으로 연 화면)과 주고받는 메시지 (형식은 이것 하나로 통일)
 *      · 받음  template-preview  { html, css }          → 다시 그림
 *      · 보냄  preview-ready     { available }          → 처음 그린 뒤 한 번
 *      · 보냄  template-rendered { placed, available }  → template-preview 를 받아 그린 뒤
 *      · 보냄  preview-height    { height }             → 내용 높이가 바뀔 때마다
 */
(function () {
    'use strict';

    // 사용할 수 있는 치환자 (resume-preview.html 의 data-section 과 1:1)
    var ALLOWED = ['profile', 'intro', 'cover', 'experience', 'project', 'education', 'certificate', 'skill', 'link'];
    var PLACEHOLDER = /\{\{\s*([a-zA-Z]+)\s*\}\}/g;

    // 템플릿 HTML 이 비어 있으면 모든 섹션을 순서대로 출력
    var DEFAULT_HTML = ALLOWED.map(function (n) { return '{{' + n + '}}'; }).join('\n');

    // 템플릿에서 허용하지 않는 태그
    var FORBIDDEN_TAGS = ['script', 'iframe', 'frame', 'frameset', 'object', 'embed', 'link', 'meta', 'base', 'form', 'input', 'button', 'textarea', 'select', 'style'];

    var resumeEl, poolEl, cssEl;
    var lastHeight = 0;
    var heightFrame = null;

    /** 위험한 태그·속성 제거 (서버에서도 검증하지만 화면에서도 한 번 더 막음) */
    function sanitize(root) {
        FORBIDDEN_TAGS.forEach(function (tag) {
            root.querySelectorAll(tag).forEach(function (el) { el.remove(); });
        });
        root.querySelectorAll('*').forEach(function (el) {
            Array.prototype.slice.call(el.attributes).forEach(function (attr) {
                var name = attr.name.toLowerCase();
                var value = (attr.value || '').replace(/\s+/g, '').toLowerCase();
                if (name.indexOf('on') === 0) {
                    el.removeAttribute(attr.name);
                } else if ((name === 'href' || name === 'src' || name === 'xlink:href' || name === 'action' || name === 'formaction')
                    && (value.indexOf('javascript:') === 0 || value.indexOf('vbscript:') === 0 || value.indexOf('data:text/html') === 0)) {
                    el.removeAttribute(attr.name);
                }
            });
        });
    }

    /** 데이터가 있는 섹션 목록 (th:if 로 빠진 섹션은 pool 에 없음) */
    function availableSections() {
        return ALLOWED.filter(function (name) {
            return !!poolEl.querySelector('[data-section="' + name + '"]');
        });
    }

    /** 템플릿 HTML/CSS 로 이력서를 그린다 */
    function render(html, css) {
        cssEl.textContent = css || '';

        var source = (html && html.trim()) ? html : DEFAULT_HTML;
        var used = {};
        var placed = [];

        // 치환자를 빈 슬롯 태그로 바꿔 둔다 (허용되지 않은 치환자·중복 치환자는 지움)
        var marked = source.replace(PLACEHOLDER, function (match, rawName) {
            var name = rawName.toLowerCase();
            if (ALLOWED.indexOf(name) < 0 || used[name]) return '';
            used[name] = true;
            return '<div data-slot="' + name + '"></div>';
        });

        var tpl = document.createElement('template');
        tpl.innerHTML = marked;
        sanitize(tpl.content);

        tpl.content.querySelectorAll('[data-slot]').forEach(function (slot) {
            var name = slot.getAttribute('data-slot');
            var section = poolEl.querySelector('[data-section="' + name + '"]');
            if (section) {
                slot.replaceWith(section.cloneNode(true));
                placed.push(name);
            } else {
                slot.remove(); // 데이터가 없는 섹션은 출력하지 않음
            }
        });

        resumeEl.replaceChildren(tpl.content);
        scheduleHeight();
        return placed;
    }

    /* ---------------- 부모 창과 통신 ---------------- */

    function isEmbedded() {
        try { return window.parent && window.parent !== window; } catch (e) { return false; }
    }

    function notifyParent(message) {
        if (isEmbedded()) window.parent.postMessage(message, window.location.origin);
    }

    /** 내용 높이를 부모 창에 알림 (바뀌었을 때만, 한 프레임에 한 번) */
    function scheduleHeight() {
        if (!isEmbedded() || heightFrame !== null) return;
        heightFrame = window.requestAnimationFrame(function () {
            heightFrame = null;
            var height = Math.ceil(document.documentElement.scrollHeight);
            if (height === lastHeight) return;
            lastHeight = height;
            notifyParent({ type: 'preview-height', height: height });
        });
    }

    function watchHeight() {
        if (!isEmbedded()) return;
        if ('ResizeObserver' in window) {
            // 글꼴이 늦게 불러와져 높이가 바뀌는 경우도 잡음
            new ResizeObserver(scheduleHeight).observe(resumeEl);
        } else {
            window.addEventListener('load', scheduleHeight);
        }
    }

    function onMessage(event) {
        if (event.origin !== window.location.origin) return;
        if (event.source !== window.parent) return;
        var data = event.data;
        if (!data || data.type !== 'template-preview') return;

        var placed = render(data.html, data.css);
        notifyParent({ type: 'template-rendered', placed: placed, available: availableSections() });
    }

    document.addEventListener('DOMContentLoaded', function () {
        resumeEl = document.getElementById('resume');
        poolEl = document.getElementById('sectionPool');
        cssEl = document.getElementById('templateCss');
        if (!resumeEl || !poolEl || !cssEl) return;

        var body = document.body;
        render(body.getAttribute('data-template-html'), body.getAttribute('data-template-css'));

        window.addEventListener('message', onMessage);
        watchHeight();
        notifyParent({ type: 'preview-ready', available: availableSections() });
    });
})();