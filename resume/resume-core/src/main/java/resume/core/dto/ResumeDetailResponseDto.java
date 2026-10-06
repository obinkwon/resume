package resume.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResumeDetailResponseDto {

    private Long resumeId;
    private Long templateId;
    private String title;

    private ProfileDto profile;
    private List<SkillDto> skills;
    private List<EducationDto> educations;
    private List<ExperienceDto> experiences;
    private List<ProjectDto> projects;
}
