package resume.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResumeSource {
    DIRECT("direct","직접 작성"),
    SARAMIN("saramin","사람인"),
    WANTED("wanted","원티드"),
    JOBKOREA("jobkorea","잡코리아"),
    REMEMBER("remember","리멤버"),
    LINKEDIN("linkedin","LinkedIn"),
    FILE("file","파일 업로드");

    private final String code;
    private final String label;

    public String getCssKey() { return name().toLowerCase(); }
    public boolean isImported() { return this != DIRECT; }
}
