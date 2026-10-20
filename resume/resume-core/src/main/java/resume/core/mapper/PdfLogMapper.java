package resume.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import resume.core.dto.PdfLogDto;

@Mapper
public interface PdfLogMapper {
    // PDF 다운로드 이력 로그 추가
    void insertPdfLog(PdfLogDto pdfLogDto);
}
