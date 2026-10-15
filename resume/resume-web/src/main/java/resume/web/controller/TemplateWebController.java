package resume.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import resume.core.dto.TemplateSettingDto;
import resume.core.service.ResumeService;
import resume.core.service.TemplateService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 템플릿 화면(HTML) 컨트롤러
 *  - GET /templates/new?resumeId=3     : 이력서에서 [템플릿] 클릭 → 이미 연결된 템플릿 있으면 수정 화면으로
 *  - GET /templates/{templateId}/edit  : 저장된 템플릿 수정
 *  - GET /templates/preview            : iframe 미리보기 / 인쇄 (templateId, resumeId 둘 다 선택)
 *
 * 저장·수정·이력서 연결은 TemplateApiController(/api/templates) 에서 처리
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/web/template")
public class TemplateWebController {

    private final TemplateService templateService;
    private final ResumeService resumeService;

    @GetMapping("/regist")
    public String templateRegistPage(Model model) {
        model.addAttribute("setting", new TemplateSettingDto());
        return "thymeleaf/resume/template-form";
    }

    @GetMapping("/preview")
    public String preview(@RequestParam(required = false) Long templateId, Model model) {
        model.addAttribute("setting", new TemplateSettingDto());
        return "thymeleaf/resume/template-preview";
    }

//    @GetMapping("/modify/{templateId}")
//    public String templateModifyPage(@PathVariable Long templateId,
//                           @RequestParam(required = false) Long resumeId,
//                           Model model) {
//        TemplateSettingDto setting = templateService.getTemplate(templateId);   // 없거나 권한 없으면 예외
//
//        // 미리보기 대상: 지정한 이력서 → 이 템플릿을 쓰는 첫 이력서 → 없으면 샘플
//        Long previewResumeId = resumeId != null
//                ? resumeId
//                : templateService.findFirstLinkedResumeId(templateId);
//
//        return builder(setting, previewResumeId, model);
//    }
//
//    private String builder(TemplateSettingDto setting, Long previewResumeId, Model model) {
//        model.addAttribute("setting", setting);
//        model.addAttribute("previewResumeId", previewResumeId);               // null → 샘플로 미리보기
//        model.addAttribute("resumes", resumeService.getMyResumeSummaries());  // 미리보기 대상 선택용 (resumeId, title)
//        return "template-form";
//    }
//
//    /**
//     * 미리보기 (빌더의 iframe 안 / 단독으로 열면 인쇄용)
//     *  - templateId 없음 → 기본 설정 (빌더에서는 postMessage 로 실시간 덮어씀)
//     *  - resumeId   없음 → 샘플 이력서
//     */

    // =========================================================
    // 이력서가 없을 때 미리보기용 샘플
    // ResumeDetailDto 와 같은 키 이름의 Map — Thymeleaf 는 Map 도 ${resumeDetail.profile.name} 로 접근 가능
    // =========================================================
    private Map<String, Object> sampleResume() {
        return map(
                "title", "샘플 이력서",
                "profile", map(
                        "name", "홍길동",
                        "email", "hong@example.com",
                        "phone", "010-1234-5678",
                        "address", "서울특별시",
                        "introduction", "사용자 경험을 먼저 생각하는 백엔드 개발자입니다.\n"
                                + "Spring Boot 기반 서비스를 설계·운영하며 성능 개선에 집중해 왔습니다."
                ),
                "links", List.of(
                        map("linkType", "github", "displayName", "GitHub", "url", "https://github.com/example")
                ),
                "skills", List.of(
                        map("skillName", "Java",        "category", "Backend",  "proficiency", 5),
                        map("skillName", "Spring Boot", "category", "Backend",  "proficiency", 4),
                        map("skillName", "MyBatis",     "category", "Backend",  "proficiency", 4),
                        map("skillName", "JavaScript",  "category", "Frontend", "proficiency", 3)
                ),
                "educations", List.of(
                        map("schoolName", "한국대학교", "major", "컴퓨터공학과", "degree", "학사",
                                "startDate", LocalDate.of(2015, 3, 1), "endDate", LocalDate.of(2021, 2, 20),
                                "description", "")
                ),
                "certificates", List.of(
                        map("certificateName", "정보처리기사", "issuer", "한국산업인력공단",
                                "issuedDate", LocalDate.of(2020, 11, 20), "description", "")
                ),
                "experiences", List.of(
                        map("companyName", "(주)샘플테크", "position", "백엔드 개발",
                                "startDate", LocalDate.of(2021, 3, 2), "endDate", null, "isCurrent", true,
                                "description", "주문/결제 API 개발 및 운영\n배치 처리 성능 40% 개선")
                ),
                "projects", List.of(
                        map("projectName", "사내 이력 관리 시스템", "role", "백엔드 리드",
                                "startDate", LocalDate.of(2023, 1, 2), "endDate", LocalDate.of(2023, 8, 31),
                                "description", "이력서 작성·템플릿·PDF 출력 기능 설계 및 구현",
                                "techStack", "Java, Spring Boot, MyBatis, Thymeleaf", "demoUrl", "")
                )
        );
    }

    /** null 값 허용 (Map.of 는 null 불가 / 키가 없으면 SpEL 오류) */
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}