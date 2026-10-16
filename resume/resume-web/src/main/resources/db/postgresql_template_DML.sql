-- ========================================
-- 시스템 기본 템플릿 (created_by = NULL)
--  html_content : 칸 구성 + 섹션 치환자
--                 {{profile}} {{intro}} {{experience}} {{project}}
--                 {{education}} {{certificate}} {{skill}} {{link}}
--                 (치환자를 빼면 그 섹션은 출력되지 않음)
--  css_content  : 템플릿 디자인 (공통 뼈대는 template-preview.css)
--  $$ ... $$ (dollar-quoting) 으로 따옴표 이스케이프 없이 작성
-- ========================================
INSERT INTO templates (template_name, description, html_content, css_content, is_public)
VALUES
-- 1) 심플 : 한 컬럼
('심플', '여백이 넉넉한 한 컬럼 기본형',
$$<div class="r-top">{{profile}}</div>
<main class="r-main">
  {{intro}}
  {{experience}}
  {{project}}
  {{education}}
  {{certificate}}
  {{skill}}
  {{link}}
</main>$$,
$$:root {
  --primary: #2D3748;
  --font: 'Pretendard';
}

.r-header {
  padding-bottom: 20px;
  border-bottom: 2px solid var(--primary);
}

.r-section__title {
  padding-bottom: 6px;
  color: var(--primary);
  border-bottom: 1px solid var(--line);
}
$$, TRUE),

-- 2) 모던 사이드바 : 색 헤더 + 왼쪽 사이드바
('모던 사이드바', '색 헤더와 왼쪽 사이드바가 있는 디자인',
$$<div class="r-top">{{profile}}</div>
<div class="r-columns">
  <aside class="r-aside">
    {{skill}}
    {{education}}
    {{certificate}}
    {{link}}
  </aside>
  <main class="r-main">
    {{intro}}
    {{experience}}
    {{project}}
  </main>
</div>$$,
$$:root {
  --primary: #2563EB;
  --font: 'Pretendard';
}

.resume { padding-top: 0; }

.r-header {
  margin: 0 -52px 32px;
  padding: 40px 52px 28px;
  background: var(--primary);
  color: #fff;
}
.r-contact { color: rgba(255, 255, 255, .85); }

.r-columns {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 32px;
}
.r-aside {
  align-self: start;
  padding: 18px;
  border-radius: 8px;
  background: var(--tint);
}
.r-aside .r-item__head { flex-direction: column; gap: 2px; }
.r-aside .r-tags li { background: #fff; }

.r-section__title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.r-section__title::before {
  content: "";
  width: 4px;
  height: 1em;
  border-radius: 2px;
  background: var(--primary);
}
.r-main .r-item {
  padding-left: 12px;
  border-left: 2px solid var(--line);
}
$$, TRUE),

-- 3) 모던 분할 : 색 헤더 + 좌우 2단
('모던 분할', '좌우 2단으로 정보를 한눈에',
$$<div class="r-top">{{profile}}</div>
<div class="r-columns">
  <main class="r-main">
    {{intro}}
    {{experience}}
    {{project}}
  </main>
  <aside class="r-aside">
    {{skill}}
    {{education}}
    {{certificate}}
    {{link}}
  </aside>
</div>$$,
$$:root {
  --primary: #0F766E;
  --font: 'Noto Sans KR';
}

.resume { padding-top: 0; }

.r-header {
  margin: 0 -52px 32px;
  padding: 40px 52px 28px;
  background: var(--primary);
  color: #fff;
}
.r-contact { color: rgba(255, 255, 255, .85); }

.r-columns {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 28px;
}
.r-aside {
  padding-left: 28px;
  border-left: 1px solid var(--line);
}
.r-aside .r-item__head { flex-direction: column; gap: 2px; }

.r-section__title {
  padding-bottom: 6px;
  color: var(--primary);
  border-bottom: 2px solid var(--primary);
}
$$, TRUE),

