package resume.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ResumeWebController {
	
	@GetMapping("/api/test")
	public String apiTest(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("key", "2");
		redirectAttributes.addAttribute("key", "1");
		
		return "redirect:/success";
    }
	
	@GetMapping("/success")
	public String successPage(Model model) {
		String str = (String) model.getAttribute("key");
		System.out.println("str ::: "+ str);
		
	    return "thymeleaf/success";
	}
    
    @GetMapping("/web/main")
    public String mainView(final Model model) {
    	
    	return "thymeleaf/main";
    }
}
