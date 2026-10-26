package resume.core.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 템플릿 편집기 저장 요청 (POST /api/template/save)
 *  - baseTemplateId 가 내 템플릿이고 saveAsNew 가 아니면 덮어쓰기
 *  - 그 외(기본 템플릿, 남의 공개 템플릿, 새로 만들기)는 새 템플릿으로 추가
 *  - resumeId 가 있으면 저장 후 그 이력서에 템플릿을 연결, 없으면 템플릿만 저장
 */
@Getter
@Setter
@NoArgsConstructor
public class TemplateSaveRequestDto {

    private Long resumeId;
    private Long baseTemplateId;
    private Boolean saveAsNew;

    private String templateName;
    private String description;
    private String htmlContent;
    private String cssContent;
}