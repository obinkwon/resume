package resume.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import resume.core.dto.*;

import java.util.List;

@Mapper
public interface ResumeMapper {
    // 이력서 목록 조회
    List<ResumeResponseDto> selectResumeList(ResumeRequestDto requestDto);
    // 이력서 상세 조회
    ResumeDetailResponseDto selectResumeDetail(ResumeRequestDto requestDto);
    // 이력서 프로필 상세 조회
    ProfileDto selectResumeProfile(long resumeId);
    // 이력서 기술 목록 조회
    List<SkillDto> selectResumeSkills(long resumeId);
    // 이력서 학력 목록 조회
    List<EducationDto> selectResumeEducations(long resumeId);
    // 이력서 경력 목록 조회
    List<ExperienceDto> selectResumeExperiences(long resumeId);
    // 이력서 프로젝트 목록 조회
    List<ProjectDto> selectResumeProjects(long resumeId);
    // 이력서 외부링크 목록 조회
    List<LinkDto> selectResumeLinks(long resumeId);
    // 이력서 자격증 목록 조회
    List<CertificateDto> selectResumeCertificates(long resumeId);

    // 이력서 추가
    void insertResume(ResumeRequestDto requestDto);
    // 이력서 프로필 추가
    void insertProfile(ProfileDto profileDto);
    // 이력서 기술 추가
    void insertSkill(SkillDto skillDto);
    // 이력서 학력 추가
    void insertEducation(EducationDto educationDto);
    // 이력서 경력 추가
    void insertExperience(ExperienceDto experienceDto);
    // 이력서 프로젝트 추가
    void insertProject(ProjectDto projectDto);
    // 이력서 외부링크 추가
    void insertLink(LinkDto linkDto);
    // 이력서 자격증 추가
    void insertCertificate(CertificateDto certificateDto);

    // 이력서 수정
    void updateResume(ResumeRequestDto requestDto);
    // 이력서 프로필 수정
    void updateProfile(ProfileDto profileDto);
    // 이력서 기술 수정
    void updateSkill(SkillDto skillDto);
    // 이력서 학력 수정
    void updateEducation(EducationDto educationDto);
    // 이력서 경력 수정
    void updateExperience(ExperienceDto experienceDto);
    // 이력서 프로젝트 수정
    void updateProject(ProjectDto projectDto);
    // 이력서 외부링크 수정
    void updateLink(LinkDto linkDto);
    // 이력서 자격증 수정
    void updateCertificate(CertificateDto certificateDto);
}
