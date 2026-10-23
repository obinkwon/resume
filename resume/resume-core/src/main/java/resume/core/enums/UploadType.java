package resume.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import resume.core.service.FileService;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum UploadType {
    PROFILE("profile", "profile");

    private final String code; // 화면에서 보내는 값
    private final String dir; // R2 저장 폴더

    public static UploadType from(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code == null ? "" : code.trim()))
                .findFirst()
                .orElseThrow(() -> new FileService.FileException(HttpStatus.BAD_REQUEST, "지원하지 않는 업로드 종류예요."));
    }
}
