package resume.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import resume.core.dto.UserDto;

@Mapper
public interface UserMapper {

    UserDto findByUserKey(String userKey);

    int insertUser(UserDto user);
}