package resume.core.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import resume.core.dto.FileDto;
import resume.core.mapper.FileMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 업로드 파일 (지금은 프로필 사진만)
 *  - 파일은 디스크(resume.upload.dir)에, DB(files)에는 정보만 저장
 *  - 올린 사진은 이력서 저장 전까지 어디에도 연결되지 않음 → 하루 지나도 안 쓰이면 정리 배치가 지움
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    /** 프로필 사진 최대 크기 (resume-form.js 의 PHOTO_MAX 와 같게) */
    public static final long MAX_PROFILE_IMAGE_SIZE = 5L * 1024 * 1024;
    /** 외부 사진 URL 최대 길이 (resume_profile.profile_image_url) */
    public static final int MAX_IMAGE_URL_LENGTH = 1000;

    private static final String PROFILE_DIR = "profile";
    private static final DateTimeFormatter MONTH_DIR = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final String DEFAULT_ORIGINAL_NAME = "profile-image";

    private final FileMapper fileMapper;

    /** 저장 기준 경로 (application.yml : resume.upload.dir) */
    @Value("${resume.upload.dir:./upload}")
    private String uploadDir;

    // ========================================================
    // 업로드
    // ========================================================

    /** 프로필 사진 올리기 → 파일 저장 + files INSERT */
    @Transactional
    public UploadResult uploadProfileImage(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new FileException(HttpStatus.BAD_REQUEST, "사진 파일을 선택해 주세요.");
        }
        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new FileException(HttpStatus.PAYLOAD_TOO_LARGE, "사진은 5MB까지 올릴 수 있어요.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new FileException(HttpStatus.BAD_REQUEST, "사진을 읽지 못했어요. 다시 시도해 주세요.", e);
        }

        // 확장자·Content-Type 은 바꿀 수 있으니 파일 내용(매직 바이트)으로 판별
        ImageType type = ImageType.detect(bytes);
        if (type == null) {
            throw new FileException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "JPG, PNG, WEBP 사진만 올릴 수 있어요.");
        }

        String subDir = PROFILE_DIR + "/" + LocalDate.now().format(MONTH_DIR);
        String storedName = UUID.randomUUID() + "." + type.getExtension();
        Path target = resolve(subDir, storedName);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            log.error("프로필 사진 저장 실패 userId={} path={}", userId, target, e);
            throw new FileException(HttpStatus.INTERNAL_SERVER_ERROR, "사진을 저장하지 못했어요. 잠시 후 다시 시도해 주세요.", e);
        }

        FileDto fileDto = FileDto.builder()
                .userId(userId)
                .originalName(cleanOriginalName(file.getOriginalFilename()))
                .storedName(storedName)
                .filePath(subDir)
                .contentType(type.getContentType())
                .fileSize((long) bytes.length)
                .build();
        try {
            fileMapper.insertFile(fileDto);
        } catch (RuntimeException e) {
            deleteQuietly(target);                  // DB 저장에 실패하면 디스크 파일도 지움
            throw e;
        }

        return new UploadResult(fileDto.getFileId(), "/api/files/" + fileDto.getFileId());
    }

    // ========================================================
    // 조회
    // ========================================================

    /** 본인 파일만 내려줌 (남의 파일·없는 파일은 똑같이 404) */
    public StoredFile loadFile(FileDto fileDto) {
        FileDto fileDetailDto = fileMapper.selectFile(fileDto);
        if (fileDetailDto == null) {
            throw new FileException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없어요.");
        }

        Path path = resolve(fileDetailDto.getFilePath(), fileDetailDto.getStoredName());
        if (!Files.isReadable(path)) {
            log.warn("DB 에는 있지만 디스크에 없는 파일 fileId={} path={}", fileDto.getFileId(), path);
            throw new FileException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없어요.");
        }

        return new StoredFile(new FileSystemResource(path),
                MediaType.parseMediaType(fileDetailDto.getContentType()),
                fileDetailDto.getFileSize());
    }

    // ========================================================
    // 이력서 저장 시 검사 (ResumeService.saveResume 에서 호출)
    // ========================================================

    /**
     * 프로필 사진 값 검사
     *  - fileDto.userId   : 로그인 사용자
     *  - fileDto.fileId   : 업로드 사진 (없으면 null)
     *  - fileDto.imageUrl : 외부 사진 URL (없으면 null)
     *  - 업로드 사진과 외부 URL 은 둘 중 하나만 / URL 은 https 만, 1000자까지 / fileId 는 본인이 올린 파일만
     */
    public void validateProfileImage(FileDto fileDto) {
        Long fileId = fileDto.getFileId();
        String imageUrl = fileDto.getImageUrl();
        boolean hasUrl = StringUtils.hasText(imageUrl);

        if (fileId != null && hasUrl) {
            throw new FileException(HttpStatus.BAD_REQUEST, "프로필 사진은 올린 사진과 URL 중 하나만 쓸 수 있어요.");
        }
        if (hasUrl) {
            validateImageUrl(imageUrl.trim());
        }
        if (fileId != null && fileMapper.selectFile(fileDto) == null) {
            throw new FileException(HttpStatus.BAD_REQUEST, "프로필 사진을 찾을 수 없어요. 사진을 다시 올려 주세요.");
        }
    }

    private void validateImageUrl(String url) {
        if (url.length() > MAX_IMAGE_URL_LENGTH) {
            throw new FileException(HttpStatus.BAD_REQUEST, "사진 URL이 너무 길어요. (1000자까지)");
        }
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
    // ========================================================
    // 내부
    // ========================================================

    private Path root() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** 저장 기준 경로 밖으로 나가는 경로는 막음 */
    private Path resolve(String dir, String name) {
        Path root = root();
        Path path = root.resolve(dir).resolve(name).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalStateException("저장 경로를 벗어난 파일 경로: " + path);
        }
        return path;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("파일 삭제 실패 path={}", path, e);
        }
    }

    /** 화면 표시용 원본 파일명 : 경로·제어문자 빼고 255자까지 */
    private String cleanOriginalName(String name) {
        if (!StringUtils.hasText(name)) return DEFAULT_ORIGINAL_NAME;
        String cleaned = StringUtils.getFilename(name.replace('\\', '/'));
        if (cleaned == null) return DEFAULT_ORIGINAL_NAME;
        cleaned = cleaned.replaceAll("\\p{Cntrl}", "").trim();
        if (cleaned.isEmpty()) return DEFAULT_ORIGINAL_NAME;
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    /** 허용하는 사진 형식 */
    @Getter
    private enum ImageType {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp");

        private final String extension;
        private final String contentType;

        ImageType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        static ImageType detect(byte[] b) {
            if (b.length >= 3 && u(b[0]) == 0xFF && u(b[1]) == 0xD8 && u(b[2]) == 0xFF) {
                return JPEG;
            }
            if (b.length >= 8 && u(b[0]) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                    && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
                return PNG;
            }
            if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return WEBP;
            }
            return null;
        }

        private static int u(byte v) {
            return v & 0xFF;
        }
    }

    // ========================================================
    // 결과 · 예외
    // ========================================================

    /** 업로드 응답 : resume-form.js 가 fileId 를 hidden 에 넣고 url 로 미리보기 */
    public record UploadResult(Long fileId, String url) {
    }

    /** 조회 결과 */
    public record StoredFile(Resource resource, MediaType contentType, long size) {
    }

    /** 화면에 그대로 보여줄 안내 문구 + HTTP 상태 */
    @Getter
    public static class FileException extends RuntimeException {
        private final HttpStatus status;

        public FileException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public FileException(HttpStatus status, String message, Throwable cause) {
            super(message, cause);
            this.status = status;
        }
    }
}