package resume.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/web/resume")
public class ResumeWebController {

	@GetMapping("/main")
	public String resumeMainPage(Model model) {
	    return "thymeleaf/resume/main";
	}

	@GetMapping("/regist")
	public String resumeRegistPage(Model model) {
		String str = (String) model.getAttribute("key");
		System.out.println("str ::: "+ str);

		return "thymeleaf/resume/resume-form";
	}
}
