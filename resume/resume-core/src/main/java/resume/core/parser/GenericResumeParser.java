package resume.core.parser;

import org.springframework.stereotype.Component;
import resume.core.enums.ResumeSource;

/**
 * 일반 파서 — 어느 사이트인지 모를 때 (직접 만든 Word·한글 양식, 링크드인, 그 밖의 사이트)
 *  - 공통 규칙(RuleBasedResumeParser)만 사용
 */
@Component
public class GenericResumeParser extends RuleBasedResumeParser {

    @Override
    public ResumeSource source() {
        return ResumeSource.DIRECT;
    }
}
