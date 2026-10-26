/**
 * 내 템플릿 목록
 *  - 썸네일 : A4 미리보기를 카드 너비에 맞춰 축소
 *  - 삭제   : DELETE /api/template/{id}
 */
(function () {
    'use strict';

    var PAGE_WIDTH = 794;
    var TOAST_TIME = 3500;
    var REMOVE_TIME = 300;   // 삭제 애니메이션(.22s)보다 조금 길게

    var cfg = {};
    var el = {};
    var toastTimer = null;

    /* ---------- 공통 ---------- */

    function csrfHeaders() {
        var headers = { 'Content-Type': 'application/json' };
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header) headers[header.content] = token.content;
        return headers;
    }

    function request(method, url) {
        return fetch(url, {
            method: method,
            headers: csrfHeaders(),
            credentials: 'same-origin'
        }).then(function (res) {
            return res.json().catch(function () { return {}; }).then(function (json) {
                if (!res.ok) throw new Error(json.message || ('요청을 처리하지 못했어요. (' + res.status + ')'));
                return json;
            });
        });
    }

    /** 아래쪽 알림 */
    function toast(message, isError) {
        clearTimeout(toastTimer);
        el.toast.textContent = message;
        el.toast.classList.toggle('is-error', !!isError);
        el.toast.classList.remove('d-none');
        toastTimer = setTimeout(function () { el.toast.classList.add('d-none'); }, TOAST_TIME);
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

    /* ---------- 삭제 ---------- */

    function deleteTemplate(card) {
        if (!card || card.classList.contains('is-deleting')) return;

        var templateId = card.getAttribute('data-template-id');
        var name = card.getAttribute('data-template-name') || '';
        if (!confirm('\'' + name + '\' 템플릿을 삭제할까요?')) return;

        card.classList.add('is-deleting');
        request('DELETE', cfg.deleteUrl + templateId)
            .then(function () {
                card.classList.remove('is-deleting');
                card.classList.add('is-removed');
                // 애니메이션이 꺼져 있으면 animationend 가 오지 않으므로 시간으로 지움
                setTimeout(function () {
                    card.remove();
                    updateMyCount();
                }, REMOVE_TIME);
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
        cfg = {
            deleteUrl: document.body.getAttribute('data-delete-url')
        };

        el.toast = document.getElementById('listToast');
        el.myGrid = document.getElementById('myTemplateGrid');
        el.myCount = document.getElementById('myCount');
        el.myEmpty = document.getElementById('myEmpty');

        initThumbs();

        document.addEventListener('click', function (e) {
            var deleteBtn = e.target.closest('.js-delete');
            if (deleteBtn) deleteTemplate(deleteBtn.closest('.tl-card'));
        });
    });
})();