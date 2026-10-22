package resume.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import resume.core.enums.ResumeSource;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResumeResponseDto {

    private Long resumeId;
    private Long templateId;
    private String title;
    private String introduction;
    private ResumeSource sourceType;
    private String sourceFileName;
    private LocalDateTime updatedAt;

    private static final ZoneId DB_ZONE = ZoneOffset.UTC;          // DB 저장 기준
    private static final ZoneId VIEW_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /** 한국 시간 */
    private ZonedDateTime updatedAtKst() {
        return updatedAt == null ? null : updatedAt.atZone(DB_ZONE).withZoneSameInstant(VIEW_ZONE);
    }

    /** 카드 표시용 : 방금 전 / n분 전 / n시간 전 / 어제 / n일 전 / yyyy.MM.dd */
    public String getUpdatedAtText() {
        ZonedDateTime t = updatedAtKst();
        if (t == null) return "";
        ZonedDateTime now = ZonedDateTime.now(VIEW_ZONE);
        long min = Duration.between(t, now).toMinutes();
        if (min < 1)  return "방금 전";
        if (min < 60) return min + "분 전";
        long days = ChronoUnit.DAYS.between(t.toLocalDate(), now.toLocalDate());
        if (days == 0) return (min / 60) + "시간 전";
        if (days == 1) return "어제";
        if (days < 7)  return days + "일 전";
        return t.format(DATE);
    }

    /** 마우스 올렸을 때 : 2026.10.22 14:51 */
    public String getUpdatedAtFull() {
        ZonedDateTime t = updatedAtKst();
        return t == null ? "" : t.format(FULL);
    }

    /** <time datetime> 용 ISO */
    public String getUpdatedAtIso() {
        ZonedDateTime t = updatedAtKst();
        return t == null ? "" : t.toOffsetDateTime().toString();
    }
}