-- 4) ATS 제출용 : 흑백, 장식 없음, 한 컬럼
('ATS 제출용', '서류 판독기 호환 흑백 한 컬럼',
$$<div class="r-top">{{profile}}</div>
<main class="r-main">
  {{intro}}
  {{experience}}
  {{project}}
  {{education}}
  {{certificate}}
  {{skill}}
  {{link}}
</main>$$,
$$:root {
  --primary: #111111;
  --font: 'Noto Sans KR';
  --tint: transparent;
}

.r-header {
  display: block;
  padding-bottom: 16px;
  border-bottom: 1px solid #111;
}
.r-photo { display: none; }
.r-contact { margin-top: 6px; text-align: left; }
.r-contact li { display: inline; }
.r-contact li + li::before { content: " | "; color: #999; }

.r-section__title {
  padding-bottom: 4px;
  border-bottom: 1px solid #111;
  text-transform: uppercase;
  letter-spacing: .04em;
}

.r-tags li {
  padding: 0;
  background: none;
  color: var(--ink);
  font-weight: 400;
}
.r-tags li + li::before { content: "· "; }
$$, TRUE);


-- ========================================
-- 구직사이트 스타일 템플릿 (created_by = NULL)
--  기존 시스템 기본 템플릿(1~4)에 이어서 추가
--  치환자 : {{profile}} {{intro}} {{experience}} {{project}}
--           {{education}} {{certificate}} {{skill}} {{link}}
--  공통 뼈대(template-preview.css)의 클래스/변수만 사용
--   .resume .r-top .r-header .r-photo .r-contact .r-columns
--   .r-main .r-aside .r-section .r-section__title .r-item .r-item__head .r-tags
--   --primary --font --tint --line --ink
-- ========================================
INSERT INTO templates (template_name, description, html_content, css_content, is_public)
VALUES
-- 5) 사람인 스타일 : 국내 표준형, 회색 섹션 바 + 학력 우선 순서
('사람인 스타일', '사진 헤더와 회색 섹션 바의 국내 표준 이력서',
$$<div class="r-top">{{profile}}</div>
<main class="r-main">
  {{intro}}
  {{education}}
  {{experience}}
  {{project}}
  {{certificate}}
  {{skill}}
  {{link}}
</main>$$,
$$:root {
  --primary: #1B55F5;
  --font: 'Pretendard';
  --tint: #F4F6FA;
}

.r-header {
  padding: 20px 24px;
  border: 1px solid var(--line);
  border-top: 3px solid var(--primary);
  border-radius: 4px;
  margin-bottom: 28px;
}

.r-section__title {
  padding: 8px 12px;
  background: var(--tint);
  border-left: 4px solid var(--primary);
  font-size: 1rem;
}

.r-item {
  padding: 12px 4px;
  border-bottom: 1px dashed var(--line);
}
.r-item:last-child { border-bottom: none; }

.r-tags li {
  border: 1px solid var(--primary);
  background: #fff;
  color: var(--primary);
}
$$, TRUE),

-- 6) 잡코리아 스타일 : 표 형식, 섹션마다 테두리 박스
('잡코리아 스타일', '섹션을 표 칸처럼 나눈 정형 이력서',
$$<div class="r-top">{{profile}}</div>
<main class="r-main">
  {{intro}}
  {{experience}}
  {{education}}
  {{certificate}}
  {{skill}}
  {{project}}
  {{link}}
</main>$$,
$$:root {
  --primary: #1A3A8F;
  --font: 'Noto Sans KR';
  --tint: #EEF2FB;
}

.r-header {
  padding: 18px 20px;
  border: 1px solid #C9D2E3;
  background: var(--tint);
  margin-bottom: 24px;
}

.r-section {
  border: 1px solid #C9D2E3;
  margin-bottom: 20px;
}
.r-section__title {
  margin: 0;
  padding: 8px 14px;
  background: var(--primary);
  color: #fff;
  font-size: .95rem;
}

.r-item {
  padding: 12px 14px;
  border-top: 1px solid #E3E8F1;
}
.r-section__title + .r-item { border-top: none; }

.r-tags { padding: 12px 14px; }
.r-tags li {
  border-radius: 2px;
  background: var(--tint);
  color: var(--primary);
}
$$, TRUE),

