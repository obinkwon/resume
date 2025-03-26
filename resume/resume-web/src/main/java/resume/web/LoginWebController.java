package resume.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginWebController {
	
    @GetMapping("/web/login")
    public String loginView(final Model model) {

        return "thymeleaf/login";
    }
    
    @GetMapping("/web/main")
    public String mainView(final Model model) {
    	
    	return "thymeleaf/main";
    }
}
