-- ========================================
-- 사용자 (소유자 식별용)
-- ========================================
CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '사용자 PK',
  `user_key` varchar(100) NOT NULL COMMENT '외부에서 전달되는 사용자 식별값',
  `name` varchar(50) DEFAULT NULL COMMENT '사용자 이름',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `user_key` (`user_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='사용자 정보';

-- ========================================
-- 이력서
-- ========================================
CREATE TABLE `resumes` (
  `resume_id` bigint NOT NULL AUTO_INCREMENT COMMENT '이력서 PK',
  `user_id` bigint NOT NULL COMMENT '사용자 ID',
  `title` varchar(100) NOT NULL COMMENT '이력서 제목',
  `summary` text COMMENT '이력서 요약',
  `template_id` bigint DEFAULT NULL COMMENT '적용 템플릿 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`resume_id`),
  KEY `idx_resume_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='이력서 정보';

-- ========================================
-- 기본 정보
-- ========================================
CREATE TABLE `resume_profile` (
  `profile_id` bigint NOT NULL AUTO_INCREMENT COMMENT '기본 정보 PK',
  `resume_id` bigint NOT NULL COMMENT '이력서 ID',
  `full_name` varchar(100) NOT NULL COMMENT '이름',
  `email` varchar(100) DEFAULT NULL COMMENT '이메일',
  `phone` varchar(30) DEFAULT NULL COMMENT '전화번호',
  `address` varchar(255) DEFAULT NULL COMMENT '주소',
  `profile_image` varchar(255) DEFAULT NULL COMMENT '프로필 이미지 경로 또는 URL',
  `birth_date` date DEFAULT NULL COMMENT '생년월일',
  `introduction` text COMMENT '자기소개',
  PRIMARY KEY (`profile_id`),
  KEY `idx_profile_resume` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='이력서 기본 정보';

-- ========================================
-- 외부 링크
-- ========================================
CREATE TABLE `resume_link` (
  `link_id` bigint NOT NULL AUTO_INCREMENT COMMENT '링크 PK',
  `resume_id` bigint NOT NULL COMMENT '이력서 ID',
  `link_type` varchar(50) NOT NULL COMMENT '링크 유형(github, linkedin, blog, portfolio, website 등)',
  `display_name` varchar(100) DEFAULT NULL COMMENT '화면 표시 이름',
  `url` varchar(255) NOT NULL COMMENT '링크 URL',
  `sort_order` int DEFAULT '0' COMMENT '출력 순서',
  PRIMARY KEY (`link_id`),
  KEY `idx_link_resume` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='이력서 외부 링크';

-- ========================================
-- 경력
-- ========================================
CREATE TABLE `resume_experience` (
  `experience_id` bigint NOT NULL AUTO_INCREMENT COMMENT '경력 PK',
  `resume_id` bigint NOT NULL COMMENT '이력서 ID',
  `company_name` varchar(100) NOT NULL COMMENT '회사명',
  `position` varchar(100) NOT NULL COMMENT '직책',
  `start_date` date DEFAULT NULL COMMENT '시작일',
  `end_date` date DEFAULT NULL COMMENT '종료일',
  `is_current` tinyint(1) DEFAULT '0' COMMENT '재직 여부',
  `description` text COMMENT '경력 설명',
  `sort_order` int DEFAULT '0' COMMENT '출력 순서',
  PRIMARY KEY (`experience_id`),
  KEY `idx_exp_resume` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='이력서 경력 정보';

-- ========================================
-- 프로젝트
-- ========================================
CREATE TABLE `resume_project` (
  `project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '프로젝트 PK',
  `resume_id` bigint NOT NULL COMMENT '이력서 ID',
  `project_name` varchar(150) NOT NULL COMMENT '프로젝트명',
  `start_date` date DEFAULT NULL COMMENT '시작일',
  `end_date` date DEFAULT NULL COMMENT '종료일',
  `description` text COMMENT '프로젝트 설명',
  `role` varchar(100) DEFAULT NULL COMMENT '담당 역할',
  `tech_stack` text COMMENT '사용 기술 스택',
  `github_url` varchar(255) DEFAULT NULL COMMENT 'GitHub URL',
  `demo_url` varchar(255) DEFAULT NULL COMMENT '데모 URL',
  `sort_order` int DEFAULT '0' COMMENT '출력 순서',
  PRIMARY KEY (`project_id`),
  KEY `idx_project_resume` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='이력서 프로젝트 정보';

-- ========================================
-- 학력
-- ========================================
CREATE TABLE `resume_education` (
  `education_id` bigint NOT NULL AUTO_INCREMENT COMMENT '학력 PK',
  `resume_id` bigint NOT NULL COMMENT '이력서 ID',
  `school_name` varchar(150) NOT NULL COMMENT '학교명',
  `major` varchar(150) DEFAULT NULL COMMENT '전공',
  `degree` varchar(100) DEFAULT NULL COMMENT '학위',
  `start_date` date DEFAULT NULL COMMENT '입학일',
  `end_date` date DEFAULT NULL COMMENT '졸업일',
  `description` text COMMENT '학력 설명',
  `sort_order` int DEFAULT '0' COMMENT '출력 순서',
  PRIMARY KEY (`education_id`),
  KEY `idx_edu_resume` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='이력서 학력 정보';

-- ========================================
-- 기술 스택
-- ========================================
CREATE TABLE `resume_skill` (
  `skill_id` bigint NOT NULL AUTO_INCREMENT COMMENT '기술 스택 PK',
  `resume_id` bigint NOT NULL COMMENT '이력서 ID',
  `skill_name` varchar(100) NOT NULL COMMENT '기술명',
  `category` varchar(50) DEFAULT NULL COMMENT '기술 분류',
  `proficiency` tinyint DEFAULT NULL COMMENT '숙련도',
  `sort_order` int DEFAULT '0' COMMENT '출력 순서',
  PRIMARY KEY (`skill_id`),
  KEY `idx_skill_resume` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='이력서 기술 스택';

-- ========================================
-- 자격증
-- ========================================
CREATE TABLE `resume_certificate` (
  `certificate_id` bigint NOT NULL AUTO_INCREMENT COMMENT '자격증 PK',
  `resume_id` bigint NOT NULL COMMENT '이력서 ID',
  `certificate_name` varchar(150) NOT NULL COMMENT '자격증명',
  `issuer` varchar(150) DEFAULT NULL COMMENT '발급 기관',
  `issued_date` date DEFAULT NULL COMMENT '취득일',
  `description` text COMMENT '자격증 설명',
  `sort_order` int DEFAULT '0' COMMENT '출력 순서',
  PRIMARY KEY (`certificate_id`),
  KEY `idx_cert_resume` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='이력서 자격증 정보';

-- ========================================
-- 템플릿
-- ========================================
CREATE TABLE `templates` (
  `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '템플릿 PK',
  `template_name` varchar(100) NOT NULL COMMENT '템플릿 이름',
  `description` text COMMENT '템플릿 설명',
  `thumbnail_url` varchar(255) DEFAULT NULL COMMENT '썸네일 이미지 URL',
  `html_content` longtext NOT NULL COMMENT 'HTML 템플릿 내용',
  `css_content` longtext COMMENT 'CSS 스타일 내용',
  `is_public` tinyint(1) DEFAULT '1' COMMENT '공개 여부',
  `created_by` bigint DEFAULT NULL COMMENT '생성 사용자 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`template_id`),
  KEY `idx_template_creator` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='이력서 템플릿';

-- ========================================
-- 템플릿 섹션 배치
-- ========================================
CREATE TABLE `template_section` (
  `section_id` bigint NOT NULL AUTO_INCREMENT COMMENT '섹션 PK',
  `template_id` bigint NOT NULL COMMENT '템플릿 ID',
  `section_type` varchar(50) NOT NULL COMMENT '섹션 유형(profile, project, experience 등)',
  `display_name` varchar(100) DEFAULT NULL COMMENT '화면 표시 이름',
  `position_x` int DEFAULT '0' COMMENT 'X 좌표',
  `position_y` int DEFAULT '0' COMMENT 'Y 좌표',
  `width` int DEFAULT '12' COMMENT '너비',
  `height` int DEFAULT '1' COMMENT '높이',
  `sort_order` int DEFAULT '0' COMMENT '출력 순서',
  `visible` tinyint(1) DEFAULT '1' COMMENT '노출 여부',
  `style_json` json DEFAULT NULL COMMENT '섹션 스타일 JSON',
  PRIMARY KEY (`section_id`),
  KEY `idx_section_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='템플릿 섹션 배치 정보';

-- ========================================
-- PDF 생성 이력
-- ========================================
CREATE TABLE `resume_pdf` (
  `pdf_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PDF PK',
  `resume_id` bigint NOT NULL COMMENT '이력서 ID',
  `template_id` bigint NOT NULL COMMENT '템플릿 ID',
  `file_path` varchar(255) NOT NULL COMMENT '파일 저장 경로',
  `file_name` varchar(255) NOT NULL COMMENT '파일명',
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'PDF 생성일시',
  PRIMARY KEY (`pdf_id`),
  KEY `idx_pdf_resume` (`resume_id`),
  KEY `idx_pdf_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='PDF 생성 이력';

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