/* =========================================================
   resume-list.js  (이력서 목록)
   삭제 : 카드의 휴지통 → 확인 → DELETE /api/resume/{id} → 카드만 지움 (새로고침 없음)
   필요 : common.js (callApi) 를 먼저 불러야 함
   ========================================================= */
(() => {
    'use strict';

    const listEl  = document.getElementById('resumeList');
    const emptyEl = document.getElementById('resumeEmpty');
    const toastEl = document.getElementById('listToast');
    if (!listEl) return;

    let toastTimer = null;

    // ---------- 알림 ----------
    function showToast(text, isError = false) {
        clearTimeout(toastTimer);
        toastEl.textContent = text;
        toastEl.classList.toggle('is-error', isError);
        toastEl.classList.remove('d-none');
        toastTimer = setTimeout(() => toastEl.classList.add('d-none'), isError ? 4000 : 2500);
    }

    // ---------- 삭제 API ----------
    /**
     * DELETE /api/resume/{id}  (CSRF 헤더·오류 처리는 callApi 가 함)
     *  - 성공 : 'deleted'
     *  - 404  : 'gone' (이미 삭제됐거나 내 이력서가 아님 → 화면에서도 지움)
     *  - 그 밖의 실패 : ApiError (message = 서버 안내 문구 또는 기본 문구)
     */
    function deleteResume(id) {
        return callApi({
            url: `/api/resume/${encodeURIComponent(id)}`,
            method: 'DELETE',
            success: () => 'deleted',
            error: err => {
                if (err.status === 404) return 'gone';
                throw err;
            }
        });
    }

    // ---------- 카드 지우기 ----------
    function removeCard(card) {
        const done = () => {
            card.remove();
            // 마지막 카드였으면 빈 화면
            if (!listEl.querySelector('.resume-card')) {
                listEl.classList.add('d-none');
                emptyEl?.classList.remove('d-none');
            }
        };
        card.classList.remove('is-deleting');
        card.classList.add('is-removed');
        card.addEventListener('animationend', done, { once: true });
        setTimeout(() => card.isConnected && done(), 400);    // 애니메이션이 없을 때 대비
    }

    // ---------- 이벤트 ----------
    listEl.addEventListener('click', async e => {
        const btn = e.target.closest('.resume-card__delete');
        if (!btn) return;
        const card = btn.closest('.resume-card');
        const id = card.dataset.id;
        const title = card.dataset.title || '제목 없음';

        if (!confirm(`'${title}' 이력서를 삭제할까요?\n삭제하면 목록에서 사라져요.`)) return;

        card.classList.add('is-deleting');
        btn.disabled = true;
        try {
            const result = await deleteResume(id);
            removeCard(card);
            showToast(result === 'gone'
                ? `'${title}' 이력서는 이미 삭제됐어요.`
                : `'${title}' 이력서를 삭제했어요.`);
        } catch (err) {
            console.error('이력서 삭제 실패', err);
            card.classList.remove('is-deleting');
            btn.disabled = false;
            showToast(err.message, true);
        }
    });
})();