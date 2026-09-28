package resume.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import resume.core.dto.*;

import java.util.List;

@Mapper
public interface ResumeMapper {

    List<ResumeResponseDto> selectResumeList(ResumeRequestDto requestDto);

    ResumeDetailResponseDto selectResumeDetail(ResumeRequestDto requestDto);

    ProfileDto selectResumeProfile(long resumeId);

    SkillDto selectResumeSkills(long resumeId);

    void insertResume(ResumeRequestDto requestDto);

    void insertProfile(ProfileDto profileDto);

    void insertSkill(SkillDto skillDto);

    void insertEducation(EducationDto educationDto);

    void insertExperience(ExperienceDto experienceDto);

    void insertProject(ProjectDto projectDto);
}
