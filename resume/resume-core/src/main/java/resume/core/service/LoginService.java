package resume.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import resume.core.dto.UserDto;
import resume.core.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserMapper userMapper;

    @Transactional
    public UserDto login(String userKey) {

        if (userKey == null || userKey.isBlank()) {
            throw new IllegalArgumentException("사용자 ID를 입력해주세요.");
        }

        userKey = userKey.trim();

        if (userKey.length() > 100) {
            throw new IllegalArgumentException("사용자 ID는 100자 이하로 입력해주세요.");
        }

        UserDto user = userMapper.findByUserKey(userKey);

        // 기존 사용자
        if (user != null) {
            return user;
        }

        // 신규 사용자
        UserDto newUser = new UserDto();
        newUser.setUserKey(userKey);

        userMapper.insertUser(newUser);

        return newUser;
    }
}
