package resume.core.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import resume.core.dto.ResumeResponseDto;
import resume.core.dto.TemplateDto;

import java.util.List;

/**
 * 템플릿 편집기 / 내 템플릿 목록 전용 쿼리.
 *  - 로그인 아이디(loginId) = users.user_key 로 가정
 *  - 템플릿 작성자(templates.created_by) 에 loginId 를 저장
 *  - 컬럼명이 다르면 이 파일만 고치면 됨 (XML 매퍼로 옮겨도 무방)
 */
@Mapper
public interface TemplateEditorMapper {

    String TEMPLATE_COLUMNS = "template_id, template_name, description, thumbnail_url, "
            + "html_content, css_content, is_public, created_by, use_yn";

    @Select("SELECT " + TEMPLATE_COLUMNS + " FROM templates "
            + "WHERE template_id = #{templateId} AND use_yn = 'Y'")
    TemplateDto findTemplate(@Param("templateId") Long templateId);

    @Insert("INSERT INTO templates "
            + "(template_name, description, thumbnail_url, html_content, css_content, is_public, created_by, use_yn) "
            + "VALUES (#{templateName}, #{description}, #{thumbnailUrl}, #{htmlContent}, #{cssContent}, "
            + "#{isPublic}, #{createdBy}, 'Y')")
    @Options(useGeneratedKeys = true, keyProperty = "templateId", keyColumn = "template_id")
    int insertTemplate(TemplateDto template);

    @Update("UPDATE templates SET "
            + "template_name = #{templateName}, description = #{description}, "
            + "html_content = #{htmlContent}, css_content = #{cssContent} "
            + "WHERE template_id = #{templateId} AND created_by = #{createdBy} AND use_yn = 'Y'")
    int updateTemplate(TemplateDto template);

    /** 이 템플릿을 쓰는 내 이력서 수 (삭제 전 확인) */
    @Select("SELECT COUNT(*) FROM resume.resumes r "
            + "JOIN resume.users u ON u.user_id = r.user_id "
            + "WHERE r.template_id = #{templateId} AND r.use_yn = 'Y' AND u.user_key = #{loginId}")
    int countResumesUsingTemplate(@Param("templateId") Long templateId, @Param("loginId") String loginId);

    /** 템플릿 적용 대상으로 고를 내 이력서 */
    @Select("SELECT r.resume_id, r.template_id, r.title, r.updated_at FROM resume.resumes r "
            + "JOIN resume.users u ON u.user_id = r.user_id "
            + "WHERE r.use_yn = 'Y' AND u.user_key = #{loginId} "
            + "ORDER BY r.updated_at DESC NULLS LAST")
    List<ResumeResponseDto> findMyResumes(@Param("loginId") String loginId);

    /** 내 이력서인지 확인 */
    @Select("SELECT COUNT(*) FROM resume.resumes r "
            + "JOIN resume.users u ON u.user_id = r.user_id "
            + "WHERE r.resume_id = #{resumeId} AND r.use_yn = 'Y' AND u.user_key = #{loginId}")
    int countMyResume(@Param("resumeId") Long resumeId, @Param("loginId") String loginId);

    @Update("UPDATE resume.resumes SET template_id = #{templateId}, updated_at = now() "
            + "WHERE resume_id = #{resumeId} AND use_yn = 'Y'")
    int updateResumeTemplate(@Param("resumeId") Long resumeId, @Param("templateId") Long templateId);
}