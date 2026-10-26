package resume.core.importer.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import resume.core.dto.ResumeImportResultDto;
import resume.core.enums.ResumeFileType;
import resume.core.enums.ResumeSource;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 어느 사이트 이력서인지 판별해서 알맞은 파서로 넘김
 *  - 사이트 전용 파서(SiteResumeParser) 중 점수가 가장 높고 MIN_SCORE 이상이면 그 파서
 *  - 아니면 일반 파서 (판별에 실패해도 지금보다 나빠지지 않음)
 *  - @Primary : ResumeService 의 ResumeParser 주입은 이 클래스로
 */
@Primary
@Component
public class ResumeParserRouter implements ResumeParser {

    private static final Logger log = LoggerFactory.getLogger(ResumeParserRouter.class);

    /** 하단 안내문 하나(3점) 또는 화면 문구 세 개면 그 사이트로 봄 */
    static final int MIN_SCORE = 3;

    private final GenericResumeParser generic;
    private final List<SiteResumeParser> sites;

    public ResumeParserRouter(GenericResumeParser generic, List<SiteResumeParser> sites) {
        this.generic = generic;
        this.sites = sites;
    }

    @Override
    public ResumeImportResultDto parse(String text, ResumeFileType fileType) {
        return pick(text).parse(text, fileType);
    }

    /** 디버그 : 맨 앞에 판별 결과(사이트별 점수)를 붙임 */
    @Override
    public Map<String, List<String>> debugSections(String text) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        ResumeParser parser = pick(text);
        String chosen = parser instanceof SiteResumeParser s ? s.source().getLabel() : ResumeSource.DIRECT.getLabel();
        out.put("(인식한 사이트)", List.of(chosen));
        out.put("(사이트별 점수)", sites.stream()
                .map(s -> s.source().getLabel() + " " + s.matchScore(text) + "점 (기준 " + MIN_SCORE + "점)")
                .toList());
        out.putAll(parser.debugSections(text));
        return out;
    }

    private ResumeParser pick(String text) {
        return sites.stream()
                .map(s -> Map.entry(s, s.matchScore(text)))
                .filter(e -> e.getValue() >= MIN_SCORE)
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .<ResumeParser>map(e -> {
                    log.debug("이력서 파일 판별 : {} ({}점)", e.getKey().source(), e.getValue());
                    return e.getKey();
                })
                .orElse(generic);
    }
}
