package resume.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(2)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {

        http.securityMatcher("/web/**")
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