/**
 * 내 템플릿 목록
 *  - 목록   : GET /api/template/list (기본 템플릿 + 내 템플릿) → 둘로 나눠 카드로 그림
 *  - 썸네일 : 샘플 데이터 미리보기(A4)를 카드 너비에 맞춰 축소
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
    var thumbObserver = null;

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

    function show(node, visible) {
        if (node) node.classList.toggle('d-none', !visible);
    }

    /** 주소 뒤에 값이 있는 파라미터만 붙임 */
    function withParams(base, params) {
        var query = Object.keys(params)
            .filter(function (k) { return params[k] != null && params[k] !== ''; })
            .map(function (k) { return encodeURIComponent(k) + '=' + encodeURIComponent(params[k]); })
            .join('&');
        return query ? base + (base.indexOf('?') < 0 ? '?' : '&') + query : base;
    }

    function editUrl(id) { return withParams(cfg.editUrl + encodeURIComponent(id), { resumeId: cfg.resumeId }); }
    function copyUrl(id) { return withParams(cfg.newUrl, { from: id, resumeId: cfg.resumeId }); }
    function previewUrl(id) { return withParams(cfg.previewUrl, { templateId: id }); }

    /** 요소 만들기 (글자는 textContent 로만 넣어서 HTML 로 해석되지 않게) */
    function h(tag, attrs, children) {
        var node = document.createElement(tag);
        Object.keys(attrs || {}).forEach(function (key) {
            var value = attrs[key];
            if (value == null) return;
            if (key === 'text') node.textContent = value;
            else node.setAttribute(key, value);
        });
        (children || []).forEach(function (child) {
            if (child == null) return;
            node.appendChild(typeof child === 'string' ? document.createTextNode(child) : child);
        });
        return node;
    }

    function icon(name) {
        return h('i', { 'class': 'icon ' + name + ' sm', 'aria-hidden': 'true' });
    }

    /* ---------- 썸네일 크기 ---------- */

    function fitThumb(thumb) {
        var width = thumb.clientWidth;
        if (width > 0) thumb.style.setProperty('--thumb-scale', String(width / PAGE_WIDTH));
    }

    function initThumbObserver() {
        if ('ResizeObserver' in window) {
            thumbObserver = new ResizeObserver(function (entries) {
                entries.forEach(function (entry) { fitThumb(entry.target); });
            });
        } else {
            window.addEventListener('resize', function () {
                document.querySelectorAll('.tl-thumb').forEach(fitThumb);
            });
        }
    }

    /** 새로 만든 썸네일도 크기 맞춤 대상에 넣음 */
    function watchThumb(thumb) {
        if (thumbObserver) thumbObserver.observe(thumb);
        else fitThumb(thumb);
    }

    /* ---------- 카드 ---------- */

    /**
     * @param t     템플릿 (templateId, templateName, description)
     * @param mine  true : 내 템플릿 (편집·삭제) / false : 기본 템플릿 (복사해서 만들기)
     */
    function buildCard(t, mine) {
        var id = t.templateId;
        var name = t.templateName || '이름 없는 템플릿';
        var link = mine ? editUrl(id) : copyUrl(id);

        var thumb = h('a', {
            'class': 'tl-thumb',
            href: link,
            'aria-label': name + (mine ? ' 편집' : ' 복사해서 만들기')
        }, [
            h('iframe', {
                src: previewUrl(id),
                loading: 'lazy',
                tabindex: '-1',
                'aria-hidden': 'true',
                title: ''
            })
        ]);

        var head = h('div', { 'class': 'tl-card__head' }, [
            h('h3', { 'class': 'ellipsis', title: name, text: name }),
            mine
                ? h('button', {
                    type: 'button',
                    'class': 'btn icon sm danger-ghost tl-card__delete js-delete',
                    title: '삭제',
                    'aria-label': name + ' 삭제'
                }, [icon('icon-trash')])
                : h('span', { 'class': 'badge', text: '기본' })
        ]);

        var desc = t.description
            ? h('p', { 'class': 'tl-card__desc', text: t.description })
            : h('p', { 'class': 'tl-card__desc is-empty', text: '설명이 없어요' });

        var actions = h('div', { 'class': 'actions' }, [
            mine
                ? h('a', { href: link }, [icon('icon-edit'), ' 편집'])
                : h('a', { 'class': 'primary', href: link }, [icon('icon-copy'), ' 복사해서 만들기'])
        ]);

        var card = h('article', {
            'class': 'tl-card',
            'data-template-id': id,
            'data-template-name': name
        }, [thumb, head, desc, actions]);

        watchThumb(thumb);
        return card;
    }

    /* ---------- 목록 ---------- */

    /** API 응답에서 배열 꺼내기 (배열 그대로 또는 { data: [...] }) */
    function toList(json) {
        if (Array.isArray(json)) return json;
        if (json && Array.isArray(json.data)) return json.data;
        return [];
    }

    /**
     * 기본 템플릿인가
     *  - isPublic 이 오면 그 값으로
     *  - 없으면 템플릿 선택 화면(resume-template.js)과 같이 createdBy 가 없으면 기본
     */
    function isPublicTemplate(t) {
        console.log('t ::: ',t)
        if (t.isPublic != null) return t.isPublic === true;
        return t.createdBy == null;
    }

    function renderList(list) {
        var mine = list.filter(function (t) { return !isPublicTemplate(t); });
        var publics = list.filter(isPublicTemplate);

        el.myGrid.replaceChildren.apply(el.myGrid, mine.map(function (t) { return buildCard(t, true); }));
        el.publicGrid.replaceChildren.apply(el.publicGrid, publics.map(function (t) { return buildCard(t, false); }));

        el.myCount.textContent = String(mine.length);
        show(el.myCount, true);
        show(el.myGrid, mine.length > 0);
        show(el.myEmpty, mine.length === 0);
        show(el.publicSection, publics.length > 0);
    }

    function loadList() {
        show(el.loading, true);
        show(el.error, false);
        show(el.myGrid, false);
        show(el.myEmpty, false);
        show(el.myCount, false);
        show(el.publicSection, false);

        request('GET', cfg.listUrl)
            .then(function (json) {
                show(el.loading, false);
                renderList(toList(json));
            })
            .catch(function (err) {
                console.error('템플릿 목록 불러오기 실패', err);
                show(el.loading, false);
                show(el.error, true);
            });
    }

    /* ---------- 삭제 ---------- */

    function deleteTemplate(card) {
        if (!card || card.classList.contains('is-deleting')) return;

        var templateId = card.getAttribute('data-template-id');
        var name = card.getAttribute('data-template-name') || '';
        if (!confirm('\'' + name + '\' 템플릿을 삭제할까요?')) return;

        card.classList.add('is-deleting');
        request('DELETE', cfg.deleteUrl + encodeURIComponent(templateId))
            .then(function () {
                card.classList.remove('is-deleting');
                card.classList.add('is-removed');
                // 애니메이션이 꺼져 있으면 animationend 가 오지 않으므로 시간으로 지움
                setTimeout(function () {
                    var thumb = card.querySelector('.tl-thumb');
                    if (thumb && thumbObserver) thumbObserver.unobserve(thumb);
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
        var count = el.myGrid.querySelectorAll('.tl-card').length;
        el.myCount.textContent = String(count);
        show(el.myGrid, count > 0);
        show(el.myEmpty, count === 0);
    }

    /* ---------- 시작 ---------- */

    document.addEventListener('DOMContentLoaded', function () {
        var body = document.body;
        cfg = {
            resumeId: body.getAttribute('data-resume-id') || null,
            listUrl: body.getAttribute('data-list-url'),
            deleteUrl: body.getAttribute('data-delete-url'),
            editUrl: body.getAttribute('data-edit-url'),
            newUrl: body.getAttribute('data-new-url'),
            previewUrl: body.getAttribute('data-preview-url')
        };

        el.toast = document.getElementById('listToast');
        el.loading = document.getElementById('listLoading');
        el.error = document.getElementById('listError');
        el.myGrid = document.getElementById('myTemplateGrid');
        el.myCount = document.getElementById('myCount');
        el.myEmpty = document.getElementById('myEmpty');
        el.publicSection = document.getElementById('publicSection');
        el.publicGrid = document.getElementById('publicTemplateGrid');

        initThumbObserver();

        document.addEventListener('click', function (e) {
            var deleteBtn = e.target.closest('.js-delete');
            if (deleteBtn) deleteTemplate(deleteBtn.closest('.tl-card'));
        });
        document.getElementById('btnRetry').addEventListener('click', loadList);

        loadList();
    });
})();