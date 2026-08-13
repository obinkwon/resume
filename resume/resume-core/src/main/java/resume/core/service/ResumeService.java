package resume.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import resume.core.dto.*;
import resume.core.mapper.ResumeMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumeService {

    private final ResumeMapper resumeMapper;

    public Long saveResume(ResumeSaveRequestDto requestDto) {
        resumeMapper.insertResume(requestDto);

        Long resumeId = requestDto.getResumeId();

        int order = 0;

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
