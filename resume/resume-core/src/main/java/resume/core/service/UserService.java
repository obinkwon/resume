package resume.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import resume.core.dto.UserDto;
import resume.core.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public UserDto getUserId(String userKey) {
        return userMapper.findByUserKey(userKey);
    }
}
