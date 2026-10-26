package resume.core.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRequestDto {

    private Long resumeId;
    private Long userId;
    private Long templateId;
    private String title;
    private Boolean isPublic;
}
