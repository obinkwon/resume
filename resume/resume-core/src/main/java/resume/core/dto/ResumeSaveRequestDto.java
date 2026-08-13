package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResumeSaveRequestDto {

    private Long resumeId;
    private String title;
    private String name;
    private String email;
    private String phone;
    private String summary;

    private List<ExperienceDto> experiences;
    private List<EducationDto> educations;
    private List<ProjectDto> projects;
    private List<SkillDto> skills;
}
