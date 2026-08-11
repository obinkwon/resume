# 데이터베이스 생성
CREATE DATABASE resume default CHARACTER SET UTF8;

CREATE USER 'resume'@'localhost' IDENTIFIED BY 'resume1234';
CREATE USER 'resume'@'%' IDENTIFIED BY 'resume1234';
GRANT ALL PRIVILEGES ON resume.* TO 'resume'@'%';
FLUSH PRIVILEGES;