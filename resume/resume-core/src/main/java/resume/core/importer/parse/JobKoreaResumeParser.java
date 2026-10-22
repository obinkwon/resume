package resume.core.importer.parse;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 잡코리아 이력서 PDF 전용
 *
 *  잡코리아 PDF 특징 (공통 규칙과 다른 점만 여기서 처리)
 *   1) 맨 위에 요약 카드 (학력 / 경력 / 인턴·대외활동 / 자격증·어학 / 기술 태그) → 버림, 기술 태그만 살림
 *   2) 값 뒤에 라벨이 붙어 나옴 : "010-1234-5678휴대폰 a@b.comEmail", "3,560만원연봉", "SI개발주요직무"
 *   3) 경력기술서가 페이지를 넘어가면 "경력기술서" 제목 + 인적 사항이 다시 나옴 → 프로젝트를 이어서 읽음
 *   4) 첫 줄 = 이력서 제목 (한 줄 소개) → 간단 소개로
 */
@Component
public class JobKoreaResumeParser extends RuleBasedResumeParser implements SiteResumeParser {

    // ---------- 판별 : 잡코리아 PDF 에 들어가는 문구 ----------
    /** 하단 안내문 (거의 바뀌지 않음) */
    private static final List<String> STRONG_MARKERS = List.of(
            "구직자가등록한문서에대해보증하거나",
            "구인/구직목적외다른목적으로이용시이력서삭제혹은비공개",
            "위조된문서를등록하여취업활동에이용시");
    /** 화면 구성에서 나오는 말 */
    private static final List<String> WEAK_MARKERS = List.of(
            "인턴·대외활동/해외경험", "취업우대사항", "희망근무조건", "경력기술서", "주요직무", "최종수정된이력서");

    // ---------- 값 뒤에 붙은 라벨 ----------
    private static final String GLUE_LABELS = "휴대폰|휴대전화|전화번호|이메일|[Ee]-?[Mm]ail|주소|연봉|주요직무";
    private static final Pattern GLUED_LINE = Pattern.compile("^(?:\\S.*?(?<=\\S)(?:" + GLUE_LABELS + ")(?:\\s+|$))+$");
    private static final Pattern GLUED_PAIR = Pattern.compile("(\\S.*?)(?<=\\S)(" + GLUE_LABELS + ")(?=\\s|$)");

    @Override
    public ResumeSource source() {
        return ResumeSource.JOBKOREA;
    }

    /** 하단 안내문 하나 = 3점, 화면 문구 하나 = 1점 */
    @Override
    public int matchScore(String text) {
        String compact = text.replaceAll("\\s+", "");
        int score = 0;
        for (String m : STRONG_MARKERS) if (compact.contains(m)) score += 3;
        for (String m : WEAK_MARKERS) if (compact.contains(m)) score += 1;
        return score;
    }

    /** "3,560만원연봉" → "연봉: 3,560만원", "010-…휴대폰 a@b.comEmail" → 두 줄 */
    @Override
    protected List<String> preprocess(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            if (line.length() <= 100 && GLUED_LINE.matcher(line).matches()) {
                Matcher m = GLUED_PAIR.matcher(line);
                while (m.find()) out.add(m.group(2) + ": " + m.group(1).strip());
            } else {
                out.add(line);
            }
        }
        return out;
    }

    @Override
    protected boolean dropSummaryCards() {
        return true;
    }

    /** 프로젝트(경력기술서)를 읽는 중에 다음 페이지의 "경력기술서" 제목이 나오면 무시 */
    @Override
    protected boolean isRepeatedPageHeading(String line, Section heading, Section current) {
        return current == Section.PROJECT && heading == Section.EXPERIENCE
                && Section.key(line).startsWith("경력기술서");
    }

    @Override
    protected boolean useHeadlineAsIntro() {
        return true;
    }
}
