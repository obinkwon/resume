package resume.core.parser;

import resume.core.dto.ResumeImportResultDto;
import resume.core.enums.ResumeFileType;

import java.util.List;
import java.util.Map;

/**
 * 추출한 글자 → 이력서 항목
 *  - 지금은 규칙 기반(RuleBasedResumeParser)
 *  - 나중에 AI 로 바꿀 때는 이 인터페이스를 구현한 빈을 하나 더 만들고 @Primary 로 지정
 */
public interface ResumeParser {

    ResumeImportResultDto parse(String text, ResumeFileType fileType);

    /** 디버그용 : 섹션별로 나뉜 줄 (구현이 없으면 빈 값) */
    default Map<String, List<String>> debugSections(String text) {
        return Map.of();
    }
}
