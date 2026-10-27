package resume.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import resume.core.dto.FileDto;
import resume.core.enums.UploadType;
import resume.core.record.ImageUrlRequest;
import resume.core.record.StoredFile;
import resume.core.record.UploadResult;
import resume.core.security.UserPrincipal;
import resume.core.service.FileService;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileApiController {

    private final FileService fileService;

    // 이미지 파일 업로드
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResult> postUploadImage(@RequestParam("file") MultipartFile file,
                                                        @RequestParam("type") String type,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        UploadType uploadType = UploadType.from(type);
        return ResponseEntity.ok(fileService.uploadImage(file, principal.getUserId(), uploadType.getDir()));
    }

    // 외부 URL 의 이미지를 서버가 받아서 업로드 (업로드 사진과 똑같이 저장)
    @PostMapping(value = "/upload/image-url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UploadResult> postUploadImageUrl(@RequestBody ImageUrlRequest request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        UploadType uploadType = UploadType.from(request.type());
        Long fileId = fileService.uploadImageFromUrl(request.url(), principal.getUserId(), uploadType.getDir());
        return ResponseEntity.ok(new UploadResult(fileId, "/api/files/" + fileId));
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
}