-- 7) 원티드 스타일 : 사진 없음, 큰 이름, 섹션 제목 왼쪽 열
('원티드 스타일', '사진 없이 여백과 타이포로 정리한 미니멀 이력서',
$$<div class="r-top">{{profile}}</div>
<main class="r-main">
  {{intro}}
  {{experience}}
  {{project}}
  {{skill}}
  {{education}}
  {{certificate}}
  {{link}}
</main>$$,
$$:root {
  --primary: #3366FF;
  --font: 'Pretendard';
  --tint: #F7F7F8;
}

.r-photo { display: none; }

.r-header {
  display: block;
  padding-bottom: 32px;
  margin-bottom: 8px;
}
.r-contact { margin-top: 10px; text-align: left; }
.r-contact li { display: inline; color: #767676; }
.r-contact li + li::before { content: " · "; }

.r-section {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 24px;
  padding: 28px 0;
  border-top: 1px solid var(--line);
}
.r-section__title {
  margin: 0;
  font-size: .95rem;
  color: #333;
}

.r-item + .r-item { margin-top: 20px; }

.r-tags li {
  border-radius: 999px;
  background: var(--tint);
  color: #333;
}
$$, TRUE),

-- 8) 리멤버 스타일 : 경력 중심, 타임라인
('리멤버 스타일', '경력 흐름을 타임라인으로 보여주는 경력직용',
$$<div class="r-top">{{profile}}</div>
<main class="r-main">
  {{intro}}
  {{experience}}
  {{project}}
  {{skill}}
  {{certificate}}
  {{education}}
  {{link}}
</main>$$,
$$:root {
  --primary: #222222;
  --font: 'Pretendard';
  --tint: #F3F3F3;
}

.r-header {
  padding-bottom: 24px;
  border-bottom: 3px solid var(--primary);
}

.r-section__title {
  font-size: 1.05rem;
  letter-spacing: -.01em;
}

.r-main .r-item {
  position: relative;
  padding: 0 0 20px 22px;
  border-left: 2px solid var(--line);
}
.r-main .r-item:last-child { padding-bottom: 0; }
.r-main .r-item::before {
  content: "";
  position: absolute;
  left: -7px;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--primary);
}
.r-main .r-item:first-of-type::before { background: var(--primary); }

.r-tags li {
  background: var(--tint);
  color: var(--ink);
}
$$, TRUE),

-- 9) 개발자 스타일 (프로그래머스·점핏) : 기술스택 사이드 + 프로젝트 우선
('개발자 스타일', '기술 스택과 프로젝트를 앞세운 개발자 이력서',
$$<div class="r-top">{{profile}}</div>
<div class="r-columns">
  <main class="r-main">
    {{intro}}
    {{project}}
    {{experience}}
    {{education}}
  </main>
  <aside class="r-aside">
    {{skill}}
    {{link}}
    {{certificate}}
  </aside>
</div>$$,
$$:root {
  --primary: #12B886;
  --font: 'Pretendard';
  --tint: #EBFBF5;
}

.r-header {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--line);
}

.r-columns {
  display: grid;
  grid-template-columns: 1fr 210px;
  gap: 32px;
}
.r-aside {
  align-self: start;
  padding: 16px;
  border-radius: 8px;
  background: #1E2430;
  color: #E6E8EC;
}
.r-aside .r-section__title { color: var(--primary); }
.r-aside .r-item__head { flex-direction: column; gap: 2px; }

.r-section__title::before {
  content: "# ";
  color: var(--primary);
  font-family: 'D2Coding', monospace;
}

.r-tags li {
  border: 1px solid var(--primary);
  background: transparent;
  color: var(--primary);
  font-family: 'D2Coding', monospace;
  font-size: .8rem;
}

.r-main .r-item {
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
}
.r-main .r-item + .r-item { margin-top: 12px; }
$$, TRUE),

-- 10) 링크드인 스타일 : 배너 헤더 + 카드형 섹션
('링크드인 스타일', '배너 헤더와 카드형 섹션의 프로필형 이력서',
$$<div class="r-top">{{profile}}</div>
<main class="r-main">
  {{intro}}
  {{experience}}
  {{project}}
  {{education}}
  {{certificate}}
  {{skill}}
  {{link}}
</main>$$,
$$:root {
  --primary: #0A66C2;
  --font: 'Pretendard';
  --tint: #F3F2EF;
}

.resume {
  padding-top: 0;
  background: var(--tint);
}

.r-header {
  margin: 0 -52px 20px;
  padding: 72px 52px 24px;
  background:
    linear-gradient(var(--primary), var(--primary)) top / 100% 56px no-repeat,
    #fff;
  border-bottom: 1px solid var(--line);
}
.r-photo {
  border: 4px solid #fff;
  border-radius: 50%;
}

.r-section {
  padding: 20px 22px;
  margin-bottom: 14px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, .08);
}
.r-section__title { font-size: 1.1rem; }

.r-item + .r-item {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.r-tags li {
  border: 1px solid #B0B0B0;
  border-radius: 999px;
  background: #fff;
  color: var(--ink);
}
$$, TRUE);