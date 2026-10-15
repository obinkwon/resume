package resume.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resume.core.dto.TemplateSettingDto;
import resume.core.service.TemplateService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 템플릿 REST API (JSON)
 *  POST   /api/templates                                 : 템플릿 생성 (이력서 없이)
 *  GET    /api/templates                                 : 내 템플릿 목록
 *  GET    /api/templates/{templateId}                    : 템플릿 조회
 *  PUT    /api/templates/{templateId}                    : 템플릿 수정
 *  DELETE /api/templates/{templateId}                    : 템플릿 삭제 (연결된 이력서의 template_id 는 NULL 로)
 *  PUT    /api/templates/{templateId}/resumes/{resumeId} : 이력서에 템플릿 적용 (나중에 연결)
 *  DELETE /api/templates/{templateId}/resumes/{resumeId} : 이력서 템플릿 해제
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templates")
public class TemplateApiController {

    private static final Set<String> TEMPLATES = Set.of("minimal", "modern", "ats");
    private static final Set<String> LAYOUTS   = Set.of("single", "sidebar", "split");
    private static final Set<String> FONTS     = Set.of("Pretendard", "Noto Sans KR", "Roboto");
    private static final Set<String> SECTIONS  =
            Set.of("intro", "skill", "education", "certificate", "experience", "project", "link");

    private final TemplateService templateService;

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
//    @GetMapping
//    public List<TemplateSettingDto> list() {
//        return templateService.getMyTemplates();
//    }
//
//    @GetMapping("/{templateId}")
//    public TemplateSettingDto get(@PathVariable Long templateId) {
//        return templateService.getTemplate(templateId);
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

    // ---------- 검증 ----------
    private boolean isValid(TemplateSettingDto dto) {
        if (dto == null) return false;
        if (dto.getTemplateName() == null || dto.getTemplateName().isBlank()
                || dto.getTemplateName().length() > 50) return false;
        if (!TEMPLATES.contains(dto.getTemplateCode())
                || !LAYOUTS.contains(dto.getLayoutType())
                || !FONTS.contains(dto.getFontFamily())) return false;
        if (dto.getPrimaryColor() == null || !dto.getPrimaryColor().matches("^#[0-9A-Fa-f]{6}$")) return false;
        if (dto.getFontScale() < 85 || dto.getFontScale() > 115) return false;
        return dto.getSections() != null && dto.getSections().stream()
                .allMatch(s -> SECTIONS.contains(s.getKey()));
    }
}