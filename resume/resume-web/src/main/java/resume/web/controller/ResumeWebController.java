package resume.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import resume.core.dto.ResumeRequestDto;
import resume.core.security.UserPrincipal;
import resume.core.service.ResumeService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/web/resume")
public class ResumeWebController {

	private final ResumeService resumeService;

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
	public String resumeModifyPage(Model model, ResumeRequestDto requestDto, @PathVariable Long resumeId) {

		requestDto.setResumeId(resumeId);
		model.addAttribute("resumeDetail", resumeService.getResumeDetail(requestDto));
		return "thymeleaf/resume/resume-form";
	}
}

