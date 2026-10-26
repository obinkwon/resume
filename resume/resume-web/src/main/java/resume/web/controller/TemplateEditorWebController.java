package resume.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import resume.core.dto.TemplateDto;
import resume.core.service.TemplateEditorService;
import resume.core.template.SampleResumeFactory;

import java.security.Principal;

/**
 * 템플릿 직접 만들기 화면
 *  /web/template/list                     내 템플릿 목록 (새로 만들기, 편집, 이력서에 적용, 삭제)
 *  /web/template/new?resumeId=&from=      새 템플릿 (from: 복사해서 시작할 템플릿)
 *  /web/template/edit/{templateId}?resumeId=
 *  /web/template/sample-preview?templateId=   샘플 데이터 미리보기 (편집기 iframe, 목록 썸네일)
 *
 * resumeId 가 있으면 이력서 화면에서 들어온 것 → 저장 시 그 이력서에 연결, 미리보기에서 내 이력서 데이터로 전환 가능
 */
@Controller
@RequestMapping("/web/template")
@RequiredArgsConstructor
public class TemplateEditorWebController {

    private final TemplateEditorService templateEditorService;
    private final SampleResumeFactory sampleResumeFactory;

    @GetMapping("/list")
    public String list(@RequestParam(required = false) Long resumeId, Principal principal, Model model) {
        String loginId = principal.getName();
        if (resumeId != null) checkResume(resumeId, loginId);

        model.addAttribute("resumeId", resumeId);
//        model.addAttribute("myTemplates", templateEditorService.findMyTemplates(loginId));
//        model.addAttribute("publicTemplates", templateEditorService.findPublicTemplates());
//        model.addAttribute("myResumes", templateEditorService.findMyResumes(loginId));
        return "thymeleaf/resume/resume-template-list";
    }

    @GetMapping("/new")
    public String create(@RequestParam(required = false) Long resumeId,
                         @RequestParam(required = false) Long from,
                         Principal principal, Model model) {
        String loginId = principal.getName();
        if (resumeId != null) checkResume(resumeId, loginId);

        TemplateDto base = null;
        if (from != null) {
            base = findTemplate(from, loginId);
        }

        // 복사해서 시작해도 원본과 연결하지 않음 (저장하면 항상 새 템플릿)
        TemplateDto draft = TemplateDto.builder()
                .templateName(base == null ? "새 템플릿" : base.getTemplateName() + " 복사본")
                .htmlContent(base == null ? "" : base.getHtmlContent())
                .cssContent(base == null ? "" : base.getCssContent())
                .build();

        return editor(model, resumeId, draft, false);
    }

    @GetMapping("/edit/{templateId}")
    public String edit(@PathVariable Long templateId,
                       @RequestParam(required = false) Long resumeId,
                       Principal principal, Model model) {
        String loginId = principal.getName();
        if (resumeId != null) checkResume(resumeId, loginId);

        TemplateDto template = findTemplate(templateId, loginId);
        boolean own = templateEditorService.isOwner(template, loginId);
        if (!own) {
            // 기본 템플릿은 고칠 수 없으니 복사본 이름으로 시작 (저장하면 새 템플릿)
            template.setTemplateName(template.getTemplateName() + " 복사본");
        }
        return editor(model, resumeId, template, own);
    }

    @GetMapping("/sample-preview")
    public String samplePreview(@RequestParam(required = false) Long templateId, Principal principal, Model model) {
        model.addAttribute("resumeDetail", sampleResumeFactory.create());
        if (templateId != null) {
            model.addAttribute("resumeTemplate", findTemplate(templateId, principal.getName()));
        }
        return "resume-preview";
    }

    private String editor(Model model, Long resumeId, TemplateDto template, boolean ownTemplate) {
        model.addAttribute("resumeId", resumeId);
        model.addAttribute("resumeTemplate", template);
        model.addAttribute("ownTemplate", ownTemplate);
        return "thymeleaf/resume/resume-template-editor";
    }

    private TemplateDto findTemplate(Long templateId, String loginId) {
        try {
            return templateEditorService.findViewable(templateId, loginId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private void checkResume(Long resumeId, String loginId) {
        try {
            templateEditorService.checkMyResume(resumeId, loginId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}