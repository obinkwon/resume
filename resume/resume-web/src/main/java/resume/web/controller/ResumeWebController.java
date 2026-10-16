package resume.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import resume.core.dto.ResumeDetailResponseDto;
import resume.core.dto.ResumeRequestDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.security.UserPrincipal;
import resume.core.service.ResumeService;
import resume.core.service.TemplateService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/web/resume")
public class ResumeWebController {

	private final ResumeService resumeService;
	private final TemplateService templateService;

	@GetMapping("/list")
	public String resumeListPage(Authentication authentication, Model model, ResumeRequestDto requestDto) {

		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
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
	public String resumeModifyPage(@PathVariable Long resumeId, Model model, ResumeRequestDto requestDto) {

		requestDto.setResumeId(resumeId);
		model.addAttribute("resumeDetail", resumeService.getResumeDetail(requestDto));
		return "thymeleaf/resume/resume-form";
	}

    @GetMapping("/template/{resumeId}")
    public String resumeTemplatePage(@PathVariable Long resumeId, Model model, ResumeRequestDto requestDto) {

        requestDto.setResumeId(resumeId);
        model.addAttribute("resumeDetail", resumeService.getResumeDetail(requestDto));
        return "thymeleaf/resume/resume-template";
    }

    @GetMapping("/preview/{resumeId}")
    public String resumePreviewPage(@PathVariable Long resumeId, Model model, ResumeRequestDto requestDto) {

        requestDto.setResumeId(resumeId);
        ResumeDetailResponseDto resumeDetail = resumeService.getResumeDetail(requestDto);

        TemplateRequestDto templateRequestDto = new TemplateRequestDto();
        if(requestDto.getTemplateId() != null){
            templateRequestDto.setTemplateId(requestDto.getTemplateId());
        } else {
            templateRequestDto.setTemplateId(resumeDetail.getTemplateId());
        }

        model.addAttribute("resumeDetail", resumeDetail);
        model.addAttribute("resumeTemplate", templateService.getTemplateDetail(templateRequestDto));
        return "thymeleaf/resume/resume-preview";
    }
}

