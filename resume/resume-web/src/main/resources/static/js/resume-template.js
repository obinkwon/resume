/* =========================================================
   resume-template.js  (이력서 템플릿 꾸미기)
   왼쪽 : ① 템플릿 선택  ② 섹션 표시  ③ CSS 수정
   오른쪽: 미리보기(iframe, 실제 이력서 내용)

   미리보기와 통신 (postMessage)
     빌더 → 미리보기 : { type: 'template:apply', template: { htmlContent, cssContent } }
     미리보기 → 빌더 : { type: 'preview:ready' }, { type: 'preview:height', height }

   저장 : PUT {data-save-url}  body { templateId, cssContent }
          응답 { templateId } — 서버가 새 템플릿 행을 만들었으면 그 ID
   ========================================================= */
(() => {
    'use strict';

    const SECTION_LABELS = {
        intro: '자기소개',
        experience: '경력',
        project: '프로젝트',
        education: '학력',
        certificate: '자격증',
        skill: '기술 스택',
        link: '외부 링크'
    };

    const CSS_MAX = 20000;
    const A4_WIDTH = 794;

    // 서버에서도 같은 규칙으로 거부해야 함 (화면 검사는 안내용)
    const CSS_BLOCKED = [
        { re: /@import/i,         msg: '@import 는 사용할 수 없어요.' },
        { re: /url\s*\(/i,        msg: 'url() 로 외부 파일을 불러올 수 없어요.' },
        { re: /<\s*\/?\s*style/i, msg: '<style> 태그는 쓸 수 없어요. CSS만 입력해 주세요.' },
        { re: /expression\s*\(/i, msg: 'expression() 은 사용할 수 없어요.' },
        { re: /javascript\s*:/i,  msg: 'javascript: 는 사용할 수 없어요.' }
    ];

    // ③ 자주 쓰는 스타일 (선택하면 CSS 맨 아래에 추가)
    const SNIPPETS = [
        { label: '대표 색상 바꾸기',       css: ':root {\n  --primary: #E11D48;\n}' },
        { label: '폰트 바꾸기',           css: ":root {\n  --font: 'Noto Sans KR'; /* Pretendard, Noto Sans KR, Roboto */\n}" },
        { label: '글자 크기 키우기',       css: ':root {\n  --scale: 1.06;\n}' },
        { label: '이름을 가운데로',        css: '.r-header {\n  flex-direction: column;\n  align-items: center;\n  text-align: center;\n}\n.r-contact {\n  text-align: center;\n}' },
        { label: '섹션 제목을 색 박스로',   css: '.r-section__title {\n  padding: 4px 10px;\n  border: none;\n  border-radius: 4px;\n  background: var(--primary);\n  color: #fff;\n}' },
        { label: '경력을 타임라인으로',     css: '.r-section[data-section="experience"] .r-item {\n  position: relative;\n  padding-left: 18px;\n  border-left: 2px solid var(--line);\n}\n.r-section[data-section="experience"] .r-item::before {\n  content: "";\n  position: absolute;\n  left: -6px;\n  top: 6px;\n  width: 10px;\n  height: 10px;\n  border-radius: 50%;\n  background: var(--primary);\n}' },
        { label: '기술 태그를 테두리형으로', css: '.r-tags li {\n  border-radius: 4px;\n  border: 1px solid var(--primary);\n  background: transparent;\n}' },
        { label: '기간을 강조색으로',       css: '.r-period {\n  color: var(--primary);\n  font-weight: 600;\n}' }
    ];

    // ② 섹션 숨김 규칙은 CSS 맨 아래 이 표시 사이에 자동으로 관리
    const HIDE_START = '/* ===== 섹션 숨김 (섹션 표시 메뉴에서 자동 관리) ===== */';
    const HIDE_END   = '/* ===== 섹션 숨김 끝 ===== */';

    // ---------- DOM ----------
    const root          = document.querySelector('.builder');
    const groupsEl      = document.getElementById('templateGroups');
    const togglesEl     = document.getElementById('sectionToggles');
    const editor        = document.getElementById('cssEditor');
    const snippetSelect = document.getElementById('snippetSelect');
    const cssResetBtn   = document.getElementById('cssResetBtn');
    const cssCountEl    = document.getElementById('cssCount');
    const cssWarningEl  = document.getElementById('cssWarning');
    const frame         = document.getElementById('previewFrame');
    const page          = document.getElementById('previewPage');
    const stage         = document.getElementById('previewStage');
    const zoomLabel     = document.getElementById('zoomLabel');
    const saveBtn       = document.getElementById('saveBtn');
    const resetBtn      = document.getElementById('resetBtn');
    const printBtn      = document.getElementById('printBtn');
    const statusEl      = document.getElementById('saveStatus');

    const saveUrl = root.dataset.saveUrl;

    // ---------- 데이터 ----------
    const templates = (window.TEMPLATES || []).map(t => Object.assign({}, t, {
        cssContent: t.cssContent || '',
        htmlContent: t.htmlContent || ''
    }));

    const startTemplate = findTemplate(window.CURRENT_TEMPLATE_ID) || templates[0] || null;

    /** state = 이 이력서에 저장될 값 */
    let state = {
        templateId: startTemplate ? startTemplate.templateId : null,
        cssContent: startTemplate ? startTemplate.cssContent : ''
    };
    // 템플릿이 아직 없던 이력서는 저장 전 상태로 시작
    let saved = window.CURRENT_TEMPLATE_ID ? clone(state) : { templateId: null, cssContent: '' };
    let frameHeight = 1123;

    // ---------- 유틸 ----------
    function clone(obj) { return JSON.parse(JSON.stringify(obj)); }

    function findTemplate(id) {
        return id == null ? null : templates.find(t => String(t.templateId) === String(id)) || null;
    }

    function current() { return findTemplate(state.templateId); }

    function sameCss(a, b) {
        return (a || '').replace(/\s+/g, '') === (b || '').replace(/\s+/g, '');
    }

    function isDirty() {
        return String(state.templateId) !== String(saved.templateId) || state.cssContent !== saved.cssContent;
    }

    function setStatus(text, type) {
        statusEl.textContent = text;
        statusEl.className = 'save-status text-sm' + (type ? ' is-' + type : '');
    }

    function csrfHeaders() {
        const token  = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    function escapeHtml(str) {
        return String(str ?? '').replace(/[&<>"']/g, c => (
            { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
        ));
    }

    // ---------- 섹션 숨김 블록 ----------
    function splitHidden(css) {
        const s = css.indexOf(HIDE_START);
        const e = css.indexOf(HIDE_END);
        if (s < 0 || e < s) return { body: css, hidden: [] };
        const block = css.slice(s, e);
        const hidden = [...block.matchAll(/data-section="([a-z]+)"/g)].map(m => m[1]);
        const body = (css.slice(0, s) + css.slice(e + HIDE_END.length)).replace(/\s+$/, '');
        return { body, hidden };
    }

    function joinHidden(body, hidden) {
        const base = (body || '').replace(/\s+$/, '');
        if (!hidden.length) return base ? base + '\n' : '';
        const selectors = hidden.map(k => `.r-section[data-section="${k}"]`).join(',\n');
        return `${base}${base ? '\n\n' : ''}${HIDE_START}\n${selectors} {\n  display: none !important;\n}\n${HIDE_END}\n`;
    }

    // ---------- 렌더링 ----------
    function renderAll() {
        renderTemplates();
        editor.value = state.cssContent;
        renderToggles();
        renderCssInfo();
    }

    /** 미니 썸네일: thumbnailUrl 이 있으면 이미지, 없으면 HTML 틀·CSS 를 보고 대략 그림 */
    function thumbHtml(t) {
        if (t.thumbnailUrl) {
            return `<img src="${escapeHtml(t.thumbnailUrl)}" alt="" loading="lazy">`;
        }
        const html = t.htmlContent;
        const asideAt = html.indexOf('r-aside');
        const mainAt  = html.indexOf('r-main');
        const layout  = asideAt < 0 ? 'single' : (asideAt < mainAt ? 'sidebar' : 'split');
        const banner  = /\.r-header\s*\{[^}]*background\s*:/i.test(t.cssContent);
        const color   = /--primary\s*:\s*(#[0-9a-f]{3,8})/i.exec(t.cssContent)?.[1] || '#2D3748';
        return `
            <span class="mini" data-layout="${layout}" data-banner="${banner}" style="--t-color:${color}">
                <span class="mini__head"></span>
                <span class="mini__body">
                    <span class="mini__col"></span>
                    <span class="mini__col mini__col--aside"></span>
                </span>
            </span>`;
    }

    function cardHtml(t) {
        const selected = String(t.templateId) === String(state.templateId);
        return `
            <button type="button" class="template-card" role="radio"
                    aria-checked="${selected}" data-id="${t.templateId}">
                <span class="template-card__thumb">${thumbHtml(t)}</span>
                <span class="template-card__name">${escapeHtml(t.templateName)}</span>
                ${t.description ? `<span class="template-card__desc">${escapeHtml(t.description)}</span>` : ''}
            </button>`;
    }

    function renderTemplates() {
        if (!templates.length) {
            groupsEl.innerHTML = '<p class="panel-hint">사용할 수 있는 템플릿이 없어요.</p>';
            return;
        }
        const groups = [
            { title: '기본 템플릿', items: templates.filter(t => t.createdBy == null) },
            { title: '내 템플릿',   items: templates.filter(t => t.createdBy != null) }
        ].filter(g => g.items.length);

        groupsEl.innerHTML = groups.map(g => `
            <div class="template-group">
                ${groups.length > 1 ? `<h3 class="template-group__title">${g.title}</h3>` : ''}
                <div class="template-grid" role="radiogroup" aria-label="${g.title}">
                    ${g.items.map(cardHtml).join('')}
                </div>
            </div>`).join('');
    }

    function renderToggles() {
        const { hidden } = splitHidden(state.cssContent);
        togglesEl.innerHTML = Object.entries(SECTION_LABELS).map(([key, label]) => `
            <li class="section-toggle${hidden.includes(key) ? ' is-hidden' : ''}">
                <span class="section-name">${label}</span>
                <label class="switch" title="표시">
                    <input type="checkbox" data-key="${key}" ${hidden.includes(key) ? '' : 'checked'}
                           aria-label="${label} 표시">
                    <span></span>
                </label>
            </li>`).join('');
    }

    /** 글자 수 + 금지 문구 안내. 문제가 있으면 메시지 반환 */
    function renderCssInfo() {
        const css = state.cssContent;
        cssCountEl.textContent = `${css.length.toLocaleString()} / ${CSS_MAX.toLocaleString()}`;
        cssCountEl.classList.toggle('text-danger', css.length > CSS_MAX);

        let problem = css.length > CSS_MAX ? `CSS는 ${CSS_MAX.toLocaleString()}자까지 입력할 수 있어요.` : '';
        if (!problem) problem = CSS_BLOCKED.find(b => b.re.test(css))?.msg || '';

        cssWarningEl.hidden = !problem;
        cssWarningEl.textContent = problem;
        cssResetBtn.disabled = !current() || sameCss(css, current().cssContent);
        return problem;
    }

    // ---------- state 변경 → 미리보기 ----------
    function update(patch) {
        Object.assign(state, patch);
        pushToPreview();
        const dirty = isDirty();
        resetBtn.disabled = !dirty || !saved.templateId;
        setStatus(dirty ? '저장 안 됨' : '', dirty ? 'dirty' : '');
    }

    function pushToPreview() {
        const t = current();
        if (!frame.contentWindow || !t) return;
        frame.contentWindow.postMessage({
            type: 'template:apply',
            template: { htmlContent: t.htmlContent, cssContent: state.cssContent }
        }, location.origin);
    }

    /** 에디터 내용을 바꾸고 화면·미리보기 갱신 */
    function setCss(css) {
        editor.value = css;
        renderToggles();
        renderCssInfo();
        update({ cssContent: css });
    }

    // ---------- 이벤트: ① 템플릿 선택 ----------
    groupsEl.addEventListener('click', e => {
        const card = e.target.closest('.template-card');
        if (!card) return;
        const t = findTemplate(card.dataset.id);
        if (!t || String(t.templateId) === String(state.templateId)) return;

        // 직접 고친 CSS 가 있으면 확인 (섹션 숨김은 고친 것으로 보지 않음)
        const prev = current();
        const { body, hidden } = splitHidden(state.cssContent);
        const edited = prev && !sameCss(body, splitHidden(prev.cssContent).body);
        if (edited && !confirm('수정한 CSS는 사라지고 선택한 템플릿의 CSS로 바뀌어요. 바꿀까요?')) return;

        state.templateId = t.templateId;
        renderTemplates();
        groupsEl.querySelector(`.template-card[data-id="${t.templateId}"]`)?.focus();
        // 섹션 표시 설정은 그대로 유지
        setCss(joinHidden(t.cssContent, hidden));
    });

    // 방향키로 카드 이동
    groupsEl.addEventListener('keydown', e => {
        if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(e.key)) return;
        const cards = [...groupsEl.querySelectorAll('.template-card')];
        const i = cards.indexOf(document.activeElement);
        if (i < 0) return;
        e.preventDefault();
        const step = e.key === 'ArrowLeft' || e.key === 'ArrowUp' ? -1 : 1;
        const next = cards[(i + step + cards.length) % cards.length];
        next.focus();
        next.click();
    });

    // ---------- 이벤트: ② 섹션 표시 ----------
    togglesEl.addEventListener('change', e => {
        const input = e.target.closest('input[data-key]');
        if (!input) return;
        const { body } = splitHidden(state.cssContent);
        const hidden = [...togglesEl.querySelectorAll('input[data-key]:not(:checked)')].map(el => el.dataset.key);
        setCss(joinHidden(body, hidden));
    });

    // ---------- 이벤트: ③ CSS 수정 ----------
    let cssTimer = null;
    editor.addEventListener('input', () => {
        state.cssContent = editor.value;          // 글자 수·경고는 바로
        renderCssInfo();
        clearTimeout(cssTimer);                   // 미리보기·토글은 입력이 멈추면
        cssTimer = setTimeout(() => {
            renderToggles();
            update({ cssContent: editor.value });
        }, 250);
    });

    // Tab → 공백 2칸 (Shift+Tab 은 기본 동작 = 포커스 이동)
    editor.addEventListener('keydown', e => {
        if (e.key !== 'Tab' || e.shiftKey) return;
        e.preventDefault();
        const { selectionStart: s, selectionEnd: en, value } = editor;
        editor.value = value.slice(0, s) + '  ' + value.slice(en);
        editor.selectionStart = editor.selectionEnd = s + 2;
        editor.dispatchEvent(new Event('input'));
    });

    SNIPPETS.forEach((sn, i) => snippetSelect.add(new Option(sn.label, String(i))));
    snippetSelect.addEventListener('change', () => {
        const sn = SNIPPETS[Number(snippetSelect.value)];
        snippetSelect.value = '';
        if (!sn) return;
        // 섹션 숨김 블록보다 위에 추가
        const { body, hidden } = splitHidden(editor.value);
        const next = (body ? body + '\n\n' : '') + `/* ${sn.label} */\n${sn.css}`;
        setCss(joinHidden(next, hidden));
        editor.focus();
        editor.scrollTop = editor.scrollHeight;
    });

    cssResetBtn.addEventListener('click', () => {
        const t = current();
        if (!t || !confirm('수정한 CSS를 지우고 템플릿의 원래 CSS로 되돌릴까요?')) return;
        setCss(t.cssContent);
    });

    // ---------- 저장 / 되돌리기 / 인쇄 ----------
    saveBtn.addEventListener('click', async () => {
        clearTimeout(cssTimer);
        state.cssContent = editor.value;

        if (!state.templateId) {
            setStatus('템플릿을 선택해 주세요', 'error');
            return;
        }
        if (renderCssInfo()) {
            editor.focus();
            setStatus('CSS를 확인해 주세요', 'error');
            return;
        }

        saveBtn.disabled = true;
        setStatus('저장 중…');
        try {
            const res = await fetch(saveUrl, {
                method: 'PUT',
                headers: Object.assign({ 'Content-Type': 'application/json' }, csrfHeaders()),
                body: JSON.stringify({ templateId: state.templateId, cssContent: state.cssContent })
            });
            if (!res.ok) throw new Error(res.status);

            // 서버가 CSS 를 반영한 새 템플릿을 만들었으면 그 ID 로 교체 (내 템플릿에 추가)
            const data = res.status === 204 ? null : await res.json().catch(() => null);
            if (data?.templateId && String(data.templateId) !== String(state.templateId)) {
                const base = current();
                templates.push({
                    templateId: data.templateId,
                    templateName: data.templateName || `${base.templateName} (내 디자인)`,
                    description: '',
                    thumbnailUrl: null,
                    htmlContent: base.htmlContent,
                    cssContent: state.cssContent,
                    createdBy: data.createdBy ?? -1
                });
                state.templateId = data.templateId;
            } else if (current()?.createdBy != null) {
                current().cssContent = state.cssContent;   // 내 템플릿을 고친 경우
            }

            saved = clone(state);
            renderTemplates();
            renderCssInfo();
            resetBtn.disabled = true;
            setStatus('저장됨', 'saved');
        } catch (err) {
            console.error('템플릿 저장 실패', err);
            setStatus('저장 실패', 'error');
        } finally {
            saveBtn.disabled = false;
        }
    });

    resetBtn.addEventListener('click', () => {
        if (!saved.templateId) return;
        state.templateId = saved.templateId;
        renderTemplates();
        setCss(saved.cssContent);
    });

    printBtn.addEventListener('click', () => {
        // 브라우저 인쇄창 → "PDF로 저장"
        frame.contentWindow?.focus();
        frame.contentWindow?.print();
    });

    window.addEventListener('beforeunload', e => {
        if (isDirty()) { e.preventDefault(); e.returnValue = ''; }
    });

    // ---------- 미리보기 iframe 통신 / 크기 맞춤 ----------
    window.addEventListener('message', e => {
        if (e.origin !== location.origin || e.source !== frame.contentWindow) return;
        const msg = e.data || {};
        if (msg.type === 'preview:ready') pushToPreview();
        if (msg.type === 'preview:height') {
            frameHeight = Math.max(1123, Math.ceil(msg.height));
            fitPreview();
        }
    });
    frame.addEventListener('load', pushToPreview);

    function fitPreview() {
        const style = getComputedStyle(stage);
        const avail = stage.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight);
        const scale = Math.min(1, avail / A4_WIDTH);
        frame.style.height = frameHeight + 'px';
        frame.style.transform = `scale(${scale})`;
        page.style.width  = A4_WIDTH * scale + 'px';
        page.style.height = frameHeight * scale + 'px';
        zoomLabel.textContent = Math.round(scale * 100) + '%';
    }
    new ResizeObserver(fitPreview).observe(stage);

    // ---------- 시작 ----------
    renderAll();
    update({});
    fitPreview();
})();