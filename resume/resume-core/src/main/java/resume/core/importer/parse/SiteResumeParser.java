package resume.core.importer.parse;

/**
 * 구직 사이트 전용 파서
 *  - matchScore : 글자에 그 사이트 PDF 에만 있는 문구가 얼마나 있는지 (ResumeParserRouter 가 가장 높은 곳을 고름)
 *  - 새 사이트 추가 = 이 인터페이스 + RuleBasedResumeParser 를 상속한 @Component 하나
 */
public interface SiteResumeParser extends ResumeParser {

    ResumeSource source();

    /** 0 이면 이 사이트가 아님. ResumeParserRouter.MIN_SCORE 이상이어야 이 파서를 씀 */
    int matchScore(String text);
}
