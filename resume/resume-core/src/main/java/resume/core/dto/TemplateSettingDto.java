package resume.core.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 이력서 템플릿 설정 — 이력서와 독립적으로 존재
 *
 * DB 예시
 *   resume_template : template_id(PK), user_id, template_name, template_code, layout_type,
 *                     primary_color, font_family, font_scale, section_config(JSON), reg_dt, upd_dt
 *   resume          : ... , template_id (NULL 허용, FK) ← 이력서가 템플릿을 "선택"하는 구조
 *
 * → 템플릿만 먼저 만들어 두고, 이력서는 나중에 작성해서 연결 가능
 * → 하나의 템플릿을 여러 이력서에 재사용 가능
 */
@Getter @Setter
@NoArgsConstructor
public class TemplateSettingDto {

    private Long templateId;                    // null 이면 아직 저장 안 된 새 템플릿
    private String templateName = "새 템플릿";
    private String templateCode = "minimal";    // minimal | modern | ats
    private String layoutType   = "single";     // single | sidebar | split
    private String primaryColor = "#2D3748";
    private String fontFamily   = "Pretendard";
    private int fontScale       = 100;          // 85 ~ 115 (%)
    private List<SectionSetting> sections = defaultSections();

    @Getter @Setter
    @NoArgsConstructor
    public static class SectionSetting {
        private String key;
        private boolean visible = true;

        public SectionSetting(String key, boolean visible) {
            this.key = key;
            this.visible = visible;
        }
    }

    /** 이력서 작성 폼(resume-form.html)의 섹션 순서 기준 */
    public static final List<String> SECTION_KEYS =
            List.of("intro", "skill", "education", "certificate", "experience", "project", "link");

    public static List<SectionSetting> defaultSections() {
        List<SectionSetting> list = new ArrayList<>();
        SECTION_KEYS.forEach(key -> list.add(new SectionSetting(key, true)));
        return list;
    }
}