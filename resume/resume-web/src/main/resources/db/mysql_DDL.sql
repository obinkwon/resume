-- ========================================
-- 사용자 (소유자 식별용)
-- ========================================
CREATE TABLE users (
    user_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_key     VARCHAR(100) NOT NULL UNIQUE COMMENT '외부에서 전달되는 사용자 식별값',
    name         VARCHAR(50) NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- ========================================
-- 이력서
-- ========================================
CREATE TABLE resumes (
    resume_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    title            VARCHAR(100) NOT NULL,
    summary          TEXT,
    template_id      BIGINT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- ========================================
-- 기본 정보
-- ========================================
CREATE TABLE resume_profile (
    profile_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id        BIGINT NOT NULL,
    full_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(100),
    phone            VARCHAR(30),
    address          VARCHAR(255),
    profile_image    VARCHAR(255),
    birth_date       DATE NULL,
    introduction     TEXT
);
-- ========================================
-- 경력
-- ========================================
CREATE TABLE resume_experience (
    experience_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id        BIGINT NOT NULL,
    company_name     VARCHAR(100) NOT NULL,
    position         VARCHAR(100) NOT NULL,
    start_date       DATE,
    end_date         DATE,
    is_current       TINYINT(1) DEFAULT 0,
    description      TEXT,
    sort_order       INT DEFAULT 0,
    CONSTRAINT fk_experience_resume FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE
);
-- ========================================
-- 프로젝트
-- ========================================
CREATE TABLE resume_project (
    project_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id        BIGINT NOT NULL,
    project_name     VARCHAR(150) NOT NULL,
    start_date       DATE,
    end_date         DATE,
    description      TEXT,
    role             VARCHAR(100),
    tech_stack       TEXT,
    github_url       VARCHAR(255),
    demo_url         VARCHAR(255),
    sort_order       INT DEFAULT 0,
    CONSTRAINT fk_project_resume FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE
);
-- ========================================
-- 학력
-- ========================================
CREATE TABLE resume_education (
    education_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id        BIGINT NOT NULL,
    school_name      VARCHAR(150) NOT NULL,
    major            VARCHAR(150),
    degree           VARCHAR(100),
    start_date       DATE,
    end_date         DATE,
    description      TEXT,
    sort_order       INT DEFAULT 0,
    CONSTRAINT fk_education_resume FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE
);
-- ========================================
-- 기술 스택
-- ========================================
CREATE TABLE resume_skill (
    skill_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id         BIGINT NOT NULL,
    skill_name        VARCHAR(100) NOT NULL,
    category          VARCHAR(50),
    proficiency       TINYINT,
    sort_order        INT DEFAULT 0,
    CONSTRAINT fk_skill_resume FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE
);
-- ========================================
-- 자격증
-- ========================================
CREATE TABLE resume_certificate (
    certificate_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id         BIGINT NOT NULL,
    certificate_name  VARCHAR(150) NOT NULL,
    issuer            VARCHAR(150),
    issued_date       DATE,
    description       TEXT,
    sort_order        INT DEFAULT 0,
    CONSTRAINT fk_certificate_resume FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE
);
-- ========================================
-- 템플릿
-- ========================================
CREATE TABLE templates (
    template_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name     VARCHAR(100) NOT NULL,
    description       TEXT,
    thumbnail_url     VARCHAR(255),
    html_content      LONGTEXT NOT NULL,
    css_content       LONGTEXT,
    is_public         TINYINT(1) DEFAULT 1,
    created_by        BIGINT NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_template_user FOREIGN KEY (created_by) REFERENCES users(user_id)
);
-- ========================================
-- 템플릿 섹션 배치
-- ========================================
CREATE TABLE template_section (
    section_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id       BIGINT NOT NULL,
    section_type      VARCHAR(50) NOT NULL,
    display_name      VARCHAR(100),
    position_x        INT DEFAULT 0,
    position_y        INT DEFAULT 0,
    width             INT DEFAULT 12,
    height            INT DEFAULT 1,
    sort_order        INT DEFAULT 0,
    visible           TINYINT(1) DEFAULT 1,
    style_json        JSON,
    CONSTRAINT fk_section_template FOREIGN KEY (template_id) REFERENCES templates(template_id) ON DELETE CASCADE
);
-- ========================================
-- PDF 생성 이력
-- ========================================
CREATE TABLE resume_pdf (
    pdf_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id         BIGINT NOT NULL,
    template_id       BIGINT NOT NULL,
    file_path         VARCHAR(255) NOT NULL,
    file_name         VARCHAR(255) NOT NULL,
    generated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pdf_resume FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE,
    CONSTRAINT fk_pdf_template FOREIGN KEY (template_id) REFERENCES templates(template_id)
);
-- ========================================
-- 인덱스
-- ========================================
CREATE INDEX idx_resume_user ON resumes(user_id);
CREATE INDEX idx_exp_resume ON resume_experience(resume_id);
CREATE INDEX idx_project_resume ON resume_project(resume_id);
CREATE INDEX idx_edu_resume ON resume_education(resume_id);
CREATE INDEX idx_skill_resume ON resume_skill(resume_id);
CREATE INDEX idx_cert_resume ON resume_certificate(resume_id);
CREATE INDEX idx_section_template ON template_section(template_id);
CREATE INDEX idx_pdf_resume ON resume_pdf(resume_id);