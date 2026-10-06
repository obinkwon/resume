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

        // 기술 추가
        int order = 0;
        if (requestDto.getSkills() != null) {
            for (SkillDto skillDto : requestDto.getSkills()) {
                skillDto.setResumeId(resumeId);
                skillDto.setSortOrder(order++);
                resumeMapper.insertSkill(skillDto);
            }
        }

        // 교육 추가
        order = 0;
        if (requestDto.getEducations() != null) {
            for (EducationDto eduDto : requestDto.getEducations()) {
                eduDto.setResumeId(resumeId);
                eduDto.setSortOrder(order++);
                resumeMapper.insertEducation(eduDto);
            }
        }

        // 경력 추가
        order = 0;
        if (requestDto.getExperiences() != null) {
            for (ExperienceDto expDto : requestDto.getExperiences()) {
                expDto.setResumeId(resumeId);
                expDto.setSortOrder(order++);
                resumeMapper.insertExperience(expDto);
            }
        }

        // 프로젝트 추가
        order = 0;
        if (requestDto.getProjects() != null) {
            for (ProjectDto projDto : requestDto.getProjects()) {
                projDto.setResumeId(resumeId);
                projDto.setSortOrder(order++);
                resumeMapper.insertProject(projDto);
            }
        }

        return resumeId;
    }
}
