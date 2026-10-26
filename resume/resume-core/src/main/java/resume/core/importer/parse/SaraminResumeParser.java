package resume.core.importer.parse;

import org.springframework.stereotype.Component;
import resume.core.enums.ResumeSource;
import resume.core.enums.Section;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 사람인 이력서 PDF 전용
 *
 *  사람인 PDF 특징 (공통 규칙과 다른 점만 여기서 처리)
 *   1) 섹션 제목이 내용과 떨어져서 나옴 ("경험/활동/교육", "자격/어학/수상" 이 붙어서 먼저 나오고 내용은 나중에)
 *      → 섹션 제목 대신 줄 모양으로 섹션을 정함 (routeLine)
 *        · 학교 + 기간          → 학력       "인하공업전문대학 컴퓨터정보과2014.03 ~ 2019.02"
 *        · 날짜 + 합격/취득     → 자격증     "정보처리산업기사2018.08 최종합격 한국산업인력공단"
 *        · 포트폴리오 파일      → 프로젝트   "포트폴리오_단골의 희열.docx포트폴리오"
 *        · 포트폴리오 다음 문단 → 자기소개서
 *   2) 라벨과 값이 공백으로만 구분됨 : "이메일 a@b.com 휴대폰 010-…", "작업기간 … 작업인원 3명", "작업 툴 …"
 *   3) 학교 이름 뒤 "(2·3년제)" 의 가운뎃점이 칸 나누기와 헷갈림 → 지움
 *   4) 경험/활동/교육 은 이력서 폼에 칸이 없어서 가져오지 않음
 */
@Component
public class SaraminResumeParser extends RuleBasedResumeParser implements SiteResumeParser {

    // ---------- 판별 : 사람인 PDF 화면 구성 문구 (하나에 1점) ----------
    private static final List<String> MARKERS = List.of(
            "나의스킬", "경험/활동/교육", "자격/어학/수상", "포트폴리오/기타문서", "주/야간", "작업기간", "작품소개",
            "학력전공경력희망연봉");

    // ---------- 공백으로만 구분된 라벨 → 'label: 값' ----------
    /** 사람인 라벨 → 공통 규칙이 아는 라벨 */
    private static final Map<String, String> SPACED_LABELS = Map.of(
            "이메일", "이메일", "휴대폰", "휴대폰", "전화번호", "전화번호", "주소", "주소",
            "작업기간", "기간", "작업인원", "작업인원", "작업툴", "사용 기술", "작품소개", "작품소개");
    private static final String SPACED = "이메일|휴대폰|전화번호|주소|작업기간|작업인원|작업\\s?툴|작품\\s?소개";
    private static final Pattern SPACED_START = Pattern.compile("^(?:" + SPACED + ")\\s+\\S.*");
    private static final Pattern SPACED_PAIR = Pattern.compile(
            "(" + SPACED + ")\\s+(.+?)(?=\\s+(?:" + SPACED + ")\\s|$)");

    /** "(2·3년제)", "(4년제)", "(2,3년)" */
    private static final Pattern SCHOOL_YEARS = Pattern.compile("\\(\\s*\\d(?:\\s*[·,]\\s*\\d)?\\s*년제?\\s*\\)");
    /** "포트폴리오_단골의 희열.docx포트폴리오" → 파일 이름이 프로젝트 이름 */
    private static final Pattern PORTFOLIO_FILE = Pattern.compile(
            "^포트폴리오_(.+?)\\.(?:docx?|pdf|pptx?|hwpx?|zip|xlsx?|jpe?g|png)\\s*(?:포트폴리오|기타문서|기타|경력기술서|기획서)$",
            Pattern.CASE_INSENSITIVE);

