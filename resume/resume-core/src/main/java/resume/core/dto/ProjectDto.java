package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProjectDto {

    private Long projectId;
    private Long resumeId;
    private String projectName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String role;
    private String description;
    private String techStack;
    private String demoUrl;
    private int sortOrder;
}
