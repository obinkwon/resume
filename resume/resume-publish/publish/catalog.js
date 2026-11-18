// 퍼블리싱 산출물 목록 : 새 페이지는 해당 그룹 pages 에 한 줄 추가 (CHANGES 는 최신 건을 위로)
window.PUBLISH_CATALOG = [
    {
        eyebrow: "CHANGES",
        name: "CSS 변경 산출물",
        desc: "CSS 를 수정하거나 추가할 때마다 한 건씩 쌓는 기록입니다. 최신 건을 위에 둡니다.",
        pages: [
            { path: "changes/20261118-baseline", title: "기준 CSS 복사", desc: "resume-web CSS 를 이 폴더로 복사한 시작점입니다.", css: ["전체"], updated: "2026-11-18" },
        ],
    },
    {
        eyebrow: "FOUNDATION",
        name: "디자인 기초",
        desc: "모든 화면이 공유하는 색상과 글자 규칙입니다.",
        pages: [
            { path: "foundation/tokens", title: "색상 · 타이포그래피", desc: "common.css 변수, 모서리, 그림자, 글자 크기 · 색 클래스", css: ["common.css"], updated: "2026-11-18" },
        ],
    },
    {
        eyebrow: "COMPONENTS",
        name: "컴포넌트",
        desc: "사이트에서 실제로 쓰는 부품을 상태별로 모았습니다.",
        pages: [
            { path: "components/buttons", title: "버튼 · 아이콘", desc: "기본, 보조, 외곽선, 고스트, 위험, 크기, 아이콘 버튼, 아이콘 목록", css: ["common.css"], updated: "2026-11-18" },
            { path: "components/forms", title: "폼", desc: "입력칸, 라벨, 체크, 한줄형 · 카드형 항목, 날짜 줄", css: ["common.css", "resume-form.css"], updated: "2026-11-18" },
            { path: "components/badges", title: "뱃지", desc: "기본 · 상태 뱃지, 출처 뱃지 7종", css: ["common.css", "resume-list.css"], updated: "2026-11-18" },
            { path: "components/cards", title: "카드", desc: "이력서 카드, 템플릿 카드, 미니 썸네일 3종", css: ["common.css", "resume-list.css", "resume-template-list.css", "resume-template.css"], updated: "2026-11-18" },
            { path: "components/feedback", title: "모달 · 토스트 · 안내", desc: "모달, 토스트, 저장 안내, 빈 화면", css: ["common.css", "resume-form.css", "resume-list.css"], updated: "2026-11-18" },
        ],
    },
    {
        eyebrow: "SCREENS",
        name: "화면",
        desc: "실제 화면 구성을 더미 데이터로 옮긴 페이지입니다. JS 동작은 빼고 모양만 둡니다.",
        pages: [
            { path: "screens/login", title: "로그인", desc: "사용자 ID 입력, 오류 안내", css: ["login.css"], updated: "2026-11-18" },
            { path: "screens/resume-list", title: "이력서 목록", desc: "이력서 카드, 출처 뱃지, 수정 시간, 미리보기 비활성", css: ["resume-list.css"], updated: "2026-11-18" },
            { path: "screens/resume-form", title: "이력서 작성 · 수정", desc: "8개 섹션, 프로필 사진, 한줄형 · 카드형 항목, 하단 저장 줄", css: ["resume-form.css"], updated: "2026-11-18" },
            { path: "screens/import-modal", title: "이력서 불러오기 모달", desc: "가져올 곳 탭, 이력서 선택, 항목 · 방식 선택", css: ["resume-form.css"], updated: "2026-11-18" },
            { path: "screens/import-modal-file", title: "이력서 불러오기 모달 (파일)", desc: "파일 끌어놓기 칸, 서버 안내", css: ["resume-form.css"], updated: "2026-11-18" },
            { path: "screens/resume-template", title: "템플릿 선택", desc: "템플릿 카드, 섹션 표시 스위치, CSS 편집기, A4 미리보기", css: ["resume-template.css"], updated: "2026-11-18" },
            { path: "screens/template-list", title: "내 템플릿", desc: "내 템플릿 카드, 기본 템플릿 복사", css: ["resume-template-list.css"], updated: "2026-11-18" },
            { path: "screens/template-form", title: "템플릿 편집", desc: "치환자 목록, HTML/CSS 탭 편집기, 검사 메시지, 미리보기", css: ["resume-template-form.css"], updated: "2026-11-18" },
            { path: "screens/resume-preview", title: "이력서 미리보기 (A4)", desc: "기본 템플릿 '모던 사이드바' 로 그린 샘플 이력서", css: ["resume-preview.css"], updated: "2026-11-18" },
        ],
    },
];
