package resume.core.importer.extract;

import resume.core.importer.ResumeFileType;

/**
 * 파일 형식별 글자 추출
 *  - 표는 한 줄에 칸을 " | " 로 이어서 냄 (파서가 칸을 나눌 때 사용)
 */
public interface ResumeTextExtractor {

    ResumeFileType supports();

    /** 실패하면 ResumeImportException (사용자에게 보여줄 문구 포함) */
    String extract(byte[] bytes);
}
