package resume.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import resume.core.dto.UserDto;
import resume.core.service.LoginService;
import resume.web.security.UserPrincipal;

@Controller
@RequiredArgsConstructor
public class LoginWebController {

    private final LoginService loginService;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    /**
     * 로그인 페이지
     */
    @GetMapping("/web/login")
    public String loginPage() {
        return "thymeleaf/resume/login";
    }

    /**
     * 로그인 처리
     */
    @PostMapping("/web/login")
    public String loginProc(@RequestParam("userKey") String userKey, HttpServletRequest request, HttpServletResponse response) {

        UserDto user = loginService.login(userKey);

        UserPrincipal principal = new UserPrincipal(user);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 중요
        // SecurityContext를 HTTP Session에 저장
        securityContextRepository.saveContext(
                context,
                request,
                response
        );

        return "redirect:/web/resume/main";
    }

    /**
     * 로그아웃
     */
    @PostMapping("/web/logout")
    public String logout() {
        return "redirect:/web/login";
    }
}
