package resume.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import resume.core.dto.TemplateDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.security.UserPrincipal;
import resume.core.service.TemplateEditorService;
import resume.core.service.TemplateService;
import resume.core.template.SampleResumeFactory;

import java.security.Principal;

@Controller
@RequestMapping("/web/template")
@RequiredArgsConstructor
public class TemplateWebController {

    private final TemplateService templateService;
    private final TemplateEditorService templateEditorService;
    private final SampleResumeFactory sampleResumeFactory;

    @GetMapping("/list")
    public String list(@AuthenticationPrincipal UserPrincipal principal,
                       Model model) {
        model.addAttribute("user", principal);
        return "thymeleaf/resume/resume-template-list";
    }

    @GetMapping("/sample-preview")
    public String samplePreview(TemplateRequestDto requestDto,
                                @AuthenticationPrincipal UserPrincipal principal,
                                Model model) {
        model.addAttribute("resumeDetail", sampleResumeFactory.create());
        if (requestDto.getTemplateId() != null) {
            model.addAttribute("resumeTemplate", templateService.getTemplateDetail(requestDto));
        }
        return "thymeleaf/resume/resume-preview";
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