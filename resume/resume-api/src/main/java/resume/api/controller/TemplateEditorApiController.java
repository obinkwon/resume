package resume.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import resume.core.dto.TemplateApplyRequestDto;
import resume.core.dto.TemplateSaveRequestDto;
import resume.core.service.TemplateEditorService;

import java.security.Principal;
import java.util.Map;

/**
 * 템플릿 직접 만들기 API
 *  POST   /api/template/save          저장 (덮어쓰기 또는 새 템플릿) → { templateId }
 *  POST   /api/template/apply         이력서에 템플릿 연결
 *  DELETE /api/template/{templateId}  내 템플릿 삭제
 *
 * 오류는 400 { message } 로 응답 (편집기가 message 를 그대로 보여 줌)
 */
@RestController
@RequestMapping("/api/template")
@RequiredArgsConstructor
public class TemplateEditorApiController {

    private final TemplateEditorService templateEditorService;

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> save(@RequestBody TemplateSaveRequestDto req, Principal principal) {
        Long templateId = templateEditorService.save(req, principal.getName());
        return ResponseEntity.ok(Map.of("templateId", templateId, "message", "저장했습니다."));
    }

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> apply(@RequestBody TemplateApplyRequestDto req, Principal principal) {
        templateEditorService.apply(req, principal.getName());
        return ResponseEntity.ok(Map.of("message", "이력서에 적용했습니다."));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long templateId, Principal principal) {
        templateEditorService.delete(templateId, principal.getName());
        return ResponseEntity.ok(Map.of("message", "삭제했습니다."));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() == null ? "요청을 처리하지 못했습니다." : e.getMessage()));
    }
}