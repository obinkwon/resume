package resume.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import resume.core.dto.FileDto;
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

    /** 프로필 사진 올리기 → { fileId, url } */
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResult> postProfileImage(@RequestParam("file") MultipartFile file,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.uploadProfileImage(file, principal.getUserId()));
    }

    /** 파일 보기 (본인 파일만) — 이력서 폼 미리보기, 이력서 미리보기·PDF 에서 사용 */
    @GetMapping("/{fileId:\\d+}")
    public ResponseEntity<Resource> getFile(@PathVariable Long fileId,
                                            @AuthenticationPrincipal UserPrincipal principal,
                                            FileDto fileDto) {
        fileDto.setUserId(principal.getUserId());
        fileDto.setFileId(fileId);
        StoredFile file = fileService.loadFile(fileDto);

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