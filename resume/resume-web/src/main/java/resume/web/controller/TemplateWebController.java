package resume.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.security.UserPrincipal;
import resume.core.service.TemplateService;

@Controller
@RequestMapping("/web/template")
@RequiredArgsConstructor
public class TemplateWebController {

    /** 편집 화면 이름 입력칸 maxlength 와 같게 맞춤 */
    private static final int NAME_MAX = 50;
    private static final String COPY_SUFFIX = " 복사본";

    private final TemplateService templateService;

    @GetMapping("/list")
    public String templateListPage() {
        return "thymeleaf/resume/resume-template-list";
    }

    @GetMapping("/regist")
    public String templateRegistPage(TemplateRequestDto requestDto,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     Model model) {
        TemplateDetailResponseDto base = null;
        // 템플릿 복사해서 생성할 경우 (공개 템플릿 또는 내 템플릿만)
        if (requestDto.getTemplateId() != null) {
            requestDto.setUserId(principal.getUserId());
            requestDto.setIsPublic(true);
            base = templateService.getTemplateDetail(requestDto);
            if (base == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다.");
            }
        }

        TemplateDto draft = TemplateDto.builder()
                .templateName(base == null ? "새 템플릿" : copyName(base.getTemplateName()))
                .description(base == null ? "" : base.getDescription())
                .htmlContent(base == null ? "" : base.getHtmlContent())
                .cssContent(base == null ? "" : base.getCssContent())
                .build();
        model.addAttribute("resumeTemplate", draft);
        model.addAttribute("mode", "C");   // C : 생성 / U : 수정
        return "thymeleaf/resume/resume-template-form";
    }

    @GetMapping("/modify/{templateId}")
    public String templateModifyPage(@PathVariable Long templateId,
                                     TemplateRequestDto requestDto,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     Model model) {
        requestDto.setTemplateId(templateId);
        // 내 템플릿만 조회 (기본 템플릿은 복사해서 만들기)
        requestDto.setUserId(principal.getUserId());
        requestDto.setIsPublic(false);
        TemplateDetailResponseDto templateDetailDto = templateService.getTemplateDetail(requestDto);
        if (templateDetailDto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다.");
        }

        model.addAttribute("resumeTemplate", templateDetailDto);
        model.addAttribute("mode", "U");   // C : 생성 / U : 수정
        return "thymeleaf/resume/resume-template-form";
    }

    private String copyName(String name) {
        String base = name == null ? "" : name.trim();
        if (base.endsWith(COPY_SUFFIX.trim())) {
            base = base.substring(0, base.length() - COPY_SUFFIX.trim().length()).trim();
        }
        if (base.isEmpty()) base = "템플릿";

        int max = NAME_MAX - COPY_SUFFIX.length();
        if (base.length() > max) base = base.substring(0, max).trim();
        return base + COPY_SUFFIX;
    }
}