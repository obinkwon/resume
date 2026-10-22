package resume.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResumeSource {
    DIRECT("직접 작성"),
    SARAMIN("사람인"),
    WANTED("원티드"),
    JOBKOREA("잡코리아"),
    REMEMBER("리멤버"),
    LINKEDIN("LinkedIn"),
    FILE("파일 업로드");

    private final String label;

    public String getCssKey() { return name().toLowerCase(); }
    public boolean isImported() { return this != DIRECT; }
}
