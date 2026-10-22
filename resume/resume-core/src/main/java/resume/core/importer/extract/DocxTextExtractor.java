package resume.core.importer.extract;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFSDT;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import resume.core.importer.ResumeFileType;
import resume.core.importer.ResumeImportException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Word(.docx) 글자 추출 (Apache POI)
 *  - 문단·표를 문서 순서대로, 표 안의 표도 읽음
 */
@Component
public class DocxTextExtractor implements ResumeTextExtractor {

    /** 표 안의 표 깊이 제한 */
    private static final int MAX_DEPTH = 5;

    @Override
    public ResumeFileType supports() {
        return ResumeFileType.DOCX;
    }

    @Override
    public String extract(byte[] bytes) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder out = new StringBuilder();
            appendBody(doc.getBodyElements(), out, 0);
            return out.toString();
        } catch (EncryptedDocumentException e) {
            throw new ResumeImportException(ResumeImportException.UNREADABLE,
                    "암호가 걸린 Word 파일이에요. 암호를 해제하고 다시 저장해 올려 주세요.", e);
        } catch (IOException | RuntimeException e) {
            throw new ResumeImportException(ResumeImportException.BAD_REQUEST,
                    "Word 파일을 읽지 못했어요. 파일이 손상됐을 수 있어요.", e);
        }
    }

    private void appendBody(List<IBodyElement> elements, StringBuilder out, int depth) {
        for (IBodyElement el : elements) {
            if (el instanceof XWPFParagraph p) {
                String text = p.getText();
                if (text != null && !text.isBlank()) out.append(text).append('\n');
            } else if (el instanceof XWPFTable table) {
                if (depth >= MAX_DEPTH) continue;
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        StringBuilder sub = new StringBuilder();
                        appendBody(cell.getBodyElements(), sub, depth + 1);
                        cells.add(sub.toString());
                    }
                    TableText.appendRow(cells, out);
                }
            } else if (el instanceof XWPFSDT sdt) {
                // 콘텐츠 컨트롤(입력 칸) 안의 글자
                String text = sdt.getContent().getText();
                if (text != null && !text.isBlank()) out.append(text).append('\n');
            }
        }
    }
}
