package resume.core.importer;

import resume.core.exception.ResumeImportException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * docx / hwpx (둘 다 ZIP) 를 안전하게 읽기
 *  - 항목 개수, 압축 해제 크기를 제한해서 압축 폭탄(zip bomb)을 막음
 */
public final class SafeZip {

    static final int MAX_ENTRIES = 2_000;
    static final long MAX_ENTRY_BYTES = 30L * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 100L * 1024 * 1024;

    private SafeZip() {
    }

    /**
     * @param names 전체 항목 이름 (순서 유지)
     * @param files keep 조건에 맞는 항목의 내용
     */
    public record Result(Set<String> names, Map<String, byte[]> files) {
    }

    public static Result read(byte[] zip, Predicate<String> keep) {
        Set<String> names = new LinkedHashSet<>();
        Map<String, byte[]> files = new HashMap<>();
        byte[] buf = new byte[8192];
        long total = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (names.size() >= MAX_ENTRIES) {
                    throw tooBig();
                }
                String name = entry.getName();
                names.add(name);

                ByteArrayOutputStream out = !entry.isDirectory() && keep.test(name) ? new ByteArrayOutputStream() : null;
                long size = 0;
                int n;
                while ((n = zis.read(buf)) > 0) {
                    size += n;
                    total += n;
                    if (size > MAX_ENTRY_BYTES || total > MAX_TOTAL_BYTES) {
                        throw tooBig();
                    }
                    if (out != null) out.write(buf, 0, n);
                }
                if (out != null) files.put(name, out.toByteArray());
            }
        } catch (ZipException e) {
            throw new ResumeImportException(ResumeImportException.BAD_REQUEST,
                    "파일이 손상돼서 열 수 없어요. 원본 파일을 다시 저장해 올려 주세요.", e);
        } catch (IOException e) {
            throw new ResumeImportException(ResumeImportException.BAD_REQUEST,
                    "파일을 읽지 못했어요. 다시 시도해 주세요.", e);
        }
        return new Result(names, files);
    }

    private static ResumeImportException tooBig() {
        return new ResumeImportException(ResumeImportException.TOO_LARGE,
                "파일 안의 내용이 너무 커서 읽을 수 없어요.");
    }
}
