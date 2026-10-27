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
import resume.core.dto.ResumeRequestDto;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.security.UserPrincipal;
import resume.core.service.ResumeService;
import resume.core.service.TemplateService;
import resume.core.template.SampleResumeFactory;

@Controller
@RequiredArgsConstructor
@RequestMapping("/web/resume")
public class ResumeWebController {

	private final ResumeService resumeService;
	private final TemplateService templateService;
    private final SampleResumeFactory sampleResumeFactory;

	@GetMapping("/list")
	public String resumeListPage(@AuthenticationPrincipal UserPrincipal principal,
                                 Model model,
                                 ResumeRequestDto requestDto) {
		requestDto.setUserId(principal.getUserId());

		model.addAttribute("user", principal);
		model.addAttribute("resumeList", resumeService.getResumeList(requestDto));
		return "thymeleaf/resume/resume-list";
	}

	@GetMapping("/regist")
	public String resumeRegistPage(Model model) {

		return "thymeleaf/resume/resume-form";
	}

	@GetMapping("/modify/{resumeId}")
    public String resumeModifyPage(@PathVariable Long resumeId,
                                   @AuthenticationPrincipal UserPrincipal principal,
                                   Model model,
                                   ResumeRequestDto requestDto) {

		requestDto.setResumeId(resumeId);
		model.addAttribute("resumeDetail", resumeService.getResumeDetail(requestDto));
		return "thymeleaf/resume/resume-form";
	}

    @GetMapping("/template/{resumeId}")
    public String resumeTemplatePage(@PathVariable Long resumeId,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     Model model,
                                     ResumeRequestDto requestDto) {

        requestDto.setResumeId(resumeId);
        model.addAttribute("resumeDetail", resumeService.getResumeDetail(requestDto));
        return "thymeleaf/resume/resume-template";
    }

    @GetMapping({"/preview", "/preview/{resumeId}"})
    public String resumePreviewPage(@PathVariable(required = false) Long resumeId,
                                    @AuthenticationPrincipal UserPrincipal principal,
                                    Model model,
                                    ResumeRequestDto requestDto) {

        if (resumeId != null) {
            requestDto.setResumeId(resumeId);
        }

        model.addAttribute("resumeDetail", requestDto.getResumeId() != null
                ? resumeService.getResumeDetail(requestDto)
                : sampleResumeFactory.create());
        model.addAttribute("resumeTemplate", findUsableTemplate(requestDto.getTemplateId(), principal));
        return "thymeleaf/resume/resume-preview";
    }


    /**
     * 쓸 수 있는 템플릿 조회 (공개 템플릿 또는 내 템플릿)
     *  - templateId 가 없거나 찾지 못하면 null → 미리보기가 모든 섹션을 기본 순서로 그림
     *  - required 면 찾지 못했을 때 404 (주소로 남의 비공개 템플릿을 지정한 경우 등)
     */
    private TemplateDetailResponseDto findUsableTemplate(Long templateId, UserPrincipal principal) {
        if (templateId == null) return null;

        TemplateRequestDto templateRequestDto = new TemplateRequestDto();
        templateRequestDto.setTemplateId(templateId);
        templateRequestDto.setUserId(principal.getUserId());
        templateRequestDto.setIsPublic(true);

        TemplateDetailResponseDto template = templateService.getTemplateDetail(templateRequestDto);
        if (template == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다.");
        }
        return template;
    }
}