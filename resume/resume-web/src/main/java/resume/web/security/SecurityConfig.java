package resume.web.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserKeyAuthenticationProvider authenticationProvider;

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

        UserKeyAuthenticationFilter authenticationFilter = new UserKeyAuthenticationFilter(authenticationManager);

        authenticationFilter.setAuthenticationSuccessHandler(
            new SimpleUrlAuthenticationSuccessHandler("/web/resume/main")
        );
        authenticationFilter.setAuthenticationFailureHandler(
            new SimpleUrlAuthenticationFailureHandler("/web/login?error=true")
        );

        http.securityMatcher("/web/**", "/login", "/error", "/favicon.ico", "/css/**", "/js/**")
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/web/login", "/css/**", "/js/**", "/images/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            )
            // 기본 formLogin 사용하지 않음
            .formLogin(AbstractHttpConfigurer::disable)
            // 로그아웃
            .logout(logout -> logout
                    .logoutUrl("/web/logout")
                    .logoutSuccessUrl("/web/login")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}