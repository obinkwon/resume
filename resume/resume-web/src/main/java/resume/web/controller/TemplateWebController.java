package resume.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import resume.core.dto.ResumeRequestDto;

@Controller
@RequestMapping("/web/template")
@RequiredArgsConstructor
public class TemplateWebController {

	@GetMapping("/regist")
	public String resumeRegistPage(Authentication authentication, Model model, ResumeRequestDto requestDto) {

		return "thymeleaf/resume/template-form";
	}
}

