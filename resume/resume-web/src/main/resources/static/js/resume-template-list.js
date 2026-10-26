/**
 * 내 템플릿 목록
 *  - 썸네일 : A4 미리보기를 카드 너비에 맞춰 축소
 *  - 이력서에 적용 : 이력서를 골라 POST /api/template/apply
 *  - 삭제         : DELETE /api/template/{id}
 */
(function () {
    'use strict';

    var PAGE_WIDTH = 794;
    var TOAST_TIME = 3500;

    var cfg = {};
    var el = {};
    var current = { templateId: null, templateName: '' };
    var toastTimer = null;

    /* ---------- 공통 ---------- */

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
                if (!res.ok) throw new Error(json.message || ('요청을 처리하지 못했어요. (' + res.status + ')'));
                return json;
            });
        });
    }

    /** 아래쪽 알림. link 를 주면 오른쪽에 이동 링크 */
    function toast(message, isError, link) {
        clearTimeout(toastTimer);
        el.toast.replaceChildren(document.createTextNode(message));
        if (link) {
            var a = document.createElement('a');
            a.href = link.href;
            a.textContent = link.text;
            el.toast.appendChild(a);
        }
        el.toast.classList.toggle('is-error', !!isError);
        el.toast.classList.remove('d-none');
        toastTimer = setTimeout(function () { el.toast.classList.add('d-none'); }, link ? TOAST_TIME * 2 : TOAST_TIME);
    }

    /* ---------- 썸네일 크기 ---------- */

    function fitThumb(thumb) {
        var width = thumb.clientWidth;
        if (width > 0) thumb.style.setProperty('--thumb-scale', String(width / PAGE_WIDTH));
    }

    function initThumbs() {
        var thumbs = document.querySelectorAll('.tl-thumb');
        if ('ResizeObserver' in window) {
            var ro = new ResizeObserver(function (entries) {
                entries.forEach(function (entry) { fitThumb(entry.target); });
            });
            thumbs.forEach(function (t) { ro.observe(t); });
        } else {
            var fitAll = function () { thumbs.forEach(fitThumb); };
            fitAll();
            window.addEventListener('resize', fitAll);
        }
    }

    /* ---------- 이력서에 적용 ---------- */

    function openApply(card) {
        current.templateId = Number(card.getAttribute('data-template-id'));
        current.templateName = card.getAttribute('data-template-name') || '';
        el.applyName.textContent = '\'' + current.templateName + '\'';
        updateApplyNote();
        el.modal.showModal();
    }

    function closeApply() {
        el.modal.close();
    }

    function selectedOption() {
        return el.applyResume ? el.applyResume.options[el.applyResume.selectedIndex] : null;
    }

    function updateApplyNote() {
        var option = selectedOption();
        var using = option && Number(option.getAttribute('data-current-template')) === current.templateId;
        el.applyNote.textContent = using ? '이 이력서는 이미 이 템플릿을 쓰고 있어요.' : '';
    }

    function confirmApply() {
        var option = selectedOption();
        if (!option) return;
        var resumeId = Number(option.value);
        var resumeTitle = option.textContent.trim();

        el.applyConfirm.disabled = true;
        request('POST', cfg.applyUrl, { resumeId: resumeId, templateId: current.templateId })
            .then(function () {
                option.setAttribute('data-current-template', String(current.templateId));
                closeApply();
                toast('\'' + resumeTitle + '\' 에 적용했어요.', false,
                    { href: cfg.resumeTemplateUrl + resumeId, text: '확인하러 가기' });
            })
            .catch(function (err) { toast(err.message, true); })
            .finally(function () { el.applyConfirm.disabled = false; });
    }

    /* ---------- 삭제 ---------- */

    function deleteTemplate(card) {
        var templateId = card.getAttribute('data-template-id');
        var name = card.getAttribute('data-template-name') || '';
        if (!confirm('\'' + name + '\' 템플릿을 삭제할까요?')) return;

        card.classList.add('is-deleting');
        request('DELETE', cfg.deleteUrl + templateId)
            .then(function () {
                card.classList.remove('is-deleting');
                card.classList.add('is-removed');
                card.addEventListener('animationend', function () {
                    card.remove();
                    updateMyCount();
                }, { once: true });
                toast('\'' + name + '\' 템플릿을 삭제했어요.');
            })
            .catch(function (err) {
                card.classList.remove('is-deleting');
                toast(err.message, true);
            });
    }

    function updateMyCount() {
        var count = el.myGrid ? el.myGrid.querySelectorAll('.tl-card').length : 0;
        if (el.myCount) el.myCount.textContent = String(count);
        if (count === 0) {
            if (el.myGrid) el.myGrid.classList.add('d-none');
            if (el.myEmpty) el.myEmpty.classList.remove('d-none');
        }
    }

    /* ---------- 시작 ---------- */

    document.addEventListener('DOMContentLoaded', function () {
        var body = document.body;
        cfg = {
            applyUrl: body.getAttribute('data-apply-url'),
            deleteUrl: body.getAttribute('data-delete-url'),
            resumeTemplateUrl: body.getAttribute('data-resume-template-url')
        };

        el.modal = document.getElementById('applyModal');
        el.applyName = document.getElementById('applyTemplateName');
        el.applyResume = document.getElementById('applyResume');
        el.applyNote = document.getElementById('applyNote');
        el.applyConfirm = document.getElementById('applyConfirm');
        el.toast = document.getElementById('listToast');
        el.myGrid = document.getElementById('myTemplateGrid');
        el.myCount = document.getElementById('myCount');
        el.myEmpty = document.getElementById('myEmpty');

        initThumbs();

        document.addEventListener('click', function (e) {
            var applyBtn = e.target.closest('.js-apply');
            if (applyBtn) { openApply(applyBtn.closest('.tl-card')); return; }
            var deleteBtn = e.target.closest('.js-delete');
            if (deleteBtn) deleteTemplate(deleteBtn.closest('.tl-card'));
        });

        el.modal.querySelectorAll('[data-close]').forEach(function (btn) {
            btn.addEventListener('click', closeApply);
        });
        // 바깥(backdrop) 클릭 시 닫기
        el.modal.addEventListener('click', function (e) {
            if (e.target === el.modal) closeApply();
        });
        el.applyConfirm.addEventListener('click', confirmApply);
        if (el.applyResume) el.applyResume.addEventListener('change', updateApplyNote);
    });
})();