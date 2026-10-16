package resume.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import resume.core.dto.TemplateRequestDto;
import resume.core.dto.TemplateResponseDto;

import java.util.List;

@Mapper
public interface TemplateMapper {
    // 템플릿 목록 조회
    List<TemplateResponseDto> selectTemplateList(TemplateRequestDto requestDto);
}
