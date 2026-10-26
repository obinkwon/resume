package resume.core.importer;

import resume.core.enums.ResumeFileType;
import resume.core.exception.ResumeImportException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 확장자 + 실제 파일 내용으로 형식 판별
 *  - 확장자만 바꾼 파일, 암호 걸린 Office 파일, 지원하지 않는 형식을 걸러냄
 */
public final class ResumeFileTypeDetector {

    private static final Pattern HWPX_SECTION = Pattern.compile("Contents/section\\d+\\.xml");
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "tif", "tiff");

    private static final String SUPPORTED_MSG = "PDF, Word(.docx), 한글(.hwpx) 파일만 올릴 수 있어요.";

    private ResumeFileTypeDetector() {
    }

    public static ResumeFileType detect(String fileName, byte[] bytes) {
        String ext = extension(fileName);

        switch (ext) {
            case "hwp" -> throw unsupported("한글 .hwp 파일은 지원하지 않아요. 한글에서 [다른 이름으로 저장]으로 .hwpx 또는 PDF로 저장해 올려 주세요.");
            case "doc" -> throw unsupported("예전 Word(.doc) 파일은 지원하지 않아요. Word에서 .docx로 저장해 올려 주세요.");
            default -> {
                if (IMAGE_EXT.contains(ext)) {
                    throw unsupported("이미지 파일은 지원하지 않아요. " + SUPPORTED_MSG);
                }
            }
        }

        ResumeFileType type = ResumeFileType.fromExtension(ext).orElseThrow(() -> unsupported(SUPPORTED_MSG));

        switch (type) {
            case PDF -> {
                if (!looksLikePdf(bytes)) throw mismatch(ext);
            }
            case DOCX, HWPX -> {
                // 암호 걸린 docx / hwpx 는 ZIP 이 아니라 OLE2 형식으로 저장됨
                if (isOle2(bytes)) {
                    throw new ResumeImportException(ResumeImportException.UNREADABLE,
                            "암호가 걸린 파일이에요. 암호를 해제하고 다시 저장해 올려 주세요.");
                }
                if (!isZip(bytes)) throw mismatch(ext);

                Set<String> names = SafeZip.read(bytes, name -> false).names();
                boolean ok = type == ResumeFileType.DOCX
                        ? names.contains("word/document.xml")
                        : names.stream().anyMatch(n -> HWPX_SECTION.matcher(n).matches());
                if (!ok) throw mismatch(ext);
            }
        }
        return type;
    }

    private static String extension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
    }

    /** 일부 PDF 는 헤더 앞에 쓰레기 바이트가 있어서 앞부분 1KB 안에서 찾음 */
    private static boolean looksLikePdf(byte[] b) {
        int limit = Math.min(b.length, 1024);
        String head = new String(b, 0, limit, StandardCharsets.ISO_8859_1);
        return head.contains("%PDF-");
    }

    private static boolean isZip(byte[] b) {
        return b.length >= 4 && b[0] == 0x50 && b[1] == 0x4B && b[2] == 0x03 && b[3] == 0x04;
    }

    private static boolean isOle2(byte[] b) {
        return b.length >= 8
                && (b[0] & 0xFF) == 0xD0 && (b[1] & 0xFF) == 0xCF && (b[2] & 0xFF) == 0x11 && (b[3] & 0xFF) == 0xE0
                && (b[4] & 0xFF) == 0xA1 && (b[5] & 0xFF) == 0xB1 && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0xE1;
    }

    private static ResumeImportException unsupported(String msg) {
        return new ResumeImportException(ResumeImportException.UNSUPPORTED, msg);
    }

    private static ResumeImportException mismatch(String ext) {
        return new ResumeImportException(ResumeImportException.BAD_REQUEST,
                "파일 내용이 확장자(." + ext + ")와 맞지 않아요. 파일이 손상됐거나 확장자만 바뀐 파일일 수 있어요.");
    }
}
