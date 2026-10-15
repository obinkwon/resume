package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CertificateDto {

    private Long certificateId;
    private Long resumeId;
    private String certificateName;
    private String issuer;
    private LocalDate issuedDate;
    private String description;
    private String useYn;
    private int sortOrder;
}
