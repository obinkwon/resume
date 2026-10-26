package resume.core.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 만들어 둔 템플릿을 이력서에 연결 (POST /api/template/apply)
 */
@Getter
@Setter
@NoArgsConstructor
public class TemplateApplyRequestDto {

    private Long resumeId;
    private Long templateId;
}