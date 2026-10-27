package resume.core.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import resume.core.dto.FileDto;
import resume.core.enums.ImageType;
import resume.core.exception.FileException;
import resume.core.mapper.FileMapper;
import resume.core.record.StoredFile;
import resume.core.record.UploadResult;
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
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static resume.core.utils.FileUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

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

    // ---------- 외부 URL 사진 받기 ----------
    /** 따라갈 리다이렉트 최대 횟수 (매번 주소 검사) */
    private static final int URL_MAX_REDIRECTS = 3;
    private static final Duration URL_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration URL_TIMEOUT = Duration.ofSeconds(10);
    private static final String URL_FAIL_MESSAGE = "사진 URL에서 이미지를 불러오지 못했어요. 주소를 확인하거나 사진을 직접 올려 주세요.";

    /** 리다이렉트는 직접 따라가며 주소를 검사 (내부망으로 돌려보내는 것 방지) */
    private static final HttpClient URL_CLIENT = HttpClient.newBuilder()
            .connectTimeout(URL_CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private record DownloadedImage(byte[] bytes, String fileName) { }

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

    // R2 파일 제거
    private void deleteQuietly(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.warn("R2 파일 삭제 실패 key={}", key, e);
        }
    }

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

        Long fileId = storeImage(bytes, userId, baseDir, file.getOriginalFilename());
        return new UploadResult(fileId, "/api/files/" + fileId);
    }

    /**
     * 외부 URL 의 사진을 받아서 업로드 사진과 똑같이 저장 → R2 업로드 + files INSERT
     *  - https, 기본 포트만 / 내부망·localhost 주소 차단 (리다이렉트마다 다시 검사)
     *  - 크기·형식 검사는 직접 올린 사진과 같음
     *  - 다운로드는 트랜잭션 밖 (외부 서버가 느려도 DB 커넥션을 붙잡지 않음)
     * @param dir 저장할 폴더 (uploadImage 와 같음)
     */
    public UploadResult uploadImageFromUrl(String imageUrl, Long userId, String dir) {
        String baseDir = checkDir(dir);
        if (!StringUtils.hasText(imageUrl)) {
            throw new FileException(HttpStatus.BAD_REQUEST, "사진 URL을 입력해 주세요.");
        }
        String url = imageUrl.trim();
        validateImageUrl(url);

        DownloadedImage img = downloadImage(url);
        Long fileId = storeImage(img.bytes(), userId, baseDir, img.fileName());
        return new UploadResult(fileId, "/api/files/" + fileId);
    }

    /** 이미지 검사 → R2 업로드 → files INSERT (업로드·URL 공통). 저장된 file_id 반환 */
    private Long storeImage(byte[] bytes, Long userId, String baseDir, String originalName) {
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
                .originalName(cleanOriginalName(originalName))
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

        return fileDto.getFileId();
    }

    // 본인 파일만 내려줌 (남의 파일·없는 파일은 똑같이 404)
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

    // =========================================================
    //   외부 URL 다운로드
    // =========================================================

    /** URL 에서 사진을 받아 옴 (크기 제한까지만 읽음) */
    private DownloadedImage downloadImage(String imageUrl) {
        URI first = toHttpsUri(imageUrl);
        URI uri = first;

        for (int i = 0; i <= URL_MAX_REDIRECTS; i++) {
            checkPublicHost(uri);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(URL_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (resume-image)")
                    .header("Accept", "image/*")
                    .GET()
                    .build();

            HttpResponse<InputStream> res;
            try {
                res = URL_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw urlFail("중단됨", uri);
            } catch (IOException e) {
                throw urlFail("요청 실패 " + e.getClass().getSimpleName(), uri);
            }

            try (InputStream in = res.body()) {
                int status = res.statusCode();

                // 리다이렉트 : 다음 주소도 같은 검사를 거치도록 직접 따라감
                if (status >= 300 && status < 400) {
                    String location = res.headers().firstValue("Location")
                            .orElseThrow(() -> urlFail("리다이렉트 주소 없음", first));
                    try {
                        uri = toHttpsUri(uri.resolve(location).toString());
                    } catch (IllegalArgumentException e) {
                        throw urlFail("잘못된 리다이렉트 주소", first);
                    }
                    continue;
                }
                if (status != 200) {
                    throw urlFail("상태 " + status, uri);
                }

                byte[] bytes = in.readNBytes((int) MAX_IMAGE_SIZE + 1);
                if (bytes.length > MAX_IMAGE_SIZE) {
                    throw new FileException(HttpStatus.PAYLOAD_TOO_LARGE, "사진은 5MB까지 쓸 수 있어요.");
                }

                log.info("사진 URL 다운로드 host={} size={}KB", uri.getHost(), bytes.length / 1024);
                return new DownloadedImage(bytes, fileNameOf(first));
            } catch (IOException e) {
                throw urlFail("읽기 실패", uri);
            }
        }
        throw urlFail("리다이렉트가 너무 많음", first);
    }

    /** https 이고 기본 포트(443)인 주소만 허용 */
    private static URI toHttpsUri(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw urlFail("잘못된 주소", null);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw urlFail("https 주소 아님", uri);
        }
        if (uri.getPort() != -1 && uri.getPort() != 443) {
            throw urlFail("허용하지 않는 포트 " + uri.getPort(), uri);
        }
        return uri;
    }

    /** 내부망·localhost 등 서버 안쪽 주소면 거부 (SSRF 방지) */
    private static void checkPublicHost(URI uri) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException e) {
            throw urlFail("호스트를 찾을 수 없음", uri);
        }
        for (InetAddress a : addresses) {
            boolean uniqueLocalV6 = a instanceof Inet6Address && (a.getAddress()[0] & 0xFE) == 0xFC;  // fc00::/7
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                    || a.isSiteLocalAddress() || a.isMulticastAddress() || uniqueLocalV6) {
                throw urlFail("내부 주소 " + a.getHostAddress(), uri);
            }
        }
    }

    /** URL 마지막 부분을 원본 파일 이름으로 (정리는 cleanOriginalName 에서) */
    private static String fileNameOf(URI uri) {
        String path = uri.getPath() == null ? "" : uri.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1).trim();
        return StringUtils.hasText(name) ? name : "url-image";
    }

    /** 실패 로그 (개인정보라 전체 URL 대신 호스트만) + 화면용 예외 */
    private static FileException urlFail(String reason, URI uri) {
        log.info("사진 URL 다운로드 실패 host={} reason={}", uri == null ? null : uri.getHost(), reason);
        return new FileException(HttpStatus.BAD_REQUEST, URL_FAIL_MESSAGE);
    }
}