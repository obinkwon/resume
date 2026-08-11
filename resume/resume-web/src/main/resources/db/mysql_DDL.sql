-- ========================================
-- 사용자 (소유자 식별용)
-- ========================================
CREATE TABLE users (
    user_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 PK',
    user_key     VARCHAR(100) NOT NULL UNIQUE COMMENT '외부에서 전달되는 사용자 식별값',
    name         VARCHAR(50) COMMENT '사용자 이름',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
) COMMENT='사용자 정보';

-- ========================================
-- 이력서
-- ========================================
CREATE TABLE resumes (
    resume_id        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '이력서 PK',
    user_id          BIGINT NOT NULL COMMENT '사용자 ID',
    title            VARCHAR(100) NOT NULL COMMENT '이력서 제목',
    summary          TEXT COMMENT '이력서 요약',
    template_id      BIGINT COMMENT '적용 템플릿 ID',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
) COMMENT='이력서 정보';

-- ========================================
-- 기본 정보
-- ========================================
CREATE TABLE resume_profile (
    profile_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '기본 정보 PK',
    resume_id        BIGINT NOT NULL COMMENT '이력서 ID',
    full_name        VARCHAR(100) NOT NULL COMMENT '이름',
    email            VARCHAR(100) COMMENT '이메일',
    phone            VARCHAR(30) COMMENT '전화번호',
    address          VARCHAR(255) COMMENT '주소',
    profile_image    VARCHAR(255) COMMENT '프로필 이미지 경로 또는 URL',
    birth_date       DATE COMMENT '생년월일',
    introduction     TEXT COMMENT '자기소개'
) COMMENT='이력서 기본 정보';

-- ========================================
-- 외부 링크
-- ========================================
CREATE TABLE resume_link (
    link_id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '링크 PK',
    resume_id        BIGINT NOT NULL COMMENT '이력서 ID',
    link_type        VARCHAR(50) NOT NULL COMMENT '링크 유형(github, linkedin, blog, portfolio, website 등)',
    display_name     VARCHAR(100) COMMENT '화면 표시 이름',
    url              VARCHAR(255) NOT NULL COMMENT '링크 URL',
    sort_order       INT DEFAULT 0 COMMENT '출력 순서'
) COMMENT='이력서 외부 링크';

-- ========================================
-- 경력
-- ========================================
CREATE TABLE resume_experience (
    experience_id    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '경력 PK',
    resume_id        BIGINT NOT NULL COMMENT '이력서 ID',
    company_name     VARCHAR(100) NOT NULL COMMENT '회사명',
    position         VARCHAR(100) NOT NULL COMMENT '직책',
    start_date       DATE COMMENT '시작일',
    end_date         DATE COMMENT '종료일',
    is_current       TINYINT(1) DEFAULT 0 COMMENT '재직 여부',
    description      TEXT COMMENT '경력 설명',
    sort_order       INT DEFAULT 0 COMMENT '출력 순서'
) COMMENT='이력서 경력 정보';

-- ========================================
-- 프로젝트
-- ========================================
CREATE TABLE resume_project (
    project_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '프로젝트 PK',
    resume_id        BIGINT NOT NULL COMMENT '이력서 ID',
    project_name     VARCHAR(150) NOT NULL COMMENT '프로젝트명',
    start_date       DATE COMMENT '시작일',
    end_date         DATE COMMENT '종료일',
    description      TEXT COMMENT '프로젝트 설명',
    role             VARCHAR(100) COMMENT '담당 역할',
    tech_stack       TEXT COMMENT '사용 기술 스택',
    github_url       VARCHAR(255) COMMENT 'GitHub URL',
    demo_url         VARCHAR(255) COMMENT '데모 URL',
    sort_order       INT DEFAULT 0 COMMENT '출력 순서'
) COMMENT='이력서 프로젝트 정보';

-- ========================================
-- 학력
-- ========================================
CREATE TABLE resume_education (
    education_id     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '학력 PK',
    resume_id        BIGINT NOT NULL COMMENT '이력서 ID',
    school_name      VARCHAR(150) NOT NULL COMMENT '학교명',
    major            VARCHAR(150) COMMENT '전공',
    degree           VARCHAR(100) COMMENT '학위',
    start_date       DATE COMMENT '입학일',
    end_date         DATE COMMENT '졸업일',
    description      TEXT COMMENT '학력 설명',
    sort_order       INT DEFAULT 0 COMMENT '출력 순서'
) COMMENT='이력서 학력 정보';

