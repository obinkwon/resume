package resume.core.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import resume.core.dto.FileDto;
import resume.core.mapper.FileMapper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    // 프로필 사진 최대 크기 (resume-form.js 의 PHOTO_MAX 와 같게)
    public static final long MAX_PROFILE_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final String PROFILE_DIR = "profile";

    /** 업로드 폴더 이름 규칙 : 영문 소문자·숫자·- 로 된 이름, / 로 하위 폴더 가능 (예: profile, template/thumbnail) */
    private static final Pattern DIR_PATTERN = Pattern.compile("[a-z0-9-]+(/[a-z0-9-]+)*");
    private static final DateTimeFormatter MONTH_DIR = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final String DEFAULT_ORIGINAL_NAME = "upload/image";

    private final FileMapper fileMapper;

    // R2 설정 (application.properties : resume.r2.*, 키 값은 환경변수로)
    @Value("${resume.r2.account-id}")
    private String accountId;

    @Value("${resume.r2.access-key}")
    private String accessKey;

    @Value("${resume.r2.secret-key}")
    private String secretKey;

    @Value("${resume.r2.bucket}")
    private String bucket;

    // R2 접속 클라이언트 (S3 호환 API)
    private S3Client s3;

    @PostConstruct
    void initR2Client() {
        s3 = S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))                                         // R2 는 region 을 auto 로
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                // 최신 SDK 의 기본 체크섬 방식을 R2 가 받지 못하는 경우가 있어 필요할 때만 쓰도록
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
        log.info("R2 연결 설정 완료 bucket={}", bucket);
    }

    @PreDestroy
    void closeR2Client() {
        if (s3 != null) s3.close();
    }

    // ========================================================
    // 업로드
    // ========================================================

    /**
     * 이미지 올리기 → R2 업로드 + files INSERT
     * @param dir 저장할 폴더 (예: "profile") — 코드에서 정한 값만 넘길 것 (요청 파라미터를 그대로 넘기지 않기)
     *            실제 저장 위치 : {dir}/yyyy/MM/UUID.확장자
     */
    @Transactional
    public UploadResult uploadImage(MultipartFile file, Long userId, String dir) {
        String baseDir = checkDir(dir);
        if (file == null || file.isEmpty()) {
            throw new FileException(HttpStatus.BAD_REQUEST, "사진 파일을 선택해 주세요.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
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

        String subDir = baseDir + "/" + LocalDate.now().format(MONTH_DIR);
        String storedName = UUID.randomUUID() + "." + type.getExtension();
        String key = objectKey(subDir, storedName);

        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(type.getContentType())
                            .contentLength((long) bytes.length)
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (SdkException e) {
            log.error("이미지 R2 업로드 실패 userId={} key={}", userId, key, e);
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
            deleteQuietly(key);                     // DB 저장에 실패하면 R2 에 올린 파일도 지움
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

        String key = objectKey(fileDetailDto.getFilePath(), fileDetailDto.getStoredName());
        ResponseInputStream<GetObjectResponse> object;
        try {
            object = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (NoSuchKeyException e) {
            log.warn("DB 에는 있지만 R2 에 없는 파일 fileId={} key={}", fileDto.getFileId(), key);
            throw new FileException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없어요.", e);
        } catch (SdkException e) {
            log.error("R2 파일 조회 실패 fileId={} key={}", fileDto.getFileId(), key, e);
            throw new FileException(HttpStatus.SERVICE_UNAVAILABLE, "사진을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.", e);
        }

        // 응답에 다 쓰고 나면 스프링이 스트림을 닫음
        Long size = object.response().contentLength();
        return new StoredFile(new InputStreamResource(object),
                MediaType.parseMediaType(fileDetailDto.getContentType()),
                size != null ? size : fileDetailDto.getFileSize());
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

    /**
     * 업로드 폴더 검사 : 앞뒤 / 는 지우고, 규칙(DIR_PATTERN)에 맞지 않으면 거절
     *  - '..', 대문자, 공백, 한글 등은 막아서 버킷 안 엉뚱한 위치에 쓰지 못하게 함
     *  - 개발자가 잘못 넘긴 값이므로 화면 안내(FileException)가 아니라 서버 오류로 처리
     */
    private String checkDir(String dir) {
        String d = dir == null ? "" : dir.trim().replaceAll("^/+|/+$", "");
        if (!DIR_PATTERN.matcher(d).matches()) {
            throw new IllegalArgumentException("잘못된 업로드 폴더: " + dir);
        }
        return d;
    }

    /** R2 객체 키 : 'profile/2026/10' + '/' + 'UUID.jpg' (앞뒤 / 는 정리) */
    private String objectKey(String dir, String name) {
        String d = dir == null ? "" : dir.replace('\\', '/').replaceAll("^/+|/+$", "");
        if (!StringUtils.hasText(name) || name.contains("/") || name.contains("..") || d.contains("..")) {
            throw new IllegalStateException("잘못된 파일 경로: " + dir + "/" + name);
        }
        return d.isEmpty() ? name : d + "/" + name;
    }

    private void deleteQuietly(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.warn("R2 파일 삭제 실패 key={}", key, e);
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