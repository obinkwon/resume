package resume.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import resume.core.dto.ResumeDetailResponseDto;
import resume.core.dto.ResumeRequestDto;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.security.UserPrincipal;
import resume.core.service.ResumeService;
import resume.core.service.TemplateService;
import resume.core.template.SampleResumeFactory;

@Controller
@RequiredArgsConstructor
@RequestMapping("/web/resume")
public class ResumeWebController {

	private final ResumeService resumeService;
	private final TemplateService templateService;
    private final SampleResumeFactory sampleResumeFactory;

	@GetMapping("/list")
	public String resumeListPage(@AuthenticationPrincipal UserPrincipal principal,
                                 Model model,
                                 ResumeRequestDto requestDto) {
		requestDto.setUserId(principal.getUserId());

		model.addAttribute("user", principal);
		model.addAttribute("resumeList", resumeService.getResumeList(requestDto));
		return "thymeleaf/resume/resume-list";
	}

	@GetMapping("/regist")
	public String resumeRegistPage(Model model) {

		return "thymeleaf/resume/resume-form";
	}

	@GetMapping("/modify/{resumeId}")
    public String resumeModifyPage(@PathVariable Long resumeId,
                                   @AuthenticationPrincipal UserPrincipal principal,
                                   Model model,
                                   ResumeRequestDto requestDto) {

		requestDto.setResumeId(resumeId);
		model.addAttribute("resumeDetail", resumeService.getResumeDetail(requestDto));
		return "thymeleaf/resume/resume-form";
	}

    @GetMapping("/template/{resumeId}")
    public String resumeTemplatePage(@PathVariable Long resumeId,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     Model model,
                                     ResumeRequestDto requestDto) {

        requestDto.setResumeId(resumeId);
        model.addAttribute("resumeDetail", resumeService.getResumeDetail(requestDto));
        return "thymeleaf/resume/resume-template";
    }

    @GetMapping({"/preview", "/preview/{resumeId}"})
    public String resumePreviewPage(@PathVariable(required = false) Long resumeId,
                                    @AuthenticationPrincipal UserPrincipal principal,
                                    Model model,
                                    ResumeRequestDto requestDto) {
        ResumeDetailResponseDto resumeDetail = sampleResumeFactory.create();
        if (resumeId != null) {
            requestDto.setResumeId(resumeId);
            resumeDetail = resumeService.getResumeDetail(requestDto);
        }

        TemplateDetailResponseDto resumeTemplate = null;
        if (requestDto.getTemplateId() != null) {
            resumeTemplate = templateService.getTemplateDetail(TemplateRequestDto.builder()
                            .templateId(requestDto.getTemplateId())
                            .userId(principal.getUserId())
                            .isPublic(true)
                            .build());
        }

        model.addAttribute("resumeDetail", resumeDetail);
        model.addAttribute("resumeTemplate", resumeTemplate);
        return "thymeleaf/resume/resume-preview";
    }
}