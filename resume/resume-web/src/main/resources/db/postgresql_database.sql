-- 사용자 생성
CREATE USER resume WITH PASSWORD 'resume1234';

-- 스키마 생성
CREATE SCHEMA resume;

-- resume 스키마 사용 권한
GRANT USAGE ON SCHEMA resume TO resume;

-- 현재 존재하는 모든 테이블에 CRUD 권한
GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA resume
TO resume;

-- 현재 존재하는 Sequence 권한
GRANT USAGE, SELECT, UPDATE
ON ALL SEQUENCES IN SCHEMA resume
TO resume;

-- 앞으로 생성되는 테이블에도 자동으로 CRUD 권한
ALTER DEFAULT PRIVILEGES IN SCHEMA resume
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLES TO resume;

-- 앞으로 생성되는 Sequence에도 자동으로 권한
ALTER DEFAULT PRIVILEGES IN SCHEMA resume
GRANT USAGE, SELECT, UPDATE
ON SEQUENCES TO resume;