/* =========================================================
   resume-preview.js  (이력서 미리보기 / 인쇄)
   ========================================================= */
(() => {
    'use strict';

    const SECTION_KEYS = ['profile', 'intro', 'experience', 'project', 'education', 'certificate', 'skill', 'link'];

    // 템플릿이 없을 때 쓰는 한 컬럼 기본 틀
    const FALLBACK_HTML = SECTION_KEYS.map(k => `{{${k}}}`).join('\n');

    // 템플릿 HTML 에서 허용하지 않는 것 (CSP 로도 막지만 한 번 더 제거)
    const BLOCKED_TAGS = 'script, iframe, frame, object, embed, link, meta, base, form, input, button, textarea, select, style';

    const body    = document.body;
    const resume  = document.getElementById('resume');
    const pool    = document.getElementById('sectionPool');
    const styleEl = document.getElementById('templateCss');

    // 섹션 원본 (내용이 없는 섹션은 서버에서 아예 그리지 않음 → 치환자는 빈 자리로)
    const sections = {};
    pool.querySelectorAll('[data-section]').forEach(el => { sections[el.dataset.section] = el; });

    /** 템플릿 HTML 정리: 위험한 태그·이벤트 속성·javascript: 링크 제거 */
    function sanitize(fragment) {
        fragment.querySelectorAll(BLOCKED_TAGS).forEach(el => el.remove());
        fragment.querySelectorAll('*').forEach(el => {
            [...el.attributes].forEach(attr => {
                const name = attr.name.toLowerCase();
                const value = attr.value.trim().toLowerCase();
                if (name.startsWith('on') || name === 'srcdoc'
                    || (['href', 'src', 'xlink:href', 'action'].includes(name) && value.startsWith('javascript:'))) {
                    el.removeAttribute(attr.name);
                }
            });
        });
        return fragment;
    }

    function render(template) {
        const html = (template && template.htmlContent) || FALLBACK_HTML;
        const css  = (template && template.cssContent) || '';

        // 1) CSS — textContent 라서 </style> 같은 문자열이 와도 HTML 로 해석되지 않음
        styleEl.textContent = css;

        // 2) 치환자 → 자리표시 요소. 정해진 이름만 바꾸고 나머지 {{...}} 는 지움
        const marked = html.replace(/\{\{\s*([a-zA-Z]+)\s*\}\}/g, (m, key) =>
            SECTION_KEYS.includes(key) ? `<template data-slot="${key}"></template>` : '');

        const tpl = document.createElement('template');
        tpl.innerHTML = marked;                 // <template> 안이라 스크립트·이미지 로드가 일어나지 않음
        const fragment = sanitize(tpl.content);

        // 3) 자리표시 요소를 실제 섹션 복사본으로 교체 (같은 섹션을 두 번 넣어도 각각 복사)
        fragment.querySelectorAll('template[data-slot]').forEach(slot => {
            const src = sections[slot.dataset.slot];
            if (src) slot.replaceWith(src.cloneNode(true));
            else slot.remove();
        });

        resume.replaceChildren(fragment);
        body.dataset.rendered = 'true';
        reportHeight();
    }

    // ---------- 부모(빌더)와 통신 ----------
    const inBuilder = window.parent !== window;

    function reportHeight() {
        if (!inBuilder) return;
        requestAnimationFrame(() => {
            window.parent.postMessage(
                { type: 'preview:height', height: resume.offsetHeight },
                location.origin
            );
        });
    }

    window.addEventListener('message', e => {
        if (e.origin !== location.origin || e.source !== window.parent) return;
        if (e.data?.type === 'template:apply') render(e.data.template);
    });

    document.fonts?.ready.then(reportHeight);
    new ResizeObserver(reportHeight).observe(resume);

    // 저장된 템플릿으로 먼저 그림 (단독으로 열거나 인쇄할 때 그대로 사용)
    render({
        htmlContent: body.dataset.templateHtml,
        cssContent: body.dataset.templateCss
    });
    if (inBuilder) window.parent.postMessage({ type: 'preview:ready' }, location.origin);
})();