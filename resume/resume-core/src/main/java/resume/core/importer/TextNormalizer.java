package resume.core.importer;

import resume.core.exception.ResumeImportException;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * 추출한 글자 정리 + 읽을 수 있는 글자인지 검사
 */
public final class TextNormalizer {

    private static final Pattern INVISIBLE = Pattern.compile("[\\u200B-\\u200D\\u2060\\uFEFF\\u00AD]");
    /** PDF 아이콘·글머리표가 사용자 영역 문자로 나오는 경우 */
    private static final Pattern PRIVATE_USE = Pattern.compile("[\\uE000-\\uF8FF]");
    private static final Pattern SPACES = Pattern.compile("[\\t\\u00A0\\u2002-\\u200A\\u202F\\u3000]");
    /** 공백 3개 이상 → 2개 (칸 구분으로 쓰이는 2칸 공백은 남김) */
    private static final Pattern MANY_SPACES = Pattern.compile(" {3,}");
    private static final Pattern MANY_BLANK_LINES = Pattern.compile("\\n{3,}");

    /** 의미 있는 글자가 이보다 적으면 스캔본·이미지로 봄 */
    private static final int MIN_MEANINGFUL = 30;
    /** 공백 제외 글자 중 한글·영문·숫자 비율이 이보다 낮으면 글자 깨짐으로 봄 */
    private static final double MIN_MEANINGFUL_RATIO = 0.5;

    private TextNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        // 맥에서 만든 파일은 '홍' 이 'ㅎ+ㅗ+ㅇ' 으로 나뉘어 있을 수 있음 → 합침
        String s = Normalizer.normalize(raw, Normalizer.Form.NFC);
        s = s.replace("\r\n", "\n").replace('\r', '\n').replace('\f', '\n');
        s = INVISIBLE.matcher(s).replaceAll("");
        s = PRIVATE_USE.matcher(s).replaceAll(" ");
        s = SPACES.matcher(s).replaceAll(" ");

        StringBuilder sb = new StringBuilder(s.length());
        for (String line : s.split("\n", -1)) {
            sb.append(MANY_SPACES.matcher(line).replaceAll("  ").strip()).append('\n');
        }
        return MANY_BLANK_LINES.matcher(sb).replaceAll("\n\n").strip();
    }

    /** 글자가 거의 없거나 깨진 파일이면 예외 */
    public static void requireReadable(String text) {
        int total = 0;
        int meaningful = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) continue;
            total++;
            if ((c >= '가' && c <= '힣') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                meaningful++;
            }
        }
        if (meaningful < MIN_MEANINGFUL) {
            throw new ResumeImportException(ResumeImportException.UNREADABLE,
                    "파일에서 글자를 찾지 못했어요. 스캔본이나 이미지로 된 문서는 지원하지 않아요.");
        }
        if ((double) meaningful / total < MIN_MEANINGFUL_RATIO) {
            throw new ResumeImportException(ResumeImportException.UNREADABLE,
                    "글자가 깨져서 읽을 수 없는 파일이에요. 원본 사이트에서 다른 형식(Word 등)으로 받아 올려 주세요.");
        }
    }
}
