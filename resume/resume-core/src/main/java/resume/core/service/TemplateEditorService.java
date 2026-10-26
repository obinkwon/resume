package resume.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import resume.core.dto.TemplateDto;
import resume.core.dto.TemplateSaveRequestDto;
import resume.core.mapper.TemplateEditorMapper;
import resume.core.template.TemplateValidator;

import java.util.Objects;

/**
 * 템플릿 직접 만들기
 *  - 이력서 없이 템플릿만 만들어 두고, 나중에 이력서에 연결할 수 있다.
 *  - 기본(공개) 템플릿은 고칠 수 없고, 편집해서 저장하면 내 템플릿으로 새로 추가된다.
 */
@Service
@RequiredArgsConstructor
public class TemplateEditorService {

    private final TemplateEditorMapper templateEditorMapper;
    private final TemplateValidator templateValidator;

    /** 볼 수 있는 템플릿 (공개 템플릿 또는 내 템플릿) */
    @Transactional(readOnly = true)
    public TemplateDto findViewable(Long templateId, String loginId) {
        TemplateDto t = templateEditorMapper.findTemplate(templateId);
        if (t == null || !(isPublic(t) || isOwner(t, loginId))) {
            throw new IllegalArgumentException("템플릿을 찾을 수 없습니다.");
        }
        return t;
    }

    public boolean isOwner(TemplateDto t, String loginId) {
        return t != null && loginId != null && Objects.equals(t.getCreatedBy(), loginId) && !isPublic(t);
    }

    /** 내 이력서가 아니면 예외 */
    @Transactional(readOnly = true)
    public void checkMyResume(Long resumeId, String loginId) {
        if (resumeId == null || templateEditorMapper.countMyResume(resumeId, loginId) == 0) {
            throw new IllegalArgumentException("이력서를 찾을 수 없습니다.");
        }
    }

    /**
     * 저장 후 템플릿 번호를 돌려준다.
     *  - 내 템플릿을 편집 중이고 saveAsNew 가 아니면 덮어쓰기
     *  - 그 외에는 새 템플릿으로 추가
     *  - resumeId 가 있으면 그 이력서에 연결
     */
    @Transactional
    public Long save(TemplateSaveRequestDto req, String loginId) {
        templateValidator.validateOrThrow(req.getTemplateName(), req.getHtmlContent(), req.getCssContent());
        if (req.getResumeId() != null) {
            checkMyResume(req.getResumeId(), loginId);
        }

        TemplateDto base = req.getBaseTemplateId() == null ? null : templateEditorMapper.findTemplate(req.getBaseTemplateId());
        boolean overwrite = !Boolean.TRUE.equals(req.getSaveAsNew()) && isOwner(base, loginId);

        TemplateDto t = TemplateDto.builder()
                .templateName(req.getTemplateName().trim())
                .description(StringUtils.hasText(req.getDescription()) ? req.getDescription().trim() : "직접 만든 템플릿")
                .htmlContent(nullToEmpty(req.getHtmlContent()))
                .cssContent(nullToEmpty(req.getCssContent()))
                .isPublic(false)
                .createdBy(req.getResumeId())
                .useYn("Y")
                .build();

        Long templateId;
        if (overwrite) {
            t.setTemplateId(base.getTemplateId());
            if (templateEditorMapper.updateTemplate(t) == 0) {
                throw new IllegalStateException("템플릿을 저장하지 못했습니다.");
            }
            templateId = base.getTemplateId();
        } else {
            templateEditorMapper.insertTemplate(t);
            templateId = t.getTemplateId();
        }

        if (req.getResumeId() != null) {
            templateEditorMapper.updateResumeTemplate(req.getResumeId(), templateId);
        }
        return templateId;
    }

    private static boolean isPublic(TemplateDto t) {
        return Boolean.TRUE.equals(t.getIsPublic());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}