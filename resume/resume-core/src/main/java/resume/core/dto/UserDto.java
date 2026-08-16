package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserDto {

    private Long userId;
    private String userKey;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
