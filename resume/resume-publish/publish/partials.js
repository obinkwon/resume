// 모든 페이지가 공유하는 조각 : {root} 는 사이트 최상위 경로 (publish.js 가 바꿈)
window.PUBLISH_PARTIALS = {
    publishBar: `
<a class="pub-bar" href="{root}index.html">
    <i class="icon icon-arrow-left sm" aria-hidden="true"></i>퍼블리싱 목록
    <span data-page-path></span>
</a>
`,
};
