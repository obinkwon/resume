package resume.core.extractor;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import resume.core.enums.ResumeFileType;
import resume.core.exception.ResumeImportException;

import java.io.IOException;

/**
 * PDF 글자 추출 (Apache PDFBox 3.x)
 */
@Component
public class PdfTextExtractor implements ResumeTextExtractor {

    /** 이력서로 보기 어려운 긴 문서는 거절 */
    private static final int MAX_PAGES = 30;

    @Override
    public ResumeFileType supports() {
        return ResumeFileType.PDF;
    }

    @Override
    public String extract(byte[] bytes) {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            if (doc.getNumberOfPages() > MAX_PAGES) {
                throw new ResumeImportException(ResumeImportException.BAD_REQUEST,
                        "PDF가 너무 길어요. " + MAX_PAGES + "쪽 이하의 이력서만 올릴 수 있어요.");
            }
            if (doc.isEncrypted() && !doc.getCurrentAccessPermission().canExtractContent()) {
                throw new ResumeImportException(ResumeImportException.UNREADABLE,
                        "글자 복사가 막혀 있는 PDF예요. 보안 설정이 없는 PDF로 다시 저장해 올려 주세요.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");
            // false(기본값) = PDF 에 쓰인 순서대로. 2단 레이아웃에서 왼쪽·오른쪽 칸이 섞이지 않음
            stripper.setSortByPosition(false);
            return stripper.getText(doc);
        } catch (ResumeImportException e) {
            throw e;
        } catch (InvalidPasswordException e) {
            throw new ResumeImportException(ResumeImportException.UNREADABLE,
                    "암호가 걸린 PDF예요. 암호를 해제하고 다시 저장해 올려 주세요.", e);
        } catch (IOException | RuntimeException e) {
            throw new ResumeImportException(ResumeImportException.BAD_REQUEST,
                    "PDF를 읽지 못했어요. 파일이 손상됐을 수 있어요.", e);
        }
    }
}