    // ---------- 줄 모양 ----------
    /** 자격증 줄 : 날짜 + 합격/취득 */
    private static final Pattern CERT_ROW = Pattern.compile(".*(?:19|20)\\d{2}\\.\\s?\\d{1,2}\\.?\\s*(?:최종합격|필기합격|실기합격|합격|취득|등록|발급).*");
    /** 학력 상세 줄 : "지역 인천 학점 3.7/4.5 주/야간 주간" */
    private static final Pattern EDU_DETAIL = Pattern.compile(".*(?:학점|주/야간|지역|부전공|복수전공|이수학점).*");
    /** 요약 표 머리 : "학력 전공 경력 희망연봉 포트폴리오" */
    private static final Set<String> SUMMARY_WORDS = Set.of(
            "학력", "전공", "경력", "희망연봉", "포트폴리오", "스킬", "자격증", "어학", "희망근무지", "신입");
    /** 포트폴리오(프로젝트) 상세 라벨 */
    private static final Set<String> PROJECT_LABELS = Set.of("프로젝트명", "기간", "작업인원", "사용기술", "작품소개");

    @Override
    public ResumeSource source() {
        return ResumeSource.SARAMIN;
    }

    @Override
    public int matchScore(String text) {
        String compact = text.replaceAll("\\s+", "");
        int score = 0;
        for (String m : MARKERS) if (compact.contains(m)) score++;
        return score;
    }

    @Override
    protected List<String> preprocess(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            String l = SCHOOL_YEARS.matcher(line).replaceAll("").replaceAll("\\s{2,}", " ").strip();

            Matcher pf = PORTFOLIO_FILE.matcher(l);
            if (pf.matches()) {
                out.add("프로젝트 명: " + pf.group(1).strip());
                continue;
            }
            if (SPACED_START.matcher(l).matches()) {
                // "이메일 a@b.com 휴대폰 010-…" → "이메일: a@b.com" / "휴대폰: 010-…"
                Matcher m = SPACED_PAIR.matcher(l);
                while (m.find()) {
                    String label = SPACED_LABELS.getOrDefault(m.group(1).replaceAll("\\s", ""), m.group(1));
                    out.add(label + ": " + m.group(2).strip());
                }
                continue;
            }
            out.add(l);
        }
        return out;
    }

    /** 섹션 제목이 믿을 수 없어서 줄 모양으로 섹션을 정함 */
    @Override
    protected Section routeLine(String line, Section current, List<String> currentLines) {
        if (isSummaryHeader(line)) return Section.OTHER;
        if (SCHOOL.matcher(line).matches() && Period.find(line, false) != null) return Section.EDUCATION;
        if (CERT_ROW.matcher(line).matches()) return Section.CERTIFICATE;

        switch (current) {
            case EDUCATION -> {
                // 학교 줄 · 졸업 상태 · 학점 줄이 아니면 학력 끝 (다음은 경험/활동/교육)
                boolean status = line.length() <= 6 && EDU_STATUS.matcher(line).matches();
                if (!status && !EDU_DETAIL.matcher(line).matches()) return Section.OTHER;
            }
            case CERTIFICATE -> {
                return Section.OTHER;                     // 자격증 줄이 아니면 자격증 끝
            }
            case PROJECT -> {
                // 포트폴리오 상세가 끝나고 문단이 바뀌면 자기소개서 (사람인은 포트폴리오 다음에 자기소개서)
                if (!isProjectLabel(line) && prevEndsParagraph(currentLines)) return Section.COVER;
            }
            default -> {
            }
        }
        return null;
    }

    private static boolean isSummaryHeader(String line) {
        String[] words = line.split("\\s+");
        if (words.length < 3) return false;
        for (String w : words) if (!SUMMARY_WORDS.contains(w)) return false;
        return true;
    }

    private static boolean isProjectLabel(String line) {
        Matcher m = LABEL_INLINE.matcher(line);
        return m.matches() && PROJECT_LABELS.contains(labelKeyOf(m.group(1)));
    }

    private static boolean prevEndsParagraph(List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            String l = lines.get(i);
            if (l.isEmpty()) continue;
            return PARAGRAPH_END.matcher(l).matches();
        }
        return false;
    }
}
