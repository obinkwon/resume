package resume.api;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resume.core.dto.ResumeSaveRequestDto;
import resume.core.service.ResumeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeApiController {

    private final ResumeService resumeService;

    @PostMapping("/save")
    public ResponseEntity<Long> save(@RequestBody ResumeSaveRequestDto request) {
        return ResponseEntity.ok(resumeService.saveResume(request));
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id, @RequestParam String template) {
        return null;
    }
}
