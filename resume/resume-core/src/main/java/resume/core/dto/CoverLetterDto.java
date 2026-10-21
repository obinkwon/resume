package resume.core.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 이력서 자기소개서 항목 (제목 + 내용)
 */
@Getter
@Setter
@NoArgsConstructor
public class CoverLetterDto {

    private Long coverLetterId;
    private Long resumeId;
    private String title;
    private String content;
    private Integer maxLength;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}