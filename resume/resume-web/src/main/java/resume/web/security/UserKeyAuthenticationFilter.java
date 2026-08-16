package resume.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class UserKeyAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public UserKeyAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
        setFilterProcessesUrl("/web/login");
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request,
            HttpServletResponse response) {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        System.out.println("HTTP METHOD = " + request.getMethod());
        System.out.println("REQUEST URI = " + request.getRequestURI());
        System.out.println("userKey = [" + request.getParameter("userKey") + "]");

        String userKey = request.getParameter("userKey");

        if (userKey == null || userKey.isBlank()) {
            throw new IllegalArgumentException("userKey is required");
        }

        userKey = userKey.trim();

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        userKey,
                        null
                );

        setDetails(request, authentication);

        return getAuthenticationManager().authenticate(authentication);
    }
}