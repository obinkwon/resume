package resume.core.extractor;

import java.util.ArrayList;
import java.util.List;

/**
 * 표의 한 행을 글자로 바꾸기 (docx, hwpx 공통)
 *  - 짧은 칸들        → "성명 | 홍길동 | 생년월일 | 1995.01.01"
 *  - 긴 글이 있는 행  → 칸마다 줄을 나눠서 ("지원동기" 다음 줄부터 내용)
 */
final class TableText {

    /** 이보다 긴 칸이 있으면 한 줄로 잇지 않음 */
    private static final int LONG_CELL = 80;

    private TableText() {
    }

    static void appendRow(List<String> cells, StringBuilder out) {
        List<String> values = new ArrayList<>();
        String prev = null;
        for (String cell : cells) {
            String v = cell == null ? "" : cell.strip();
            // 빈 칸, 병합 셀 때문에 같은 값이 반복되는 칸은 건너뜀
            if (v.isEmpty() || v.equals(prev)) continue;
            values.add(v);
            prev = v;
        }
        if (values.isEmpty()) return;

        boolean multiline = values.stream().anyMatch(v -> v.indexOf('\n') >= 0 || v.length() > LONG_CELL);
        if (values.size() == 1 || multiline) {
            for (String v : values) out.append(v).append('\n');
        } else {
            out.append(String.join(" | ", values)).append('\n');
        }
    }
}
