/* =========================================================
   template-form.js  (템플릿 빌더)
   - 설정 변경 → iframe(미리보기)에 postMessage 로 즉시 반영
   - 섹션 표시/숨김 + 드래그(또는 ▲▼)로 순서 변경
   - [설정 저장] → 새 템플릿이면 POST /api/templates, 이후 PUT /api/templates/{id}
   - 이력서 데이터는 쓰지 않음 (미리보기는 자리표시 문구 + 현재 설정)
   ========================================================= */
(() => {
    'use strict';

    // template_section.section_type 과 동일 (profile 은 헤더라 항상 표시 → 목록에서 제외)
    const SECTION_LABELS = {
        intro: '자기소개',
        experience: '경력',
        project: '프로젝트',
        education: '학력',
        certificate: '자격증',
        skill: '기술 스택',
        link: '외부 링크'
    };

    const DEFAULT_SETTING = {
        templateId: null,
        templateName: '새 템플릿',
        templateCode: 'minimal',
        layoutType: 'single',
        primaryColor: '#2D3748',
        fontFamily: 'Pretendard',
        fontScale: 100,
        sections: Object.keys(SECTION_LABELS).map(key => ({ key, visible: true }))
    };

    const A4_WIDTH = 794;

    // ---------- DOM ----------
    const root          = document.querySelector('.builder');
    const sidebar       = document.getElementById('sidebar');
    const sectionListEl = document.getElementById('sectionList');
    const colorInput    = document.getElementById('primaryColor');
    const colorValueEl  = document.getElementById('colorValue');
    const swatchesEl    = document.getElementById('swatches');
    const fontScaleEl   = document.getElementById('fontScale');
    const fontScaleVal  = document.getElementById('fontScaleValue');
    const frame         = document.getElementById('previewFrame');
    const page          = document.getElementById('previewPage');
    const stage         = document.getElementById('previewStage');
    const zoomLabel     = document.getElementById('zoomLabel');
    const saveBtn       = document.getElementById('saveBtn');
    const resetBtn      = document.getElementById('resetBtn');
    const printBtn      = document.getElementById('printBtn');
    const statusEl      = document.getElementById('saveStatus');
    const nameInput     = document.getElementById('templateName');   // 없으면 이름 입력 생략

    // 새 템플릿이면 비어 있음 → 첫 저장 시 POST 후 받은 id 로 이후 PUT
    let templateId = root.dataset.templateId || null;
    const apiUrl   = root.dataset.apiUrl || '/api/templates';

    // ---------- 상태 ----------
    let saved = normalize(window.TEMPLATE_SETTING);
    let state = clone(saved);
    let frameHeight = 1123;

    // ---------- 유틸 ----------
    function clone(obj) { return JSON.parse(JSON.stringify(obj)); }

    /** 서버 값이 비어 있거나 섹션이 빠져 있어도 안전하게 보정 */
    function normalize(raw) {
        const s = Object.assign(clone(DEFAULT_SETTING), raw || {});
        // sections 가 JSON 문자열로 넘어오는 경우 대비
        if (typeof s.sections === 'string') {
            try { s.sections = JSON.parse(s.sections); } catch (e) { s.sections = null; }
        }
        const list = Array.isArray(s.sections) ? s.sections.filter(x => SECTION_LABELS[x.key]) : [];
        Object.keys(SECTION_LABELS).forEach(key => {
            if (!list.some(x => x.key === key)) list.push({ key, visible: true });
        });
        s.sections = list.map(x => ({ key: x.key, visible: x.visible !== false }));
        s.fontScale = Number(s.fontScale) || 100;
        return s;
    }

    function isDirty() { return JSON.stringify(state) !== JSON.stringify(saved); }

    function setStatus(text, type) {
        statusEl.textContent = text;
        statusEl.className = 'save-status text-sm' + (type ? ' is-' + type : '');
    }

    function csrfHeaders() {
        const token  = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    // ---------- 폼 ← state ----------
    function renderForm() {
        if (nameInput) nameInput.value = state.templateName;
        sidebar.querySelectorAll('input[name="templateCode"]').forEach(r => {
            r.checked = r.value === state.templateCode;
        });
        sidebar.querySelectorAll('input[name="layoutType"]').forEach(r => {
            r.checked = r.value === state.layoutType;
        });
        sidebar.querySelector('#fontFamily').value = state.fontFamily;
        fontScaleEl.value = state.fontScale;
        fontScaleVal.textContent = state.fontScale + '%';
        renderColor();
        renderSections();
        applyAtsLock();
    }

    function renderColor() {
        const color = state.primaryColor.toUpperCase();
        colorInput.value = color;
        colorValueEl.textContent = color;
        swatchesEl.querySelectorAll('.swatch[data-color]').forEach(b => {
            b.classList.toggle('is-active', b.dataset.color.toUpperCase() === color);
        });
    }

    /** ATS 템플릿은 한 컬럼 + 흑백 고정 */
    function applyAtsLock() {
        const isAts = state.templateCode === 'ats';
        sidebar.querySelector('.segmented').classList.toggle('is-locked', isAts);
        swatchesEl.classList.toggle('is-locked', isAts);
        sidebar.querySelectorAll('[data-ats-hint]').forEach(p => { p.hidden = !isAts; });
    }

    function renderSections() {
        sectionListEl.innerHTML = '';
        state.sections.forEach((sec, i) => {
            const li = document.createElement('li');
            li.className = 'section-item' + (sec.visible ? '' : ' is-hidden');
            li.draggable = true;
            li.dataset.key = sec.key;
            li.innerHTML = `
                <span class="drag-handle" aria-hidden="true">⋮⋮</span>
                <span class="section-name">${SECTION_LABELS[sec.key]}</span>
                <span class="move-btns">
                    <button type="button" class="btn ghost icon sm" data-move="-1"
                            aria-label="${SECTION_LABELS[sec.key]} 위로" ${i === 0 ? 'disabled' : ''}>▲</button>
                    <button type="button" class="btn ghost icon sm" data-move="1"
                            aria-label="${SECTION_LABELS[sec.key]} 아래로" ${i === state.sections.length - 1 ? 'disabled' : ''}>▼</button>
                </span>
                <label class="switch" title="표시">
                    <input type="checkbox" ${sec.visible ? 'checked' : ''} aria-label="${SECTION_LABELS[sec.key]} 표시">
                    <span></span>
                </label>`;
            sectionListEl.appendChild(li);
        });
    }

    // ---------- state 변경 → 미리보기 ----------
    function update(patch) {
        Object.assign(state, patch);
        renderColor();
        applyAtsLock();
        pushToPreview();
        const dirty = isDirty();
        resetBtn.disabled = !dirty;
        setStatus(dirty ? '저장 안 됨' : '', dirty ? 'dirty' : '');
    }

    function pushToPreview() {
        if (!frame.contentWindow) return;
        frame.contentWindow.postMessage({ type: 'template:apply', setting: state }, location.origin);
    }

    // ---------- 이벤트: 일반 설정 ----------
    sidebar.addEventListener('change', e => {
        const el = e.target;
        switch (el.name) {
            case 'templateCode': update({ templateCode: el.value }); break;
            case 'layoutType':   update({ layoutType: el.value }); break;
            case 'fontFamily':   update({ fontFamily: el.value }); break;
        }
    });

    // 템플릿 이름
    nameInput?.addEventListener('input', () => update({ templateName: nameInput.value }));

    // 색상: 드래그 중에도 실시간 반영
    colorInput.addEventListener('input', () => update({ primaryColor: colorInput.value }));
    swatchesEl.addEventListener('click', e => {
        const b = e.target.closest('.swatch[data-color]');
        if (b) update({ primaryColor: b.dataset.color });
    });

    fontScaleEl.addEventListener('input', () => {
        fontScaleVal.textContent = fontScaleEl.value + '%';
        update({ fontScale: Number(fontScaleEl.value) });
    });

    // ---------- 이벤트: 섹션 표시 / ▲▼ ----------
    sectionListEl.addEventListener('change', e => {
        if (e.target.type !== 'checkbox') return;
        const li = e.target.closest('.section-item');
        const sec = state.sections.find(s => s.key === li.dataset.key);
        sec.visible = e.target.checked;
        li.classList.toggle('is-hidden', !sec.visible);
        update({});
    });

    sectionListEl.addEventListener('click', e => {
        const btn = e.target.closest('[data-move]');
        if (!btn) return;
        const key = btn.closest('.section-item').dataset.key;
        const from = state.sections.findIndex(s => s.key === key);
        const to = from + Number(btn.dataset.move);
        if (to < 0 || to >= state.sections.length) return;
        moveSection(from, to);
        // 같은 항목의 버튼에 포커스 유지 (키보드 사용성)
        const sel = `.section-item[data-key="${key}"] [data-move="${btn.dataset.move}"]`;
        (sectionListEl.querySelector(sel + ':not([disabled])') ||
         sectionListEl.querySelector(`.section-item[data-key="${key}"] [data-move]:not([disabled])`))?.focus();
    });

    function moveSection(from, to) {
        const [item] = state.sections.splice(from, 1);
        state.sections.splice(to, 0, item);
        renderSections();
        update({});
    }

    // ---------- 이벤트: 드래그 정렬 ----------
    let dragKey = null;

    sectionListEl.addEventListener('dragstart', e => {
        const li = e.target.closest('.section-item');
        if (!li) return;
        dragKey = li.dataset.key;
        li.classList.add('is-dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', dragKey); // Firefox 필수
    });

    sectionListEl.addEventListener('dragover', e => {
        const li = e.target.closest('.section-item');
        if (!dragKey || !li || li.dataset.key === dragKey) return;
        e.preventDefault();
        clearDropMarks();
        const rect = li.getBoundingClientRect();
        li.classList.add(e.clientY < rect.top + rect.height / 2 ? 'drop-before' : 'drop-after');
    });

    sectionListEl.addEventListener('drop', e => {
        e.preventDefault();
        const li = e.target.closest('.section-item');
        if (!dragKey || !li || li.dataset.key === dragKey) return;
        const before = li.classList.contains('drop-before');
        const from = state.sections.findIndex(s => s.key === dragKey);
        let to = state.sections.findIndex(s => s.key === li.dataset.key);
        if (!before) to += 1;
        if (from < to) to -= 1;
        moveSection(from, to);
    });

    sectionListEl.addEventListener('dragend', () => {
        dragKey = null;
        clearDropMarks();
        sectionListEl.querySelectorAll('.is-dragging').forEach(el => el.classList.remove('is-dragging'));
    });

    function clearDropMarks() {
        sectionListEl.querySelectorAll('.drop-before, .drop-after')
            .forEach(el => el.classList.remove('drop-before', 'drop-after'));
    }

    // ---------- 저장 / 되돌리기 / 인쇄 ----------
    saveBtn.addEventListener('click', async () => {
        const name = (state.templateName || '').trim();
        if (!name) {
            nameInput?.focus();
            setStatus('템플릿 이름을 입력해 주세요', 'error');
            return;
        }

        saveBtn.disabled = true;
        setStatus('저장 중…');
        try {
            const isNew = !templateId;
            const res = await fetch(isNew ? apiUrl : `${apiUrl}/${templateId}`, {
                method: isNew ? 'POST' : 'PUT',
                headers: Object.assign({ 'Content-Type': 'application/json' }, csrfHeaders()),
                body: JSON.stringify(Object.assign({}, state, { templateName: name }))
            });
            if (!res.ok) throw new Error(res.status);

            if (isNew) {
                const data = await res.json();              // { templateId: 5 }
                templateId = String(data.templateId);
                state.templateId = data.templateId;
                root.dataset.templateId = templateId;
                // 새로고침하면 수정 화면으로 열리도록 주소만 교체
                history.replaceState(null, '', `/templates/${templateId}/edit`);
            }
            saved = clone(state);
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
        state = clone(saved);
        renderForm();
        update({});
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
    renderForm();
    fitPreview();
})();