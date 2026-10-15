/* 템플릿 빌더 js */

document.querySelectorAll('input, select').forEach(el => {
    el.addEventListener('change', () => {
        // 추후 Ajax로 설정 저장 + iframe 새로고침
    });
});