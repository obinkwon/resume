package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectDto {

    private Long resumeId;
    private String projectName;
    private String description;
    private String techStack;
    private int sortOrder;
}
