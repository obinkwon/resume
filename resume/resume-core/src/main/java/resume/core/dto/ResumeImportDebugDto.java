package resume.core.dto;

import java.util.List;
import java.util.Map;

public record ResumeImportDebugDto(
        String fileType,
        int textLength,
        String text,                          // 파일에서 꺼낸 글자 (정리 후)
        Map<String, List<String>> sections,   // 섹션별로 나뉜 줄
        ResumeImportResultDto result          // 최종 결과 (실제 API 와 같음)
) {
}
