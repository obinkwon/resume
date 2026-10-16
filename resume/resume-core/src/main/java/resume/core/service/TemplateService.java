package resume.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.dto.TemplateResponseDto;
import resume.core.mapper.TemplateMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateMapper templateMapper;

    public List<TemplateResponseDto> getTemplateList(TemplateRequestDto requestDto) {
        return templateMapper.selectTemplateList(requestDto);
    }

    public TemplateDetailResponseDto getTemplateDetail(TemplateRequestDto requestDto) {
        return templateMapper.selectTemplateDetail(requestDto);
    }
}
