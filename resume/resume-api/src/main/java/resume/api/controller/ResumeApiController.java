package resume.api.controller;


import com.microsoft.playwright.options.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import resume.core.dto.*;
import resume.core.importer.ResumeImportException;
import resume.core.security.UserPrincipal;
import resume.core.service.ResumeService;
import resume.core.service.TemplateService;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resume")
public class ResumeApiController {

    private final ResumeService resumeService;
    private final TemplateService templateService;

    @GetMapping("/list")
    public ResponseEntity<List<ResumeResponseDto>> getResumeList(ResumeRequestDto requestDto,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        requestDto.setUserId(principal.getUserId());
        return ResponseEntity.ok(resumeService.getResumeList(requestDto));
    }

    @GetMapping("/{resumeId:\\d+}")
    public ResponseEntity<ResumeDetailResponseDto> getResumeDetail(ResumeRequestDto requestDto,
                                                             @PathVariable Long resumeId,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        requestDto.setUserId(principal.getUserId());
        requestDto.setResumeId(resumeId);
        return ResponseEntity.ok(resumeService.getResumeDetail(requestDto));
    }

    @DeleteMapping("/{resumeId:\\d+}")
    public ResponseEntity<String> deleteResumeRemove(ResumeRequestDto requestDto,
                                                   @PathVariable Long resumeId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        requestDto.setUserId(principal.getUserId());
        requestDto.setResumeId(resumeId);
        resumeService.removeResume(requestDto);
        return ResponseEntity.ok("SUCCESS");
    }

    @PostMapping("/save")
    public ResponseEntity<Long> postResumeSave(@RequestBody ResumeRequestDto requestDto,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        requestDto.setUserId(principal.getUserId());
        return ResponseEntity.ok(resumeService.saveResume(requestDto));
    }

    @PostMapping("/template")
    public ResponseEntity<Long> postResumeTemplate(@RequestBody ResumeRequestDto request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        request.setLoginId(principal.getUsername());
        return ResponseEntity.ok(resumeService.saveResumeTemplate(request));
    }

    @GetMapping("/{resumeId:\\d+}/pdf")
    public ResponseEntity<byte[]> getResumeExportPDf(@PathVariable Long resumeId,
                                                     HttpServletRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        // 파일 이름용 제목 (본인 이력서가 아니면 서비스에서 예외가 나도록 되어 있다면 여기서 먼저 막힘)
        ResumeRequestDto requestDto = new ResumeRequestDto();
        requestDto.setResumeId(resumeId);
        requestDto.setUserId(principal.getUserId());
        ResumeDetailResponseDto resumeDetail = resumeService.getResumeDetail(requestDto);
        if (resumeDetail == null) return ResponseEntity.notFound().build();

        // 서버가 자기 자신에게 요청하는 내부 주소 (프록시·도메인을 거치지 않음)
        String baseUrl = "http://localhost:" + request.getLocalPort() + request.getContextPath();
        String previewUrl = baseUrl + "/web/resume/preview/" + resumeId;

        byte[] pdf = templateService.render(previewUrl, toBrowserCookies(request, baseUrl));
        String fileName = getFileName(resumeDetail);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)   // 한글 파일 이름
                .build();

        // PDF 다운로드 로그 저장
        resumeService.savePdfLog(PdfLogDto.builder()
                                        .resumeId(resumeDetail.getResumeId())
                                        .templateId(resumeDetail.getTemplateId())
                                        .fileName(fileName)
                                        .filePath("")
                                        .build());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }

    @PostMapping(value = "/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeImportResultDto importFile(@RequestParam("file") MultipartFile file) {
        return resumeService.importFile(file);
    }

    /** resume-form.js 처럼 err.data(텍스트) 를 그대로 alert 할 수 있게 text/plain 으로 보냄 */
    @ExceptionHandler(ResumeImportException.class)
    public ResponseEntity<String> handleImportError(ResumeImportException e) {
        return ResponseEntity.status(e.getStatus())
                .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                .body(e.getMessage());
    }

    /** 이력서 파일 파싱 확인용 — resume.import.debug=true 일 때만 등록 (운영에서는 끄기) */
    @PostMapping(value = "/import-file/debug", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeImportDebugDto debug(@RequestParam("file") MultipartFile file) {
        return resumeService.debugImportFile(file);
    }

    // 요청의 쿠키(로그인 세션 포함)를 헤드리스 크롬용으로 변환
    private List<Cookie> toBrowserCookies(HttpServletRequest request, String baseUrl) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) return List.of();
        return Arrays.stream(cookies)
                .map(c -> new Cookie(c.getName(), c.getValue()).setUrl(baseUrl))
                .toList();
    }

    // "이력서 제목.pdf" — 파일 이름에 못 쓰는 문자는 _ 로
    private String getFileName(ResumeDetailResponseDto resumeDetail) {
        String title = resumeDetail != null ? resumeDetail.getTitle() : null;
        if (title == null || title.isBlank()) title = "이력서";
        return title.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim() + ".pdf";
    }
}
