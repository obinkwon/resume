package resume.core.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TemplateDto {

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
