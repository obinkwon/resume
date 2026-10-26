package resume.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TemplateResponseDto {

    private Long resumeId;
    private Long templateId;
    private String templateName;
    private String description;
    private String thumbnailUrl;
    private String layoutType;
    private String primaryColor;
    private Boolean hasBanner;
    private Boolean isPublic;
    private Long createdBy;
}
