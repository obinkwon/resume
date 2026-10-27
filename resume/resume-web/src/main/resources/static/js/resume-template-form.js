/**
 * 템플릿 편집 화면 resume-template-form (티스토리 스킨 편집 방식)
 *  - 내 템플릿 목록에서만 들어옴 (이력서와 연결하지 않음)
 *  - 왼쪽: HTML / CSS 편집, 치환자 삽입
 *  - 상단: 템플릿 이름 / 설명 편집
 *  - 오른쪽: 샘플 데이터 미리보기 iframe 에 postMessage 로 실시간 반영
 *      · 보냄  template-preview { html, css }
 *      · 받음  preview-ready / template-rendered / preview-height
 *      · 내용이 A4 한 장보다 길면 미리보기 높이를 늘려 끝까지 보여 줌
 *  - 왼쪽에 쓴 내용만 오른쪽에 나옴
 *      · HTML 이 비어 있는 템플릿은 기본 구성(치환자 9개)을 왼쪽에 채워서 시작
 *      · 왼쪽 HTML 을 다 지우면 오른쪽도 빈 화면
 *  - 화면 모드 (body[data-mode])
 *      · C(생성) : 새 템플릿, 기본 템플릿 복사 → 저장하면 새 템플릿으로 추가
 *      · U(수정) : 내 템플릿 편집 → 저장하면 덮어쓰기 ([새 템플릿으로 저장] 도 가능)
 *      · C 에서 한 번 저장하면 그 템플릿을 수정하는 U 로 바뀜
 */
