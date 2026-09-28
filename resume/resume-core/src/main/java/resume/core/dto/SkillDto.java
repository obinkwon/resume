package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillDto {

    private Long skillId;
    private Long resumeId;
    private String skillName;
    private String category;
    private int proficiency;
    private int sortOrder;
}
