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
    private Long profileImageId;
    private String profileImageUrl;
    private Date birthDate;
    private String introduction;
    private String useYn;

    // URL 우선, 없으면 업로드 파일, 둘 다 없으면 null
    public String getProfileImageSrc() {
        if (profileImageUrl != null && !profileImageUrl.isBlank()) return profileImageUrl;
        if (profileImageId != null) return "/api/files/" + profileImageId;
        return null;
    }
}