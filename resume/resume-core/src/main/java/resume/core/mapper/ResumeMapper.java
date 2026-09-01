package resume.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import resume.core.dto.*;

import java.util.List;

@Mapper
public interface ResumeMapper {

    List<ResumeResponseDto> selectResumeList(ResumeRequestDto requestDto);

    ResumeDetailResponseDto selectResumeDetail(ResumeRequestDto requestDto);

    void insertResume(ResumeRequestDto requestDto);

    void insertProfile(ProfileDto profileDto);

    void insertExperience(ExperienceDto experienceDto);

    void insertEducation(EducationDto educationDto);

    void insertProject(ProjectDto projectDto);

    void insertSkill(SkillDto skillDto);
}
