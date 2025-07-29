package resume.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/web/login")
public class LoginWebController {
	
    @GetMapping("")
    public String loginView(final Model model) {

        return "thymeleaf/oauth/login";
    }
}
