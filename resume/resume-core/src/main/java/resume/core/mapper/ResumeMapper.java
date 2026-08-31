package resume.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import resume.core.dto.*;

@Mapper
public interface ResumeMapper {

    void insertResume(ResumeSaveRequestDto requestDto);

    void insertProfile(ProfileDto profileDto);

    void insertExperience(ExperienceDto experienceDto);

    void insertEducation(EducationDto educationDto);

    void insertProject(ProjectDto projectDto);

    void insertSkill(SkillDto skillDto);
}
