package resume.web.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import resume.core.dto.UserDto;
import resume.core.mapper.UserMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserKeyAuthenticationProvider implements AuthenticationProvider {

    private final UserMapper userMapper;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String userKey = authentication.getName();

        UserDto user = userMapper.findByUserKey(userKey);

        if (user == null) {
            throw new UsernameNotFoundException(
                    "User not found: " + userKey
            );
        }

        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication);
    }
}