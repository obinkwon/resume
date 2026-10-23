package resume.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import resume.core.dto.FileDto;
import resume.core.enums.UploadType;
import resume.core.security.UserPrincipal;
import resume.core.service.FileService;
import resume.core.service.FileService.FileException;
import resume.core.service.FileService.StoredFile;
import resume.core.service.FileService.UploadResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileApiController {

    private final FileService fileService;

    /**
     * 이미지 업로드 (공통) → { fileId, url }
     *  - type : 용도 (profile 등) — 화면은 용도만 보내고, 실제 저장 폴더는 서버가 정함 (ImageType 참고)
     *  - 예) FormData : file=사진, type=profile  →  R2 profile/yyyy/MM/UUID.jpg
     */
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResult> postUploadImage(@RequestParam("file") MultipartFile file,
                                                        @RequestParam("type") String type,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        UploadType uploadType = UploadType.from(type);
        return ResponseEntity.ok(fileService.uploadImage(file, principal.getUserId(), uploadType.getDir()));
    }

    // 파일 조회
    @GetMapping("/{fileId:\\d+}")
    public ResponseEntity<Resource> getFile(@PathVariable Long fileId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        // 본인 파일만 조회
        StoredFile file = fileService.loadFile(FileDto.builder()
                                                        .fileId(fileId)
                                                        .userId(principal.getUserId())
                                                        .build());

        return ResponseEntity.ok()
                .contentType(file.contentType())
                .contentLength(file.size())
                // 같은 fileId 의 내용은 바뀌지 않으므로 브라우저에만 캐시
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }

    /** resume-form.js(ApiError) 가 안내 문구를 그대로 보여줄 수 있게 text/plain 으로 보냄 */
    @ExceptionHandler(FileException.class)
    public ResponseEntity<String> handleFileError(FileException e) {
        return ResponseEntity.status(e.getStatus())
                .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                .body(e.getMessage());
    }
}