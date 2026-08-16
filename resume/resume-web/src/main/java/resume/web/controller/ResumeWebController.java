package resume.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import resume.web.security.UserPrincipal;

@Controller
@RequestMapping("/web/resume")
public class ResumeWebController {

	@GetMapping("/main")
	public String resumeMainPage(Authentication authentication, Model model) {

		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

		model.addAttribute("user", principal);
	    return "thymeleaf/resume/main";
	}

	@GetMapping("/regist")
	public String resumeRegistPage(Model model) {

		return "thymeleaf/resume/resume-form";
	}
}
