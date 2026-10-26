package resume.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TemplateDetailResponseDto {

    private Long templateId;
    private String templateName;
    private String description;
    private String thumbnailUrl;
    private String htmlContent;
    private String cssContent;
    private Boolean isPublic;
    private Long createdBy;
    private String useYn;
}
