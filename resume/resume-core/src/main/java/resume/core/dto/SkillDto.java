package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillDto {

    private Long resumeId;
    private String skillName;
    private int sortOrder;
}
