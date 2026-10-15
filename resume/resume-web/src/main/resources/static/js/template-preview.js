/* =========================================================
   resume-preview.js  (이력서 출력 화면)
   - 설정(sections 순서/표시, 레이아웃)에 맞춰 섹션을 main/aside 로 배치
   - 빌더(부모 창)에서 오는 postMessage 를 받아 실시간 반영
   ========================================================= */
(() => {
    'use strict';

    // 레이아웃별로 오른쪽(또는 사이드바) 컬럼에 들어갈 섹션
    const ASIDE_SECTIONS = {
        single:  [],
        sidebar: ['skill', 'education'],
        split:   ['intro', 'education', 'skill']
    };

    const body  = document.body;
    const pool  = document.getElementById('sectionPool');
    const main  = document.querySelector('[data-zone="main"]');
    const aside = document.querySelector('[data-zone="aside"]');

    // 섹션 원본을 key 로 보관 (내용이 없는 섹션은 th:if 로 아예 없음)
    const sectionEls = {};
    pool.querySelectorAll('[data-section]').forEach(el => {
        sectionEls[el.dataset.section] = el;
    });

    function apply(setting) {
        if (!setting) return;

        body.dataset.template = setting.templateCode;
        body.dataset.layout   = setting.layoutType;
        body.style.setProperty('--primary', setting.primaryColor);
        body.style.setProperty('--font', `'${setting.fontFamily}'`);
        body.style.setProperty('--scale', (Number(setting.fontScale) || 100) / 100);

        // ATS 는 항상 한 컬럼
        const layout = setting.templateCode === 'ats' ? 'single' : setting.layoutType;
        const asideKeys = ASIDE_SECTIONS[layout] || [];

        let sections = setting.sections;
        if (typeof sections === 'string') {
            try { sections = JSON.parse(sections); } catch (e) { sections = []; }
        }

        // 일단 전부 pool 로 되돌린 뒤, 설정 순서대로 다시 배치
        Object.values(sectionEls).forEach(el => pool.appendChild(el));
        (sections || []).forEach(({ key, visible }) => {
            const el = sectionEls[key];
            if (!el || visible === false) return;
            (asideKeys.includes(key) ? aside : main).appendChild(el);
        });

        reportHeight();
    }

    // ---------- 부모(빌더)와 통신 ----------
    const inBuilder = window.parent !== window;

    function reportHeight() {
        if (!inBuilder) return;
        requestAnimationFrame(() => {
            window.parent.postMessage(
                // documentElement.scrollHeight 는 iframe 높이 아래로 줄지 않으므로 .resume 기준
                { type: 'preview:height', height: document.querySelector('.resume').offsetHeight },
                location.origin
            );
        });
    }

    window.addEventListener('message', e => {
        if (e.origin !== location.origin) return;
        if (e.data?.type === 'template:apply') apply(e.data.setting);
    });

    // 웹폰트 로딩 후 높이가 바뀌므로 다시 보고
    document.fonts?.ready.then(reportHeight);
    new ResizeObserver(reportHeight).observe(document.querySelector('.resume'));

    // 단독으로 열었을 때(인쇄/공유)도 저장된 설정대로 그림
    apply(window.TEMPLATE_SETTING || {
        templateCode: 'minimal', layoutType: 'single', primaryColor: '#2D3748',
        fontFamily: 'Pretendard', fontScale: 100,
        sections: ['intro', 'career', 'project', 'education', 'skill'].map(key => ({ key, visible: true }))
    });
    if (inBuilder) window.parent.postMessage({ type: 'preview:ready' }, location.origin);
})();