package resume.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.dto.TemplateResponseDto;

import java.util.List;

@Mapper
public interface TemplateMapper {
    // 템플릿 목록 조회
    List<TemplateResponseDto> selectTemplateList(TemplateRequestDto requestDto);
    // 템플릿 상세 조회
    TemplateDetailResponseDto selectTemplateDetail(TemplateRequestDto requestDto);
    // 템플릿 추가
    void insertTemplate(TemplateDto requestDto);
    // 템플릿 수정 (작성자 본인 것만)
    int updateTemplate(TemplateDto requestDto);
    // 템플릿 삭제
    void deleteTemplate(TemplateRequestDto requestDto);
    // 내 이력서인지 확인
    int countMyResume(@Param("resumeId") Long resumeId, @Param("userId") Long userId);
    // 이력서에 템플릿 연결
    int updateResumeTemplate(@Param("resumeId") Long resumeId, @Param("templateId") Long templateId);
}
