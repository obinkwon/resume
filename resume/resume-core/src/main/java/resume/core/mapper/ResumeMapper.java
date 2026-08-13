package resume.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import resume.core.dto.*;

@Mapper
public interface ResumeMapper {

    void insertResume(ResumeSaveRequestDto requestDto);

    void insertExperience(ExperienceDto expDto);

    void insertEducation(EducationDto eduDto);

    void insertProject(ProjectDto pDto);

    void insertSkill(SkillDto sDto);
}
