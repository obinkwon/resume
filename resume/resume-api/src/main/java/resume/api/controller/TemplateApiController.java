package resume.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.dto.TemplateResponseDto;
import resume.core.dto.TemplateSaveRequestDto;
import resume.core.security.UserPrincipal;
import resume.core.service.TemplateService;

import java.security.Principal;
import java.util.List;
import java.util.Map;

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

    /** 템플릿 저장 (덮어쓰기 또는 새 템플릿) → { templateId } */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveTemplate(@RequestBody TemplateSaveRequestDto requestDto,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        Long templateId = templateService.saveTemplate(requestDto, principal.getUserId());
        return ResponseEntity.ok(Map.of("templateId", templateId, "message", "저장했습니다."));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(TemplateRequestDto requestDto,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        requestDto.setUserId(principal.getUserId());
        templateService.deleteTemplate(requestDto);
        return ResponseEntity.ok(Map.of("message", "삭제했습니다."));
    }
}