-- ========================================
-- 기술 스택
-- ========================================
CREATE TABLE resume_skill (
    skill_id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '기술 스택 PK',
    resume_id        BIGINT NOT NULL COMMENT '이력서 ID',
    skill_name       VARCHAR(100) NOT NULL COMMENT '기술명',
    category         VARCHAR(50) COMMENT '기술 분류',
    proficiency      TINYINT COMMENT '숙련도',
    sort_order       INT DEFAULT 0 COMMENT '출력 순서'
) COMMENT='이력서 기술 스택';

-- ========================================
-- 자격증
-- ========================================
CREATE TABLE resume_certificate (
    certificate_id   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '자격증 PK',
    resume_id        BIGINT NOT NULL COMMENT '이력서 ID',
    certificate_name VARCHAR(150) NOT NULL COMMENT '자격증명',
    issuer           VARCHAR(150) COMMENT '발급 기관',
    issued_date      DATE COMMENT '취득일',
    description      TEXT COMMENT '자격증 설명',
    sort_order       INT DEFAULT 0 COMMENT '출력 순서'
) COMMENT='이력서 자격증 정보';

-- ========================================
-- 템플릿
-- ========================================
CREATE TABLE templates (
    template_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '템플릿 PK',
    template_name    VARCHAR(100) NOT NULL COMMENT '템플릿 이름',
    description      TEXT COMMENT '템플릿 설명',
    thumbnail_url    VARCHAR(255) COMMENT '썸네일 이미지 URL',
    html_content     LONGTEXT NOT NULL COMMENT 'HTML 템플릿 내용',
    css_content      LONGTEXT COMMENT 'CSS 스타일 내용',
    is_public        TINYINT(1) DEFAULT 1 COMMENT '공개 여부',
    created_by       BIGINT COMMENT '생성 사용자 ID',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
) COMMENT='이력서 템플릿';

-- ========================================
-- 템플릿 섹션 배치
-- ========================================
CREATE TABLE template_section (
    section_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '섹션 PK',
    template_id      BIGINT NOT NULL COMMENT '템플릿 ID',
    section_type     VARCHAR(50) NOT NULL COMMENT '섹션 유형(profile, project, experience 등)',
    display_name     VARCHAR(100) COMMENT '화면 표시 이름',
    position_x       INT DEFAULT 0 COMMENT 'X 좌표',
    position_y       INT DEFAULT 0 COMMENT 'Y 좌표',
    width            INT DEFAULT 12 COMMENT '너비',
    height           INT DEFAULT 1 COMMENT '높이',
    sort_order       INT DEFAULT 0 COMMENT '출력 순서',
    visible          TINYINT(1) DEFAULT 1 COMMENT '노출 여부',
    style_json       JSON COMMENT '섹션 스타일 JSON'
) COMMENT='템플릿 섹션 배치 정보';

-- ========================================
-- PDF 생성 이력
-- ========================================
CREATE TABLE resume_pdf (
    pdf_id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'PDF PK',
    resume_id        BIGINT NOT NULL COMMENT '이력서 ID',
    template_id      BIGINT NOT NULL COMMENT '템플릿 ID',
    file_path        VARCHAR(255) NOT NULL COMMENT '파일 저장 경로',
    file_name        VARCHAR(255) NOT NULL COMMENT '파일명',
    generated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'PDF 생성일시'
) COMMENT='PDF 생성 이력';

-- ========================================
-- 인덱스
-- ========================================
CREATE INDEX idx_resume_user ON resumes(user_id);
CREATE INDEX idx_profile_resume ON resume_profile(resume_id);
CREATE INDEX idx_link_resume ON resume_link(resume_id);
CREATE INDEX idx_exp_resume ON resume_experience(resume_id);
CREATE INDEX idx_project_resume ON resume_project(resume_id);
CREATE INDEX idx_edu_resume ON resume_education(resume_id);
CREATE INDEX idx_skill_resume ON resume_skill(resume_id);
CREATE INDEX idx_cert_resume ON resume_certificate(resume_id);
CREATE INDEX idx_template_creator ON templates(created_by);
CREATE INDEX idx_section_template ON template_section(template_id);
CREATE INDEX idx_pdf_resume ON resume_pdf(resume_id);
CREATE INDEX idx_pdf_template ON resume_pdf(template_id);