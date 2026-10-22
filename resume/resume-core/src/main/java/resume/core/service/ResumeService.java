package resume.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import resume.core.dto.*;
import resume.core.importer.ResumeFileType;
import resume.core.importer.ResumeFileTypeDetector;
import resume.core.importer.ResumeImportException;
import resume.core.importer.TextNormalizer;
import resume.core.importer.extract.ResumeTextExtractor;
import resume.core.importer.parse.ResumeParser;
import resume.core.mapper.PdfLogMapper;
import resume.core.mapper.ResumeMapper;
import resume.core.mapper.TemplateMapper;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {
    /** 업로드 최대 크기 (application.yml 의 multipart 제한은 이보다 조금 크게) */
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final ResumeMapper resumeMapper;
    private final PdfLogMapper pdfLogMapper;
    private final TemplateMapper templateMapper;

    private final List<ResumeTextExtractor> extractors;
    private final ResumeParser parser;

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

        // 이력서: 기존에 있으면 수정 / 없으면 등록 (resume_id 유지)
        if (resumeId != null) {
            resumeMapper.updateResume(requestDto);

            // 기존 하위 항목 전부 삭제 처리 (use_yn = 'N')
            ProfileDto profileCond = new ProfileDto();
            profileCond.setResumeId(resumeId);
            resumeMapper.deleteProfile(profileCond);

            CoverLetterDto coverCond = new CoverLetterDto();
            coverCond.setResumeId(resumeId);
            resumeMapper.deleteCoverLetter(coverCond);

            SkillDto skillCond = new SkillDto();
            skillCond.setResumeId(resumeId);
            resumeMapper.deleteSkill(skillCond);

            EducationDto eduCond = new EducationDto();
            eduCond.setResumeId(resumeId);
            resumeMapper.deleteEducation(eduCond);

            ExperienceDto expCond = new ExperienceDto();
            expCond.setResumeId(resumeId);
            resumeMapper.deleteExperience(expCond);

            ProjectDto projCond = new ProjectDto();
            projCond.setResumeId(resumeId);
            resumeMapper.deleteProject(projCond);

            LinkDto linkCond = new LinkDto();
            linkCond.setResumeId(resumeId);
            resumeMapper.deleteLink(linkCond);

            CertificateDto certCond = new CertificateDto();
            certCond.setResumeId(resumeId);
            resumeMapper.deleteCertificate(certCond);
        } else {
            resumeMapper.insertResume(requestDto);
            resumeId = requestDto.getResumeId();
        }

        // 프로필 추가
        if (requestDto.getProfile() != null) {
            requestDto.getProfile().setResumeId(resumeId);
            resumeMapper.insertProfile(requestDto.getProfile());
        }

        // 기술 추가
        int order = 0;
        if (requestDto.getSkills() != null) {
            for (SkillDto skillDto : requestDto.getSkills()) {
                skillDto.setResumeId(resumeId);
                skillDto.setSortOrder(order++);
                resumeMapper.insertSkill(skillDto);
            }
        }

        // 학력 추가
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

        // 외부링크 추가
        order = 0;
        if (requestDto.getLinks() != null) {
            for (LinkDto linkDto : requestDto.getLinks()) {
                linkDto.setResumeId(resumeId);
                linkDto.setSortOrder(order++);
                resumeMapper.insertLink(linkDto);
            }
        }

        // 자격증 추가
        order = 0;
        if (requestDto.getCertificates() != null) {
            for (CertificateDto certDto : requestDto.getCertificates()) {
                certDto.setResumeId(resumeId);
                certDto.setSortOrder(order++);
                resumeMapper.insertCertificate(certDto);
            }
        }

        // 자기소개서 추가
        order = 0;
        if (requestDto.getCoverLetters() != null) {
            for (CoverLetterDto coverLetterDto : requestDto.getCoverLetters()) {
                coverLetterDto.setResumeId(resumeId);
                coverLetterDto.setSortOrder(order++);
                resumeMapper.insertCoverLetter(coverLetterDto);
            }
        }

        return resumeId;
    }

    @Transactional
    public Long saveResumeTemplate(ResumeRequestDto requestDto) {
        // CSS 를 고쳤으면 원본 템플릿을 복사한 새 템플릿을 만들고, 이력서는 새 템플릿에 연결
        if (Boolean.TRUE.equals(requestDto.getCustomize())) {
            TemplateDetailResponseDto responseDto = templateMapper.selectTemplateDetail(TemplateRequestDto.builder().templateId(requestDto.getTemplateId()).build());
            if (responseDto == null) {
                throw new IllegalArgumentException("원본 템플릿이 없습니다. templateId=" + requestDto.getTemplateId());
            }

            // 이름 : 화면에서 보낸 값, 없으면 '원본이름 (수정)'
            String name = StringUtils.hasText(requestDto.getTemplateName()) ? requestDto.getTemplateName() : responseDto.getTemplateName() + " (수정)";
            if (name.length() > 100) name = name.substring(0, 100);

            TemplateDto templateDto = TemplateDto.builder()
                    .templateName(name)
                    .description(responseDto.getTemplateName() + " 템플릿을 수정한 템플릿")
                    .htmlContent(responseDto.getHtmlContent())
                    .cssContent(requestDto.getCssContent())
                    .isPublic(false)
                    .build();
            templateMapper.insertTemplate(templateDto);
            requestDto.setTemplateId(templateDto.getTemplateId());
        }

        resumeMapper.updateResume(requestDto);
        return requestDto.getResumeId();
    }

    @Transactional
    public void removeResume(ResumeRequestDto requestDto) {
        // 이력서 키 확인
        Long resumeId = requestDto.getResumeId();
        if (requestDto.getResumeId() != null) {
            resumeMapper.deleteResume(requestDto);

            // 기존 하위 항목 전부 삭제 처리 (use_yn = 'N')
            ProfileDto profileCond = new ProfileDto();
            profileCond.setResumeId(resumeId);
            resumeMapper.deleteProfile(profileCond);

            CoverLetterDto coverCond = new CoverLetterDto();
            coverCond.setResumeId(resumeId);
            resumeMapper.deleteCoverLetter(coverCond);

            SkillDto skillCond = new SkillDto();
            skillCond.setResumeId(resumeId);
            resumeMapper.deleteSkill(skillCond);

            EducationDto eduCond = new EducationDto();
            eduCond.setResumeId(resumeId);
            resumeMapper.deleteEducation(eduCond);

            ExperienceDto expCond = new ExperienceDto();
            expCond.setResumeId(resumeId);
            resumeMapper.deleteExperience(expCond);

            ProjectDto projCond = new ProjectDto();
            projCond.setResumeId(resumeId);
            resumeMapper.deleteProject(projCond);

            LinkDto linkCond = new LinkDto();
            linkCond.setResumeId(resumeId);
            resumeMapper.deleteLink(linkCond);

            CertificateDto certCond = new CertificateDto();
            certCond.setResumeId(resumeId);
            resumeMapper.deleteCertificate(certCond);
        }
    }

    @Transactional
    public void savePdfLog(PdfLogDto pdfLogDto) {
        // 로그 저장 - 실패해도 다운로드는 진행
        try {
            pdfLogMapper.insertPdfLog(pdfLogDto);
        } catch (Exception e) {
            log.warn("PDF 로그 저장 실패 resumeId={}, templateId={}", pdfLogDto.getResumeId(), pdfLogDto.getTemplateId(), e);
        }
    }

    public ResumeImportResultDto importFile(MultipartFile file) {
        ExtractedText ex = extractText(file, true);
        return parser.parse(ex.text(), ex.type());
    }

    /** 디버그용 : 꺼낸 글자 + 섹션 + 최종 결과 */
    public ResumeImportDebugDto debugImportFile(MultipartFile file) {
        ExtractedText ex = extractText(file, false);
        return new ResumeImportDebugDto(ex.type().getExtension(), ex.text().length(), ex.text(),
                parser.debugSections(ex.text()), parser.parse(ex.text(), ex.type()));
    }

    private record ExtractedText(ResumeFileType type, String text) {
    }

    private ExtractedText extractText(MultipartFile file, boolean requireReadable) {
        if (file == null || file.isEmpty()) {
            throw new ResumeImportException(ResumeImportException.BAD_REQUEST, "파일을 선택해 주세요.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResumeImportException(ResumeImportException.TOO_LARGE, "파일은 10MB까지 올릴 수 있어요.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResumeImportException(ResumeImportException.BAD_REQUEST, "파일을 읽지 못했어요. 다시 시도해 주세요.", e);
        }

        ResumeFileType type = ResumeFileTypeDetector.detect(file.getOriginalFilename(), bytes);
        ResumeTextExtractor extractor = extractors.stream()
                .filter(e -> e.supports() == type)
                .findFirst()
                .orElseThrow(() -> new ResumeImportException(ResumeImportException.UNSUPPORTED, "지원하지 않는 파일 형식이에요."));

        try {
            String text = TextNormalizer.normalize(extractor.extract(bytes));
            // 디버그(requireReadable=false) 때는 글자가 깨진 파일도 내용을 볼 수 있게 검사하지 않음
            if (requireReadable) TextNormalizer.requireReadable(text);
            return new ExtractedText(type, text);
        } catch (ResumeImportException e) {
            // 개인정보가 담긴 파일이라 내용은 로그에 남기지 않음
            log.info("이력서 파일 가져오기 실패 type={} size={} status={} reason={}",
                    type, bytes.length, e.getStatus(), e.getCause() != null ? e.getCause().toString() : e.getMessage());
            throw e;
        }
    }
}