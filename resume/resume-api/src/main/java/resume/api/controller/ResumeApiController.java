package resume.api.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import resume.core.dto.ResumeRequestDto;
import resume.core.security.UserPrincipal;
import resume.core.service.ResumeService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resume")
public class ResumeApiController {

    private final ResumeService resumeService;

    @PostMapping("/save")
    public ResponseEntity<Long> procResumeSave(@RequestBody ResumeRequestDto request,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        request.setUserId(principal.getUserId());
        return ResponseEntity.ok(resumeService.saveResume(request));
    }

    @PostMapping("/template")
    public ResponseEntity<Long> procResumeTemplate(@RequestBody ResumeRequestDto request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        request.setUserId(principal.getUserId());
        return ResponseEntity.ok(resumeService.saveResumeTemplate(request));
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> procResumeExportPDf(@PathVariable UUID id, @RequestParam String template) {
        return null;
    }
}
