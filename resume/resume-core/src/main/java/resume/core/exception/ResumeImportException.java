package resume.core.exception;

import lombok.Getter;

/**
 * 이력서 파일 가져오기 실패 (message 는 사용자에게 그대로 보여줄 문구)
 */
@Getter
public class ResumeImportException extends RuntimeException {

    /** 400 : 요청이 잘못됨 (파일 없음, 손상된 파일) */
    public static final int BAD_REQUEST = 400;
    /** 413 : 파일이 너무 큼 */
    public static final int TOO_LARGE = 413;
    /** 415 : 지원하지 않는 형식 */
    public static final int UNSUPPORTED = 415;
    /** 422 : 형식은 맞지만 내용을 읽을 수 없음 (암호, 스캔본, 글자 깨짐) */
    public static final int UNREADABLE = 422;

    private final int status;

    public ResumeImportException(int status, String message) {
        super(message);
        this.status = status;
    }

    public ResumeImportException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

}
