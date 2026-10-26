package resume.core.record;

import java.util.List;
import java.util.Map;

public record ResumeImportDebug(
        String fileType,
        int textLength,
        String text,                          // 파일에서 꺼낸 글자 (정리 후)
        Map<String, List<String>> sections,   // 섹션별로 나뉜 줄
        ResumeImportResult result          // 최종 결과 (실제 API 와 같음)
) {
}
