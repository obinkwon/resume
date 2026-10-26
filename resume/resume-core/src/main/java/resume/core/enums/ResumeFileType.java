package resume.core.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 이력서 가져오기에서 받는 파일 형식
 */
public enum ResumeFileType {
    PDF("pdf"),
    DOCX("docx"),
    HWPX("hwpx");

    private final String extension;

    ResumeFileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public static Optional<ResumeFileType> fromExtension(String ext) {
        if (ext == null) return Optional.empty();
        String e = ext.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(t -> t.extension.equals(e)).findFirst();
    }
}
