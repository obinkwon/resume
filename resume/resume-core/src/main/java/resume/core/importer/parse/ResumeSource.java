package resume.core.importer.parse;

/**
 * 이력서 파일을 만든 곳 (사이트 판별 결과)
 *  - code  : 화면·로그용 값 (결과 JSON 의 source)
 *  - label : 화면 표시 이름 (결과 JSON 의 sourceName)
 *  - 사람인·원티드는 실제 PDF 로 규칙을 확인한 뒤 추가
 */
public enum ResumeSource {
    GENERIC("generic", "일반"),
    JOBKOREA("jobkorea", "잡코리아");

    private final String code;
    private final String label;

    ResumeSource(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
