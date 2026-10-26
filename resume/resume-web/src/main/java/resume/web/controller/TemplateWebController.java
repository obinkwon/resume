package resume.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.security.UserPrincipal;
import resume.core.service.TemplateService;
import resume.core.template.SampleResumeFactory;

@Controller
@RequestMapping("/web/template")
@RequiredArgsConstructor
public class TemplateWebController {

    private final TemplateService templateService;
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
    public String templateRegistPage(TemplateRequestDto requestDto,
                         @AuthenticationPrincipal UserPrincipal principal,
                         Model model) {
        TemplateDetailResponseDto base = null;
        // 템플릿 복사해서 생성할 경우 (공개 템플릿 또는 내 템플릿만)
        if (requestDto.getTemplateId() != null) {
            base = templateService.getTemplateDetail(requestDto);
            if (base == null || !"Y".equals(base.getUseYn()) || !(Boolean.TRUE.equals(base.getIsPublic()) || templateService.isOwner(base, principal.getUserId()))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다.");
            }
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

    @GetMapping("/modify/{templateId}")
    public String templateModifyPage(@PathVariable Long templateId,
                                     TemplateRequestDto requestDto,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     Model model) {
        requestDto.setTemplateId(templateId);
        TemplateDetailResponseDto templateDetailDto = templateService.getTemplateDetail(requestDto);
        // 내 템플릿만 수정 가능 (기본 템플릿은 복사해서 만들기)
        if (!templateService.isOwner(templateDetailDto, principal.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다.");
        }

        model.addAttribute("resumeTemplate", templateDetailDto);
        model.addAttribute("mode", "U");   // C : 생성 / U : 수정
        return "thymeleaf/resume/resume-template-form";
    }
}