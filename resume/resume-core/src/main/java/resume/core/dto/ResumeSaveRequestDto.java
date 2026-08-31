package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResumeSaveRequestDto {

    private Long resumeId;
    private Long userId;
    private String title;
    private String templateId;

    private ProfileDto profile;
    private List<ExperienceDto> experiences;
    private List<EducationDto> educations;
    private List<ProjectDto> projects;
    private List<SkillDto> skills;
}
