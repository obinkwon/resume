package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateRequestDto {

    private Long resumeId;
    private Long userId;
    private Long templateId;
    private String title;
}
