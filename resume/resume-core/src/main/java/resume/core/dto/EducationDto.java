package resume.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EducationDto {

    private Long resumeId;
    private String school;
    private String major;
    private LocalDate startDate;
    private LocalDate endDate;
    private int sortOrder;
}
