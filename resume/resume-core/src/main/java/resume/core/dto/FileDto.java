package resume.core.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDto {

    private Long fileId;
    private Long userId;
    private String originalName;
    private String storedName;
    private String filePath;
    private String contentType;
    private String imageUrl;
    private Long fileSize;
}
