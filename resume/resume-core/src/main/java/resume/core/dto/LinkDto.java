package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkDto {

    private Long linkId;
    private Long resumeId;
    private String linkType;
    private String displayName;
    private String url;
    private String useYn;
    private int sortOrder;
}
