package resume.core.utils;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import resume.core.exception.FileException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class FileUtils {

    public static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    public static final Pattern DIR_PATTERN = Pattern.compile("[a-z0-9-]+(/[a-z0-9-]+)*");
    public static final DateTimeFormatter MONTH_DIR = DateTimeFormatter.ofPattern("yyyy/MM");
    public static final String DEFAULT_ORIGINAL_NAME = "image";

    public static void validateImageUrl(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new FileException(HttpStatus.BAD_REQUEST, "올바른 사진 URL이 아니에요.", e);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
            throw new FileException(HttpStatus.BAD_REQUEST, "https:// 로 시작하는 사진 주소만 쓸 수 있어요.");
        }
    }

    /**
     * 업로드 폴더 검사 : 앞뒤 / 는 지우고, 규칙(DIR_PATTERN)에 맞지 않으면 거절
     *  - '..', 대문자, 공백, 한글 등은 막아서 버킷 안 엉뚱한 위치에 쓰지 못하게 함
     *  - 개발자가 잘못 넘긴 값이므로 화면 안내(FileException)가 아니라 서버 오류로 처리
     */
    public static String checkDir(String dir) {
        String d = dir == null ? "" : dir.trim().replaceAll("^/+|/+$", "");
        if (!DIR_PATTERN.matcher(d).matches()) {
            throw new IllegalArgumentException("잘못된 업로드 폴더: " + dir);
        }
        return d;
    }

    /** R2 객체 키 : 'profile/2026/10' + '/' + 'UUID.jpg' (앞뒤 / 는 정리) */
    public static String objectKey(String dir, String name) {
        String d = dir == null ? "" : dir.replace('\\', '/').replaceAll("^/+|/+$", "");
        if (!StringUtils.hasText(name) || name.contains("/") || name.contains("..") || d.contains("..")) {
            throw new IllegalStateException("잘못된 파일 경로: " + dir + "/" + name);
        }
        return d.isEmpty() ? name : d + "/" + name;
    }

    /** 화면 표시용 원본 파일명 : 경로·제어문자 빼고 255자까지 */
    public static String cleanOriginalName(String name) {
        if (!StringUtils.hasText(name)) return DEFAULT_ORIGINAL_NAME;
        String cleaned = StringUtils.getFilename(name.replace('\\', '/'));
        if (cleaned == null) return DEFAULT_ORIGINAL_NAME;
        cleaned = cleaned.replaceAll("\\p{Cntrl}", "").trim();
        if (cleaned.isEmpty()) return DEFAULT_ORIGINAL_NAME;
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }
}
