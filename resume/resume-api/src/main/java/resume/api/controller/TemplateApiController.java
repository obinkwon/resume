package resume.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.dto.TemplateResponseDto;
import resume.core.service.TemplateService;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/template")
public class TemplateApiController {

    private static final Set<String> TEMPLATES = Set.of("minimal", "modern", "ats");
    private static final Set<String> LAYOUTS   = Set.of("single", "sidebar", "split");
    private static final Set<String> FONTS     = Set.of("Pretendard", "Noto Sans KR", "Roboto");
    private static final Set<String> SECTIONS  =
            Set.of("intro", "skill", "education", "certificate", "experience", "project", "link");

    private final TemplateService templateService;

    @GetMapping("/list")
    public ResponseEntity<List<TemplateResponseDto>> getTemplateList(TemplateRequestDto requestDto) {
        return ResponseEntity.ok(templateService.getTemplateList(requestDto));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateDetailResponseDto> getTemplateDetail(@PathVariable Long templateId, TemplateRequestDto requestDto) {

        requestDto.setTemplateId(templateId);
        return ResponseEntity.ok(templateService.getTemplateDetail(requestDto));
    }

//    @PostMapping
//    public ResponseEntity<Map<String, Long>> create(@RequestBody TemplateSettingDto dto,
//                                                    @RequestParam(required = false) Long resumeId) {
//        if (!isValid(dto)) return ResponseEntity.badRequest().build();
//
//        dto.setTemplateId(null);
//        Long templateId = templateService.create(dto);
//
//        // 이력서에서 [템플릿] 으로 들어와 만든 경우 바로 연결
//        if (resumeId != null) templateService.applyToResume(templateId, resumeId);
//
//        return ResponseEntity
//                .created(URI.create("/api/templates/" + templateId))
//                .body(Map.of("templateId", templateId));
//    }
//
//    @PutMapping("/{templateId}")
//    public ResponseEntity<Void> update(@PathVariable Long templateId,
//                                       @RequestBody TemplateSettingDto dto) {
//        if (!isValid(dto)) return ResponseEntity.badRequest().build();
//        dto.setTemplateId(templateId);
//        templateService.update(dto);           // 본인 템플릿인지 서비스에서 확인
//        return ResponseEntity.noContent().build();
//    }
//
//    @DeleteMapping("/{templateId}")
//    public ResponseEntity<Void> delete(@PathVariable Long templateId) {
//        templateService.delete(templateId);    // resume.template_id = NULL 처리 후 삭제
//        return ResponseEntity.noContent().build();
//    }
//
//    @PutMapping("/{templateId}/resumes/{resumeId}")
//    public ResponseEntity<Void> apply(@PathVariable Long templateId, @PathVariable Long resumeId) {
//        templateService.applyToResume(templateId, resumeId);   // 템플릿·이력서 둘 다 본인 것인지 확인
//        return ResponseEntity.noContent().build();
//    }
//
//    @DeleteMapping("/{templateId}/resumes/{resumeId}")
//    public ResponseEntity<Void> detach(@PathVariable Long templateId, @PathVariable Long resumeId) {
//        templateService.detachFromResume(templateId, resumeId);
//        return ResponseEntity.noContent().build();
//    }
}