(function () {
    'use strict';

    var ALLOWED = ['profile', 'intro', 'cover', 'experience', 'project', 'education', 'certificate', 'skill', 'link'];
    var PLACEHOLDER = /\{\{\s*([^}]*?)\s*\}\}/g;
    var FORBIDDEN_TAGS = ['script', 'iframe', 'frame', 'frameset', 'object', 'embed', 'link', 'meta', 'base', 'form', 'input', 'button', 'textarea', 'select', 'style'];
    var MAX_LENGTH = 100000;
    var MAX_DESC_LENGTH = 200;
    var RENDER_DELAY = 300;
    var PAGE_WIDTH = 794;      // A4
    var PAGE_HEIGHT = 1123;

    /** 처음 시작할 때 HTML (미리보기의 기본 출력 순서와 같음) */
    var DEFAULT_HTML = ALLOWED.map(function (n) { return '{{' + n + '}}'; }).join('\n') + '\n';
    /**
     * HTML 이 비었을 때 미리보기에 보낼 값
     * (미리보기는 빈 HTML 을 받으면 모든 섹션을 기본 순서로 그리므로, 내용 없는 주석을 보내 빈 화면으로 만듦)
     */
    var EMPTY_PREVIEW_HTML = '<!-- empty -->';

    /** 화면 모드 */
    var MODE = { CREATE: 'C', UPDATE: 'U' };

    var el = {};
    var cfg = {};
    var state = {
        tab: 'html',
        ready: false,
        saving: false,
        mode: MODE.CREATE,    // C : 생성 / U : 수정(덮어쓰기 가능)
        baseTemplateId: null,
        available: [],
        original: { html: '', css: '', name: '', description: '' },
        frameHeight: PAGE_HEIGHT,
        timer: null
    };

    /* ---------------- 검사 ---------------- */

    /** HTML/CSS 검사. errors 가 있으면 저장 불가, warnings 는 안내만 */
    function validate(html, css) {
        var errors = [];
        var warnings = [];
        var used = {};
        var match;

        PLACEHOLDER.lastIndex = 0;
        while ((match = PLACEHOLDER.exec(html)) !== null) {
            var name = match[1];
            if (ALLOWED.indexOf(name) < 0) {
                errors.push('없는 치환자입니다: {{' + name + '}}');
            } else if (used[name]) {
                errors.push('{{' + name + '}} 를 두 번 이상 썼습니다. 한 번만 쓸 수 있어요.');
            }
            used[name] = true;
        }

        // 태그 속성 안에 치환자를 넣으면 안 됨 (<div class="{{skill}}"> 등)
        if (/<[^>]*\{\{[^}]*\}\}[^>]*>/.test(html)) {
            errors.push('치환자는 태그 안(속성 값)에 넣을 수 없습니다. 태그 사이에 넣어 주세요.');
        }

        FORBIDDEN_TAGS.forEach(function (tag) {
            if (new RegExp('<\\s*' + tag + '[\\s>/]', 'i').test(html)) {
                errors.push('<' + tag + '> 태그는 쓸 수 없습니다.' + (tag === 'style' ? ' 스타일은 CSS 탭에 작성해 주세요.' : ''));
            }
        });
        if (/<[^>]+\son[a-z]+\s*=/i.test(html)) {
            errors.push('onclick 같은 이벤트 속성은 쓸 수 없습니다.');
        }
        if (/(javascript|vbscript)\s*:/i.test(html) || /(javascript|vbscript)\s*:/i.test(css)) {
            errors.push('javascript: 주소는 쓸 수 없습니다.');
        }
        if (/<\/\s*style/i.test(css)) {
            errors.push('CSS 에 </style> 은 쓸 수 없습니다.');
        }
        if (/expression\s*\(/i.test(css)) {
            errors.push('CSS expression() 은 쓸 수 없습니다.');
        }
        if (html.length > MAX_LENGTH) errors.push('HTML 이 너무 깁니다. (최대 ' + MAX_LENGTH.toLocaleString() + '자)');
        if (css.length > MAX_LENGTH) errors.push('CSS 가 너무 깁니다. (최대 ' + MAX_LENGTH.toLocaleString() + '자)');

        if (html.trim() === '') {
            // 저장된 빈 HTML 은 실제 이력서에서 모든 섹션이 나오므로, 화면과 다르지 않게 저장을 막음
            errors.push('HTML 이 비어 있어요. 치환자를 하나 이상 넣어 주세요.');
        } else {
            var missing = ALLOWED.filter(function (n) { return !used[n]; });
            if (missing.length) {
                warnings.push('넣지 않은 섹션: ' + missing.map(function (n) { return '{{' + n + '}}'; }).join(', '));
            }
        }

        return { errors: errors, warnings: warnings, used: used };
    }

    function showMessages(result) {
        el.messages.replaceChildren();
        result.errors.forEach(function (text) { addMessage(text, 'error'); });
        result.warnings.forEach(function (text) { addMessage(text, 'warn'); });
        updateSaveButtons(result.errors.length > 0);
    }

    function addMessage(text, type) {
        var li = document.createElement('li');
        li.className = 'te-msg te-msg--' + type;
        li.textContent = text;
        el.messages.appendChild(li);
    }

    function updateSaveButtons(hasError) {
        var disabled = hasError || state.saving;
        el.btnSave.disabled = disabled;
        el.btnSaveAsNew.disabled = disabled;
    }

    function updateVarList(used) {
        el.varItems.forEach(function (li) {
            var name = li.getAttribute('data-var');
            li.classList.toggle('is-used', !!used[name]);
            li.classList.toggle('is-empty', state.ready && state.available.indexOf(name) < 0);
        });
    }

    /* ---------------- 미리보기 ---------------- */

    function sendPreview() {
        var html = el.html.value;
        var css = el.css.value;
        var result = validate(html, css);

        showMessages(result);
        updateVarList(result.used);

        if (!state.ready) return;
        el.frame.contentWindow.postMessage({
            type: 'template-preview',
            html: html.trim() ? html : EMPTY_PREVIEW_HTML,
            css: css
        }, window.location.origin);
    }

    function schedulePreview() {
        clearTimeout(state.timer);
        state.timer = setTimeout(sendPreview, RENDER_DELAY);
    }

    function onFrameMessage(event) {
        if (event.origin !== window.location.origin) return;
        if (event.source !== el.frame.contentWindow) return;
        var data = event.data || {};

        if (data.type === 'preview-ready') {
            state.ready = true;
            state.available = data.available || [];
            sendPreview();
        } else if (data.type === 'template-rendered') {
            state.available = data.available || state.available;
            updateVarList(validate(el.html.value, el.css.value).used);
        } else if (data.type === 'preview-height') {
            // 내용이 길면 미리보기를 늘림 (A4 한 장보다 작아지지는 않음)
            state.frameHeight = Math.max(PAGE_HEIGHT, Math.ceil(Number(data.height) || 0));
            fitPreview();
        }
    }

    /** A4 너비 미리보기를 미리보기 영역 너비에 맞춰 축소 (최대 100%), 높이는 내용에 맞춤 */
    function fitPreview() {
        var style = window.getComputedStyle(el.stage);
        var padding = parseFloat(style.paddingLeft) + parseFloat(style.paddingRight);
        var available = el.stage.clientWidth - padding;
        if (available <= 0) return;

        var scale = Math.min(1, available / PAGE_WIDTH);
        el.frame.style.height = state.frameHeight + 'px';
        el.frame.style.transform = 'scale(' + scale + ')';
        el.page.style.width = Math.floor(PAGE_WIDTH * scale) + 'px';
        el.page.style.height = Math.floor(state.frameHeight * scale) + 'px';
        if (el.zoom) el.zoom.textContent = Math.round(scale * 100) + '%';
    }

    function initPreviewSize() {
        if ('ResizeObserver' in window) {
            new ResizeObserver(fitPreview).observe(el.stage);
        } else {
            window.addEventListener('resize', fitPreview);
        }
        fitPreview();
    }

    function reloadFrame() {
        state.ready = false;
        el.frame.src = cfg.previewUrl;
    }

    /* ---------------- 편집 ---------------- */

    function currentEditor() {
        return state.tab === 'html' ? el.html : el.css;
    }

    function switchTab(tab) {
        state.tab = tab;
        el.tabs.forEach(function (btn) {
            var active = btn.getAttribute('data-tab') === tab;
            btn.classList.toggle('is-active', active);
            btn.setAttribute('aria-selected', active ? 'true' : 'false');
        });
        el.html.classList.toggle('is-active', tab === 'html');
        el.css.classList.toggle('is-active', tab === 'css');
        currentEditor().focus();
    }

    /** 커서 위치에 글자 넣기 (실행 취소가 되도록 execCommand 우선 사용) */
    function insertText(textarea, text) {
        textarea.focus();
        var ok = false;
        try { ok = document.execCommand('insertText', false, text); } catch (e) { ok = false; }
        if (!ok) {
            var start = textarea.selectionStart;
            var end = textarea.selectionEnd;
            textarea.value = textarea.value.slice(0, start) + text + textarea.value.slice(end);
            textarea.selectionStart = textarea.selectionEnd = start + text.length;
            onInput();
        }
        // execCommand 성공 시 input 이벤트가 발생해 onInput 이 호출됨
    }

    function onInput() {
        updateDirty();
        schedulePreview();
    }

    /** Tab 키로 들여쓰기, Ctrl/Cmd+S 저장 */
    function onKeyDown(event) {
        if (isSaveKey(event)) {
            event.preventDefault();
            save(false);
            return;
        }
        if (event.key === 'Tab' && !event.ctrlKey && !event.altKey && !event.shiftKey) {
            event.preventDefault();
            insertText(event.target, '    ');
        }
    }

    /** 이름 / 설명 입력칸 : Ctrl/Cmd+S 저장, Enter 로 폼 제출 방지 */
    function onMetaKeyDown(event) {
        if (isSaveKey(event)) {
            event.preventDefault();
            save(false);
        } else if (event.key === 'Enter') {
            event.preventDefault();
        }
    }

    function isSaveKey(event) {
        return (event.ctrlKey || event.metaKey) && event.key && event.key.toLowerCase() === 's';
    }

    function onVarClick(event) {
        var li = event.target.closest('li[data-var]');
        if (!li) return;
        if (state.tab !== 'html') switchTab('html');
        insertText(el.html, '{{' + li.getAttribute('data-var') + '}}');
    }

    function reset() {
        if (!isDirty()) return;
        if (!confirm('편집한 내용을 버리고 마지막 저장 상태로 되돌릴까요?')) return;
        el.html.value = state.original.html;
        el.css.value = state.original.css;
        el.name.value = state.original.name;
        el.desc.value = state.original.description;
        updateDirty();
        sendPreview();
    }

    /** 마지막 저장(또는 처음 연) 상태와 다른가 */
    function isDirty() {
        return el.html.value !== state.original.html
            || el.css.value !== state.original.css
            || el.name.value.trim() !== state.original.name.trim()
            || el.desc.value.trim() !== state.original.description.trim();
    }

    /** 바뀐 게 있는지는 [되돌리기] 활성화로만 알림 (상태 문구는 비움) */
    function updateDirty() {
        el.btnReset.disabled = !isDirty();
        setStatus('');
    }

    function setStatus(text, type) {
        el.status.textContent = text;
        el.status.className = 'save-status text-sm' + (type ? ' is-' + type : '');
    }

    function isUpdateMode() {
        return state.mode === MODE.UPDATE;
    }

    /** U(수정) : [새 템플릿으로 저장] + [저장](덮어쓰기) / C(생성) : [새 템플릿으로 저장] 하나 */
    function setMode(mode) {
        state.mode = mode === MODE.UPDATE ? MODE.UPDATE : MODE.CREATE;
        el.btnSaveText.textContent = isUpdateMode() ? '저장' : '새 템플릿으로 저장';
        el.btnSaveAsNew.classList.toggle('d-none', !isUpdateMode());
    }

    /* ---------------- 저장 ---------------- */

    function csrfHeaders() {
        var headers = { 'Content-Type': 'application/json' };
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header) headers[header.content] = token.content;
        return headers;
    }

    function save(asNew) {
        if (state.saving) return;

        var html = el.html.value;
        var css = el.css.value;
        var name = el.name.value.trim();
        var description = el.desc.value.trim();
        var result = validate(html, css);

        showMessages(result);
        if (result.errors.length) {
            alert('고쳐야 할 부분이 있습니다. 편집기 아래 메시지를 확인해 주세요.');
            return;
        }
        if (!name) {
            alert('템플릿 이름을 입력해 주세요.');
            el.name.focus();
            return;
        }
        if (description.length > MAX_DESC_LENGTH) {
            alert('설명은 ' + MAX_DESC_LENGTH + '자까지 쓸 수 있어요.');
            el.desc.focus();
            return;
        }

        var createsNew = asNew || !isUpdateMode();
        var payload = {
            baseTemplateId: state.baseTemplateId,
            saveAsNew: createsNew,
            templateName: name,
            description: description,
            htmlContent: html,
            cssContent: css
        };

        state.saving = true;
        updateSaveButtons(false);
        setStatus('저장 중…', 'saving');

        fetch(cfg.saveUrl, {
            method: 'POST',
            headers: csrfHeaders(),
            credentials: 'same-origin',
            body: JSON.stringify(payload)
        })
            .then(function (res) {
                return res.json().catch(function () { return {}; }).then(function (json) {
                    if (!res.ok) throw new Error(json.message || ('저장 실패 (' + res.status + ')'));
                    return json;
                });
            })
            .then(function (json) {
                var templateId = json.templateId != null ? json.templateId : (json.data && json.data.templateId);

                // 이제부터는 저장한 템플릿(내 템플릿)을 수정하는 상태
                if (templateId != null) {
                    state.baseTemplateId = Number(templateId);
                    setMode(MODE.UPDATE);
                    if (window.history && history.replaceState) {
                        history.replaceState(null, '', cfg.editUrl + templateId);
                    }
                }
                state.original = { html: html, css: css, name: name, description: description };
                updateDirty();
                setStatus(createsNew ? '새 템플릿으로 저장됨' : '저장됨', 'saved');

                if (confirm('저장했습니다. 내 템플릿 목록으로 갈까요?')) {
                    window.location.href = cfg.backUrl;
                }
            })
            .catch(function (err) {
                setStatus('저장 실패', 'error');
                alert(err.message || '저장 중 문제가 생겼습니다.');
            })
            .finally(function () {
                state.saving = false;
                updateSaveButtons(validate(el.html.value, el.css.value).errors.length > 0);
            });
    }

    /* ---------------- 시작 ---------------- */

    function readConfig() {
        var body = document.body;
        var baseId = body.getAttribute('data-base-template-id');
        cfg = {
            previewUrl: body.getAttribute('data-preview-url'),
            saveUrl: body.getAttribute('data-save-url'),
            editUrl: body.getAttribute('data-edit-url'),
            backUrl: body.getAttribute('data-back-url')
        };
        state.baseTemplateId = baseId ? Number(baseId) : null;
        state.mode = body.getAttribute('data-mode');
    }

    document.addEventListener('DOMContentLoaded', function () {
        readConfig();

        el.html = document.getElementById('htmlEditor');
        el.css = document.getElementById('cssEditor');
        el.name = document.getElementById('templateName');
        el.desc = document.getElementById('templateDescription');
        el.frame = document.getElementById('previewFrame');
        el.messages = document.getElementById('messages');
        el.status = document.getElementById('saveStatus');
        el.btnSave = document.getElementById('btnSave');
        el.btnSaveAsNew = document.getElementById('btnSaveAsNew');
        el.btnSaveText = document.getElementById('btnSaveText');
        el.btnReset = document.getElementById('btnReset');
        el.stage = document.getElementById('previewStage');
        el.page = document.getElementById('previewPage');
        el.zoom = document.getElementById('zoomLabel');
        el.tabs = Array.prototype.slice.call(document.querySelectorAll('.te-tab'));
        el.varItems = Array.prototype.slice.call(document.querySelectorAll('#varList li[data-var]'));

        // HTML 이 비어 있으면 미리보기에 나오는 기본 구성을 왼쪽에도 채워서 시작 (수정한 것으로 보지 않음)
        if (el.html.value.trim() === '') el.html.value = DEFAULT_HTML;

        state.original = {
            html: el.html.value,
            css: el.css.value,
            name: el.name.value,
            description: el.desc.value
        };
        setMode(state.mode);
        updateDirty();

        el.tabs.forEach(function (btn) {
            btn.addEventListener('click', function () { switchTab(btn.getAttribute('data-tab')); });
        });
        [el.html, el.css].forEach(function (ta) {
            ta.addEventListener('input', onInput);
            ta.addEventListener('keydown', onKeyDown);
        });
        [el.name, el.desc].forEach(function (input) {
            input.addEventListener('input', updateDirty);
            input.addEventListener('keydown', onMetaKeyDown);
        });
        document.getElementById('varList').addEventListener('click', onVarClick);
        document.getElementById('btnRefresh').addEventListener('click', reloadFrame);
        el.btnReset.addEventListener('click', reset);
        el.btnSave.addEventListener('click', function () { save(false); });
        el.btnSaveAsNew.addEventListener('click', function () { save(true); });

        window.addEventListener('message', onFrameMessage);
        window.addEventListener('beforeunload', function (e) {
            if (isDirty()) { e.preventDefault(); e.returnValue = ''; }
        });

        initPreviewSize();
        sendPreview();   // 검사 메시지 먼저 표시
        reloadFrame();   // 미리보기 로드 → preview-ready 를 받으면 반영
    });
})();