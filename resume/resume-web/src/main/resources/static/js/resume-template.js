/* =========================================================
   resume-template.js  (이력서 템플릿 선택)
   왼쪽 : ① 템플릿 선택  ② 섹션 표시  ③ CSS 수정
   오른쪽: 미리보기(iframe, 실제 이력서 내용)
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
    /** 템플릿이 이 개수보다 많으면 검색창 표시 */
    const SEARCH_MIN = 8;
    /** 카드 영역의 고정된 그룹 제목 높이 (선택 카드가 그 아래로 보이게) */
    const STICKY_OFFSET = 32;

    // 서버에서도 같은 규칙으로 거부해야 함 (화면 검사는 안내용)
    const CSS_BLOCKED = [
        { re: /@import/i,         msg: '@import 는 사용할 수 없어요.' },
        { re: /url\s*\(/i,        msg: 'url() 로 외부 파일을 불러올 수 없어요.' },
        { re: /<\s*\/?\s*style/i, msg: '<style> 태그는 쓸 수 없어요. CSS만 입력해 주세요.' },
        { re: /expression\s*\(/i, msg: 'expression() 은 사용할 수 없어요.' },
        { re: /javascript\s*:/i,  msg: 'javascript: 는 사용할 수 없어요.' }
    ];

    // ② 섹션 숨김 규칙은 CSS 맨 아래 이 표시 사이에 자동으로 관리
    const HIDE_START = '/* ===== 섹션 숨김 (섹션 표시 메뉴에서 자동 관리) ===== */';
    const HIDE_END   = '/* ===== 섹션 숨김 끝 ===== */';

    // ---------- DOM ----------
    const groupsEl      = document.getElementById('templateGroups');
    const countEl       = document.getElementById('templateCount');
    const searchWrap    = document.getElementById('templateSearchWrap');
    const searchInput   = document.getElementById('templateSearch');
    const togglesEl     = document.getElementById('sectionToggles');
    const editor        = document.getElementById('cssEditor');
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

    // ---------- API 경로 ----------
    const RESUME_ID = window.RESUME_ID;
    const API = {
        templateList: '/api/template/list',
        templateDetail: id => `/api/template/${encodeURIComponent(id)}`,
        templateSave: '/api/resume/template',
        templatePdf: id => `/api/resume/${encodeURIComponent(id)}/pdf`
    };

    // ---------- 데이터 ----------
    /** 템플릿 목록 (ID·이름 등 가벼운 정보만, API 로 채움) */
    const templates = [];

    /** 불러온 템플릿 내용 캐시 : 'templateId' → { htmlContent, cssContent, ... } */
    const details = new Map();
    /** 요청 중인 Promise (같은 템플릿을 연달아 눌러도 요청은 한 번) */
    const pending = new Map();

    /** state = 이 이력서에 저장될 값 (첫 템플릿을 불러온 뒤 채워짐) */
    let state = { templateId: null, cssContent: '' };
    let saved = { templateId: null, cssContent: '' };
    /** 지금 내용을 불러오는 중인 템플릿 ID (없으면 null) */
    let loadingId = null;
    /** 템플릿 검색어 */
    let keyword = '';
    let frameHeight = 1123;

    // ---------- 유틸 ----------
    function clone(obj) { return JSON.parse(JSON.stringify(obj)); }

    function findTemplate(id) {
        return id == null ? null : templates.find(t => String(t.templateId) === String(id)) || null;
    }

    /** 현재 선택된 템플릿의 내용 (아직 안 불러왔으면 null) */
    function current() {
        return state.templateId == null ? null : details.get(String(state.templateId)) || null;
    }

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

    function escapeHtml(str) {
        return String(str ?? '').replace(/[&<>"']/g, c => (
            { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
        ));
    }

    // ---------- API : 템플릿 목록 ----------
    /** 성공하면 true, 실패하면 false 로 끝나는 Promise */
    function loadTemplateList() {
        groupsEl.innerHTML = '<p class="panel-hint">템플릿 목록을 불러오는 중…</p>';
        countEl.textContent = '';
        setStatus('');

        return callApi({
            url: API.templateList,
            success: list => {
                templates.splice(0, templates.length, ...(Array.isArray(list) ? list : []));
                return true;
            },
            error: err => {
                console.error('템플릿 목록 불러오기 실패', err);
                groupsEl.innerHTML = `
                    <p class="panel-hint">템플릿 목록을 불러오지 못했어요.</p>
                    <button type="button" class="btn ghost sm" id="templateRetryBtn">다시 시도</button>`;
                setStatus('템플릿 목록을 불러오지 못했어요', 'error');
                return false;
            }
        });
    }

    // ---------- API : 템플릿 내용 ----------
    /** 실패하면 reject — selectTemplate 에서 처리 */
    function loadTemplate(id) {
        const key = String(id);
        if (details.has(key)) return Promise.resolve(details.get(key));
        if (pending.has(key)) return pending.get(key);

        const req = callApi({
            url: API.templateDetail(key),
            success: data => {
                const detail = Object.assign({}, data, {
                    htmlContent: data?.htmlContent || '',
                    cssContent: data?.cssContent || ''
                });
                details.set(key, detail);
                return detail;
            },
            error: err => { throw err; }
        }).finally(() => pending.delete(key));

        pending.set(key, req);
        return req;
    }

    /** 불러오는 동안 편집·저장 막기 */
    function setBusy(on) {
        editor.disabled = on;
        saveBtn.disabled = on;
        groupsEl.setAttribute('aria-busy', String(on));
        renderCssInfo();
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

    // ---------- 렌더링 : ① 템플릿 카드 ----------
    /**
     * 미니 썸네일
     *  - thumbnailUrl 이 있으면 이미지
     *  - 내용을 이미 불러온 템플릿이면 HTML 틀·CSS 를 보고 대략 그림
     *  - 아직 안 불러왔으면 기본 모양
     */
    function thumbHtml(t) {
        if (t.thumbnailUrl) {
            return `<img src="${escapeHtml(t.thumbnailUrl)}" alt="" loading="lazy">`;
        }
        const d = details.get(String(t.templateId));
        let layout = 'single', banner = false, color = '#2D3748';
        if (d) {
            const asideAt = d.htmlContent.indexOf('r-aside');
            const mainAt  = d.htmlContent.indexOf('r-main');
            layout = asideAt < 0 ? 'single' : (asideAt < mainAt ? 'sidebar' : 'split');
            banner = /\.r-header\s*\{[^}]*background\s*:/i.test(d.cssContent);
            color  = /--primary\s*:\s*(#[0-9a-f]{3,8})/i.exec(d.cssContent)?.[1] || color;
        }
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
        // 불러오는 중이면 그 카드를 선택된 것으로 표시
        const activeId = loadingId ?? state.templateId;
        const selected = String(t.templateId) === String(activeId);
        const busy = loadingId != null && String(t.templateId) === String(loadingId);
        return `
            <button type="button" class="template-card" role="radio"
                    aria-checked="${selected}" aria-busy="${busy}" data-id="${escapeHtml(t.templateId)}">
                <span class="template-card__thumb">${thumbHtml(t)}</span>
                <span class="template-card__name">${escapeHtml(t.templateName)}</span>
                ${t.description ? `<span class="template-card__desc">${escapeHtml(t.description)}</span>` : ''}
            </button>`;
    }

    /** 검색어로 거른 템플릿 (이름·설명에서 찾음, 대소문자 무시) */
    function visibleTemplates() {
        const q = keyword.trim().toLowerCase();
        if (!q) return templates;
        return templates.filter(t =>
            String(t.templateName ?? '').toLowerCase().includes(q)
            || String(t.description ?? '').toLowerCase().includes(q));
    }

    /** 개수 표시 + 검색창 보이기 (많을 때만, 검색 중이면 계속 보임) */
    function renderTemplateMeta(shown) {
        const total = templates.length;
        countEl.textContent = !total ? '' : (keyword.trim() ? `${shown} / ${total}개` : `${total}개`);
        searchWrap.hidden = total <= SEARCH_MIN && !keyword;
    }

    function renderTemplates() {
        const items = visibleTemplates();
        renderTemplateMeta(items.length);

        if (!templates.length) {
            groupsEl.innerHTML = '<p class="panel-hint">사용할 수 있는 템플릿이 없어요.</p>';
            return;
        }
        if (!items.length) {
            groupsEl.innerHTML = '<p class="panel-hint">검색 결과가 없어요.</p>';
            return;
        }

        // 다시 그려도 스크롤 위치·포커스(방향키 이동) 유지
        const scrollTop = groupsEl.scrollTop;
        const focusedId = groupsEl.contains(document.activeElement)
            ? document.activeElement.dataset.id : null;

        const groups = [
            { title: '기본 템플릿', items: items.filter(t => t.createdBy == null) },
            { title: '내 템플릿',   items: items.filter(t => t.createdBy != null) }
        ].filter(g => g.items.length);

        groupsEl.innerHTML = groups.map(g => `
            <div class="template-group">
                ${groups.length > 1 ? `<h3 class="template-group__title">${g.title} <span>${g.items.length}</span></h3>` : ''}
                <div class="template-grid" role="radiogroup" aria-label="${g.title}">
                    ${g.items.map(cardHtml).join('')}
                </div>
            </div>`).join('');

        groupsEl.scrollTop = scrollTop;
        if (focusedId != null) {
            findCard(focusedId)?.focus({ preventScroll: true });
        }
    }

    function findCard(id) {
        return [...groupsEl.querySelectorAll('.template-card')].find(el => el.dataset.id === String(id)) || null;
    }

    /** 카드 영역 안에서만 스크롤해서 카드를 보이게 (페이지 전체는 움직이지 않음) */
    function revealCard(card) {
        if (!card) return;
        const box = groupsEl.getBoundingClientRect();
        const rect = card.getBoundingClientRect();
        const hasSticky = !!groupsEl.querySelector('.template-group__title');
        const top = box.top + (hasSticky ? STICKY_OFFSET : 0);
        if (rect.top < top) groupsEl.scrollTop -= top - rect.top;
        else if (rect.bottom > box.bottom) groupsEl.scrollTop += rect.bottom - box.bottom;
    }

    function revealSelected() {
        revealCard(findCard(loadingId ?? state.templateId));
    }

    // ---------- 렌더링 : ② 섹션 / ③ CSS ----------
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
        cssResetBtn.disabled = loadingId != null || !current() || sameCss(css, current().cssContent);
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
        state.cssContent = css;
        renderToggles();
        renderCssInfo();
        update({ cssContent: css });
    }

    // ---------- ① 템플릿 선택 (API 로 내용 불러오기) ----------
    /**
     * @param id       선택한 템플릿 ID
     * @param initial  페이지 처음 열 때 (확인창 없음, 저장 상태 기준으로 삼음)
     */
    async function selectTemplate(id, { initial = false } = {}) {
        const summary = findTemplate(id);
        if (!summary) return;
        const tid = summary.templateId;
        const key = String(tid);

        // 이미 선택(또는 불러오는 중)인 템플릿이면 무시
        if (key === String(loadingId ?? state.templateId)) return;

        // 직접 고친 CSS 가 있으면 확인 (섹션 숨김은 고친 것으로 보지 않음)
        // 이미 다른 템플릿을 불러오는 중이면 그때 확인했으므로 다시 묻지 않음
        if (!initial && loadingId == null) {
            const prev = current();
            const edited = prev && !sameCss(splitHidden(state.cssContent).body, splitHidden(prev.cssContent).body);
            if (edited && !confirm('수정한 CSS는 사라지고 선택한 템플릿의 CSS로 바뀌어요. 바꿀까요?')) return;
        }

        loadingId = tid;
        setBusy(true);
        renderTemplates();
        if (initial) revealSelected();
        if (!details.has(key)) setStatus('템플릿 불러오는 중…');

        let detail;
        try {
            detail = await loadTemplate(tid);
        } catch (err) {
            console.error('템플릿 불러오기 실패', err);
            if (String(loadingId) !== key) return;     // 그 사이 다른 템플릿을 고름
            loadingId = null;
            setBusy(false);
            renderTemplates();
            setStatus('템플릿을 불러오지 못했어요. 다시 선택해 주세요.', 'error');
            return;
        }

        // 처음 열 때 이력서에 저장돼 있던 템플릿이면 그 값을 '저장된 상태'로
        if (initial && String(window.CURRENT_TEMPLATE_ID) === key) {
            saved = { templateId: tid, cssContent: detail.cssContent };
        }

        if (String(loadingId) !== key) return;         // 그 사이 다른 템플릿을 고름
        loadingId = null;
        state.templateId = tid;
        setBusy(false);
        renderTemplates();

        // 섹션 표시 설정은 그대로 유지
        const { hidden } = splitHidden(state.cssContent);
        setCss(initial ? detail.cssContent : joinHidden(detail.cssContent, hidden));
    }

    groupsEl.addEventListener('click', e => {
        if (e.target.closest('#templateRetryBtn')) { init(); return; }
        const card = e.target.closest('.template-card');
        if (card) selectTemplate(card.dataset.id);
    });

    // 방향키로 카드 이동 (보이는 카드 안에서만)
    groupsEl.addEventListener('keydown', e => {
        if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(e.key)) return;
        const cards = [...groupsEl.querySelectorAll('.template-card')];
        const i = cards.indexOf(document.activeElement);
        if (i < 0) return;
        e.preventDefault();
        const step = e.key === 'ArrowLeft' || e.key === 'ArrowUp' ? -1 : 1;
        const next = cards[(i + step + cards.length) % cards.length];
        next.focus({ preventScroll: true });
        revealCard(next);
        next.click();
    });

    // ---------- 이벤트: 템플릿 검색 ----------
    searchInput.addEventListener('input', () => {
        keyword = searchInput.value;
        renderTemplates();
        groupsEl.scrollTop = 0;
    });

    searchInput.addEventListener('keydown', e => {
        // Esc : 검색어 지우기
        if (e.key === 'Escape' && searchInput.value) {
            e.preventDefault();
            searchInput.value = '';
            keyword = '';
            renderTemplates();
            revealSelected();
        }
        // ↓ : 첫 번째 카드로 (선택된 카드가 보이면 그 카드로)
        if (e.key === 'ArrowDown') {
            const target = findCard(state.templateId) || groupsEl.querySelector('.template-card');
            if (!target) return;
            e.preventDefault();
            target.focus({ preventScroll: true });
            revealCard(target);
        }
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

    cssResetBtn.addEventListener('click', () => {
        const t = current();
        if (!t || !confirm('수정한 CSS를 지우고 템플릿의 원래 CSS로 되돌릴까요?')) return;
        setCss(t.cssContent);
    });

    // ---------- 저장 / 되돌리기 / 인쇄 ----------
    // 저장 성공 후 처리
    function onSaved() {
        // 이동하기 전에 '저장된 상태'로 맞춰야 beforeunload 의 "나가시겠어요?" 창이 안 뜸
        saved = clone(state);
        setStatus('저장됨', 'saved');

        alert('템플릿 저장 완료');
        location.href = '/web/resume/list';
    }

    saveBtn.addEventListener('click', () => {
        clearTimeout(cssTimer);
        state.cssContent = editor.value;

        if (loadingId != null) return;
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

        callApi({
            url: API.templateSave,
            method: 'POST',
            data: {
                resumeId: RESUME_ID,
                templateId: state.templateId,
                cssContent: state.cssContent
            },
            success: onSaved,
            error: err => {
                console.error('템플릿 저장 실패', err);
                setStatus('저장 실패', 'error');
            },
            complete: () => { saveBtn.disabled = loadingId != null; }
        });
    });

    resetBtn.addEventListener('click', () => {
        if (!saved.templateId) return;
        loadingId = null;                 // 불러오던 템플릿이 있으면 취소
        setBusy(false);
        state.templateId = saved.templateId;   // 저장된 템플릿은 이미 캐시에 있음
        renderTemplates();
        revealSelected();
        setCss(saved.cssContent);
    });

    // ---------- PDF 다운로드 (서버에서 미리보기와 똑같이 만듦) ----------
    /** Content-Disposition 에서 파일 이름 꺼내기 (filename*=UTF-8'' 우선) */
    function fileNameFrom(disposition) {
        if (!disposition) return null;
        const utf8 = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(disposition);
        if (utf8) {
            try { return decodeURIComponent(utf8[1].replace(/"/g, '')); } catch (e) { /* 아래로 */ }
        }
        return /filename\s*=\s*"?([^";]+)"?/i.exec(disposition)?.[1] || null;
    }

    /** blob 을 파일로 내려받기 */
    function downloadBlob(blob, name) {
        const url = URL.createObjectURL(blob);
        const a = Object.assign(document.createElement('a'), { href: url, download: name });
        document.body.append(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    }

    printBtn.addEventListener('click', () => {
        // PDF 는 서버에 저장된 템플릿으로 만들어짐
        if (!saved.templateId) {
            setStatus('템플릿을 먼저 저장해 주세요', 'error');
            return;
        }
        if (isDirty() && !confirm('저장하지 않은 변경은 PDF에 반영되지 않아요.\n저장된 상태로 PDF를 받을까요?')) return;

        printBtn.disabled = true;
        const prevStatus = statusEl.textContent;
        setStatus('PDF 만드는 중…');

        callApi({
            url: API.templatePdf(RESUME_ID),
            responseType: 'blob',
            success: (blob, res) => {
                const name = fileNameFrom(res.headers.get('Content-Disposition')) || '이력서.pdf';
                downloadBlob(blob, name);
                setStatus(isDirty() ? '저장 안 됨' : prevStatus, isDirty() ? 'dirty' : '');
            },
            error: err => {
                console.error('PDF 다운로드 실패', err);
                setStatus('PDF를 만들지 못했어요', 'error');
            },
            complete: () => { printBtn.disabled = false; }
        });
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
    /** 템플릿 목록 → 이력서에 저장된 템플릿(없으면 첫 번째) 내용 순서로 불러옴 */
    async function init() {
        setBusy(true);
        const ok = await loadTemplateList();
        if (!ok) return;                     // 실패하면 편집 막은 채로 '다시 시도' 대기
        setBusy(false);
        renderTemplates();

        const start = findTemplate(window.CURRENT_TEMPLATE_ID) || templates[0] || null;
        if (start) {
            selectTemplate(start.templateId, { initial: true });
        } else {
            update({});
        }
    }

    renderToggles();
    renderCssInfo();
    fitPreview();
    init();
})();