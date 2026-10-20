package resume.core.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfLogDto {

    private Long pdfId;
    private Long resumeId;
    private Long templateId;
    private String filePath;
    private String fileName;
    private LocalDateTime createdAt;
}
