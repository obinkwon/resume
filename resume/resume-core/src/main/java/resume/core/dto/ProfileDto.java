package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ProfileDto {
    private Long profileId;
    private Long resumeId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String profileImage;
    private Date birthDate;
    private String introduction;
}
