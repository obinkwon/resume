package resume.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import resume.core.dto.*;
import resume.core.mapper.ResumeMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeMapper resumeMapper;

    public List<ResumeResponseDto> getResumeList(ResumeRequestDto requestDto) {
        return resumeMapper.selectResumeList(requestDto);
    }

    public ResumeDetailResponseDto getResumeDetail(ResumeRequestDto requestDto) {
        ResumeDetailResponseDto resumeDetail = resumeMapper.selectResumeDetail(requestDto);

        if (resumeDetail != null && resumeDetail.getProfile() == null) {
            resumeDetail.setProfile(new ProfileDto());
        }

        return resumeDetail;
    }

    @Transactional
    public Long saveResume(ResumeRequestDto requestDto) {
        Long resumeId = requestDto.getResumeId();
        // 기존에 있으면 수정 / 없으면 등록
        if(requestDto.getResumeId() != null){
            resumeMapper.updateResume(requestDto);
        } else {
            resumeMapper.insertResume(requestDto);
            resumeId = requestDto.getResumeId();
        }

        requestDto.getProfile().setResumeId(resumeId);
        // 기존에 있으면 수정 / 없으면 등록
        if(requestDto.getProfile().getProfileId() != null){
            resumeMapper.updateProfile(requestDto.getProfile());
        } else {
            resumeMapper.insertProfile(requestDto.getProfile());
        }

        // 기술 추가
        int order = 0;
        if (requestDto.getSkills() != null) {
            for (SkillDto skillDto : requestDto.getSkills()) {
                skillDto.setResumeId(resumeId);
                skillDto.setSortOrder(order++);
                // 기존에 있으면 수정 / 없으면 등록
                if(skillDto.getSkillId() != null){
                    resumeMapper.updateSkill(skillDto);
                } else {
                    resumeMapper.insertSkill(skillDto);
                }
            }
        }

        // 교육 추가
        order = 0;
        if (requestDto.getEducations() != null) {
            for (EducationDto eduDto : requestDto.getEducations()) {
                eduDto.setResumeId(resumeId);
                eduDto.setSortOrder(order++);
                // 기존에 있으면 수정 / 없으면 등록
                if(eduDto.getEducationId() != null){
                    resumeMapper.updateEducation(eduDto);
                } else {
                    resumeMapper.insertEducation(eduDto);
                }
            }
        }

        // 경력 추가
        order = 0;
        if (requestDto.getExperiences() != null) {
            for (ExperienceDto expDto : requestDto.getExperiences()) {
                expDto.setResumeId(resumeId);
                expDto.setSortOrder(order++);
                // 기존에 있으면 수정 / 없으면 등록
                if(expDto.getExperienceId() != null){
                    resumeMapper.updateExperience(expDto);
                } else {
                    resumeMapper.insertExperience(expDto);
                }
            }
        }

        // 프로젝트 추가
        order = 0;
        if (requestDto.getProjects() != null) {
            for (ProjectDto projDto : requestDto.getProjects()) {
                projDto.setResumeId(resumeId);
                projDto.setSortOrder(order++);
                // 기존에 있으면 수정 / 없으면 등록
                if(projDto.getProjectId() != null){
                    resumeMapper.updateProject(projDto);
                } else {
                    resumeMapper.insertProject(projDto);
                }
            }
        }

        return resumeId;
    }
}
