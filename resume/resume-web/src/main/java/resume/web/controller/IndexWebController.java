package resume.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexWebController {

    @GetMapping("/")
    public String indexPage(Model model) {
        return "redirect:/web/resume/main";
    }
}
