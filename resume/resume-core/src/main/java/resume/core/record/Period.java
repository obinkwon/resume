package resume.core.record;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 한 줄 안의 기간 / 날짜
 *  - 2022.01 ~ 2024.03 · 2022-01-15 - 현재 · 2022년 1월 ~ 재직중 · 2016 ~ 2020 · 2022.01 ~
 *  - 날짜 하나 : 2020.11 · 2020년 11월 (자격증 취득일, 졸업일)
 *
 * @param start   시작일 'yyyy-MM-dd' (없으면 null)
 * @param end     종료일 'yyyy-MM-dd' (진행 중이거나 없으면 null)
 * @param current 현재 / 재직 중 / 진행 중
 * @param from    줄 안에서 기간이 시작하는 위치
 * @param to      줄 안에서 기간이 끝나는 위치
 */
public record Period(String start, String end, boolean current, int from, int to) {

    /** 연도 + (월) + (일). 연도 앞뒤가 다른 숫자에 붙어 있으면 날짜로 보지 않음 */
    private static final String DATE =
            "(?<!\\d)(?:19|20)\\d{2}(?!\\d)"
          + "(?:\\s*(?:[./\\-]|년)\\s*(?:0?[1-9]|1[0-2])(?!\\d)\\s*월?"
          + "(?:\\s*[./\\-]\\s*(?:0?[1-9]|[12]\\d|3[01])(?!\\d)|\\s*(?:0?[1-9]|[12]\\d|3[01])\\s*일)?)?\\.?";

    /** 월까지 있는 날짜 (날짜 하나만 있을 때는 연도만으로 판단하지 않음) */
    private static final String DATE_WITH_MONTH =
            "(?<!\\d)(?:19|20)\\d{2}\\s*(?:[./\\-]|년)\\s*(?:0?[1-9]|1[0-2])(?!\\d)\\s*월?"
          + "(?:\\s*[./\\-]\\s*(?:0?[1-9]|[12]\\d|3[01])(?!\\d)|\\s*(?:0?[1-9]|[12]\\d|3[01])\\s*일)?\\.?";

    private static final String CURRENT_WORDS =
            "현재|재직\\s*중|진행\\s*중|재학\\s*중|근무\\s*중|present|now|current";

    private static final Pattern RANGE = Pattern.compile(
            "(" + DATE + ")\\s*(?:(?:[~∼〜\\-–—]|부터)\\s*(" + DATE + "|" + CURRENT_WORDS + ")"
          + "|[~∼〜](?=\\s*(?:$|[|)\\]])))",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SINGLE = Pattern.compile(DATE_WITH_MONTH);
    private static final Pattern YEAR_ONLY = Pattern.compile("(?:19|20)\\d{2}");

    private static final Pattern PARTS = Pattern.compile(
            "((?:19|20)\\d{2})(?:\\s*(?:[./\\-]|년)\\s*(\\d{1,2})(?:\\s*월)?(?:\\s*(?:[./\\-]\\s*)?(\\d{1,2}))?)?");

    private static final Pattern CURRENT = Pattern.compile("^(?:" + CURRENT_WORDS + ")$", Pattern.CASE_INSENSITIVE);

    /** 기간(~ 로 이어진 두 날짜)을 찾음. allowSingle 이면 날짜 하나도 인정 */
    public static Period find(String line, boolean allowSingle) {
        Matcher m = RANGE.matcher(line);
        if (m.find()) {
            String endRaw = m.group(2);
            boolean current = endRaw != null && CURRENT.matcher(endRaw.trim()).matches();
            String end = endRaw == null || current ? null : toIso(endRaw);
            String start = toIso(m.group(1));
            if (start != null) return new Period(start, end, current, m.start(), m.end());
        }
        if (allowSingle && YEAR_ONLY.matcher(line.strip()).matches()) {
            // 연도만 있는 줄 : "2014" (잡코리아 고등학교 졸업년도)
            return new Period(line.strip() + "-01-01", null, false, 0, line.length());
        }
        if (allowSingle) {
            Matcher s = SINGLE.matcher(line);
            if (s.find()) {
                String date = toIso(s.group());
                if (date != null) return new Period(date, null, false, s.start(), s.end());
            }
        }
        return null;
    }

    /** '2022.1' → '2022-01-01', '2022년 3월 2일' → '2022-03-02' */
    static String toIso(String raw) {
        if (raw == null) return null;
        Matcher m = PARTS.matcher(raw);
        if (!m.find()) return null;
        int year = Integer.parseInt(m.group(1));
        int month = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
        int day = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));
        if (month < 1 || month > 12) return null;
        if (day < 1 || day > 31) day = 1;
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    /** 줄에서 기간 부분을 지운 나머지 (기간 자리는 칸 구분 " | " 으로) */
    public String removeFrom(String line) {
        return line.substring(0, from).stripTrailing() + " | " + line.substring(to).stripLeading();
    }
}
