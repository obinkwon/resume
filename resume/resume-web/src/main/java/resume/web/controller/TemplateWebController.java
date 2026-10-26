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
import resume.core.dto.TemplateDetailResponseDto;
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
    public String templateListPage() {
        return "thymeleaf/resume/resume-template-list";
    }

    @GetMapping("/preview")
    public String templatePreviewPage(TemplateRequestDto requestDto,
                                @AuthenticationPrincipal UserPrincipal principal,
                                Model model) {
        model.addAttribute("resumeDetail", sampleResumeFactory.create());
        if (requestDto.getTemplateId() != null) {
            model.addAttribute("resumeTemplate", templateService.getTemplateDetail(requestDto));
        }
        return "thymeleaf/resume/resume-preview";
    }

    @GetMapping("/regist")
    public String templateRegistPagecreate(TemplateRequestDto requestDto,
                         Model model) {
        TemplateDetailResponseDto base = null;
        // 템플릿 복사해서 생성할 경우
        if (requestDto.getTemplateId() != null) {
            base = templateService.getTemplateDetail(requestDto);
        }

        TemplateDto draft = TemplateDto.builder()
                .templateName(base == null ? "새 템플릿" : base.getTemplateName() + " 복사본")
                .htmlContent(base == null ? "" : base.getHtmlContent())
                .cssContent(base == null ? "" : base.getCssContent())
                .build();
        model.addAttribute("resumeTemplate", draft);
        model.addAttribute("mode", "C");   // C : 생성 / U : 수정
        return "thymeleaf/resume/resume-template-form";
    }

    @GetMapping("/edit/{templateId}")
    public String edit(@PathVariable Long templateId,
                       @RequestParam(required = false) Long resumeId,
                       Principal principal, Model model) {
        String loginId = principal.getName();
        if (resumeId != null) checkResume(resumeId, loginId);

        TemplateDto template = findTemplate(templateId, loginId);

        model.addAttribute("resumeId", resumeId);
        model.addAttribute("resumeTemplate", template);
        model.addAttribute("mode", "U");   // C : 생성 / U : 수정
        return "thymeleaf/resume/resume-template-form";
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