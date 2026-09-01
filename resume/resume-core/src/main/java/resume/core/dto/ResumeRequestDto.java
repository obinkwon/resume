package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResumeRequestDto {

    private Long resumeId;
    private Long userId;
    private Long templateId;
    private String title;

    private ProfileDto profile;
    private List<ExperienceDto> experiences;
    private List<EducationDto> educations;
    private List<ProjectDto> projects;
    private List<SkillDto> skills;
}
