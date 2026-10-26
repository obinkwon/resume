package resume.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.dto.TemplateResponseDto;
import resume.core.security.UserPrincipal;
import resume.core.service.TemplateService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/template")
public class TemplateApiController {

    private final TemplateService templateService;

    @GetMapping("/list")
    public ResponseEntity<List<TemplateResponseDto>> getTemplateList(TemplateRequestDto requestDto,
                                                                     @AuthenticationPrincipal UserPrincipal principal) {
        requestDto.setUserId(principal.getUserId());
        return ResponseEntity.ok(templateService.getTemplateList(requestDto));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateDetailResponseDto> getTemplateDetail(@PathVariable Long templateId, TemplateRequestDto requestDto) {

        requestDto.setTemplateId(templateId);
        return ResponseEntity.ok(templateService.getTemplateDetail(requestDto));
    }
}