/**
 * 내 템플릿 목록
 *  - 이력서에 적용 : 이력서를 골라 POST /api/template/apply
 *  - 삭제         : DELETE /api/template/{id}
 */
(function () {
    'use strict';

    var cfg = {};
    var el = {};
    var current = { templateId: null, templateName: '' };

    function csrfHeaders() {
        var headers = { 'Content-Type': 'application/json' };
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header) headers[header.content] = token.content;
        return headers;
    }

    function request(method, url, body) {
        return fetch(url, {
            method: method,
            headers: csrfHeaders(),
            credentials: 'same-origin',
            body: body ? JSON.stringify(body) : undefined
        }).then(function (res) {
            return res.json().catch(function () { return {}; }).then(function (json) {
                if (!res.ok) throw new Error(json.message || ('요청 실패 (' + res.status + ')'));
                return json;
            });
        });
    }

    /* ---------- 이력서에 적용 ---------- */

    function openApply(btn) {
        current.templateId = Number(btn.getAttribute('data-template-id'));
        current.templateName = btn.getAttribute('data-template-name') || '';
        el.applyName.textContent = current.templateName;
        updateApplyNote();

        if (typeof el.dialog.showModal === 'function') {
            el.dialog.showModal();
        } else {
            el.dialog.setAttribute('open', '');
        }
    }

    function closeApply() {
        if (typeof el.dialog.close === 'function') el.dialog.close();
        else el.dialog.removeAttribute('open');
    }

    function updateApplyNote() {
        if (!el.applyResume) return;
        var option = el.applyResume.options[el.applyResume.selectedIndex];
        var currentTemplate = option ? Number(option.getAttribute('data-current-template')) : null;
        el.applyNote.textContent = currentTemplate === current.templateId ? '이 이력서는 이미 이 템플릿을 쓰고 있어요.' : '';
    }

    function confirmApply() {
        if (!el.applyResume) return;
        var resumeId = Number(el.applyResume.value);
        el.applyConfirm.disabled = true;

        request('POST', cfg.applyUrl, { resumeId: resumeId, templateId: current.templateId })
            .then(function () {
                closeApply();
                if (confirm('이력서에 적용했습니다. 그 이력서의 템플릿 화면으로 갈까요?')) {
                    window.location.href = cfg.resumeTemplateUrl + resumeId;
                } else {
                    // 다음에 열 때 '이미 쓰는 중' 안내가 맞도록 갱신
                    var option = el.applyResume.options[el.applyResume.selectedIndex];
                    if (option) option.setAttribute('data-current-template', String(current.templateId));
                }
            })
            .catch(function (err) { alert(err.message); })
            .finally(function () { el.applyConfirm.disabled = false; });
    }

    /* ---------- 삭제 ---------- */

    function deleteTemplate(btn) {
        var templateId = btn.getAttribute('data-template-id');
        var name = btn.getAttribute('data-template-name') || '';
        if (!confirm('\'' + name + '\' 템플릿을 삭제할까요?')) return;

        btn.disabled = true;
        request('DELETE', cfg.deleteUrl + templateId)
            .then(function () {
                var card = btn.closest('.tl-card');
                var grid = card && card.parentElement;
                if (card) card.remove();
                if (grid && !grid.querySelector('.tl-card')) window.location.reload();
            })
            .catch(function (err) {
                alert(err.message);
                btn.disabled = false;
            });
    }

    /* ---------- 시작 ---------- */

    document.addEventListener('DOMContentLoaded', function () {
        var body = document.body;
        cfg = {
            applyUrl: body.getAttribute('data-apply-url'),
            deleteUrl: body.getAttribute('data-delete-url'),
            resumeTemplateUrl: body.getAttribute('data-resume-template-url')
        };

        el.dialog = document.getElementById('applyDialog');
        el.applyName = document.getElementById('applyTemplateName');
        el.applyResume = document.getElementById('applyResume');
        el.applyNote = document.getElementById('applyNote');
        el.applyConfirm = document.getElementById('applyConfirm');

        document.addEventListener('click', function (e) {
            var applyBtn = e.target.closest('.js-apply');
            if (applyBtn) { openApply(applyBtn); return; }
            var deleteBtn = e.target.closest('.js-delete');
            if (deleteBtn) deleteTemplate(deleteBtn);
        });

        document.getElementById('applyCancel').addEventListener('click', closeApply);
        el.applyConfirm.addEventListener('click', confirmApply);
        if (el.applyResume) el.applyResume.addEventListener('change', updateApplyNote);

        // 바깥(backdrop) 클릭 시 닫기
        el.dialog.addEventListener('click', function (e) {
            if (e.target === el.dialog) closeApply();
        });
    });
})();