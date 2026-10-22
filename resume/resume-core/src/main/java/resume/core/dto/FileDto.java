package resume.core.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
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
