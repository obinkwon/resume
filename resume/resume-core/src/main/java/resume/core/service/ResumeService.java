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
@Transactional
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

    public Long saveResume(ResumeRequestDto requestDto) {
        resumeMapper.insertResume(requestDto);
        Long resumeId = requestDto.getResumeId();

        requestDto.getProfile().setResumeId(resumeId);
        resumeMapper.insertProfile(requestDto.getProfile());

        int order = 0;
        log.info("requestDto.getExperiences() ::: {}",requestDto.getExperiences());
        if (requestDto.getExperiences() != null) {
            for (ExperienceDto expDto : requestDto.getExperiences()) {
                expDto.setResumeId(resumeId);
                expDto.setSortOrder(order++);
                resumeMapper.insertExperience(expDto);
            }
        }

        order = 0;

        if (requestDto.getEducations() != null) {
            for (EducationDto eduDto : requestDto.getEducations()) {
                eduDto.setResumeId(resumeId);
                eduDto.setSortOrder(order++);
                resumeMapper.insertEducation(eduDto);
            }
        }

        order = 0;

        if (requestDto.getProjects() != null) {
            for (ProjectDto pDto : requestDto.getProjects()) {
                pDto.setResumeId(resumeId);
                pDto.setSortOrder(order++);
                resumeMapper.insertProject(pDto);
            }
        }

        order = 0;

        if (requestDto.getSkills() != null) {
            for (SkillDto sDto : requestDto.getSkills()) {
                sDto.setResumeId(resumeId);
                sDto.setSortOrder(order++);
                resumeMapper.insertSkill(sDto);
            }
        }

        return resumeId;
    }
}
