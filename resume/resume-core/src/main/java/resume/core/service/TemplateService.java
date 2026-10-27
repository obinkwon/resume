package resume.core.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.Media;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.dto.TemplateResponseDto;
import resume.core.mapper.TemplateMapper;
import resume.core.template.TemplateValidator;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateMapper templateMapper;
    private final TemplateValidator templateValidator;

    /*
     * 단계별 제한 시간 — 합계(15 + 10 + 5 + 20 = 50초)가 REQUEST_TIMEOUT_SEC 보다 짧아야
     * 컨트롤러가 포기한 뒤에도 워커가 붙잡혀 다음 요청이 밀리는 일이 없다.
     */
    private static final double NAVIGATE_TIMEOUT_MS = 15_000;
    private static final double RENDER_WAIT_TIMEOUT_MS = 10_000;
    private static final int FONT_WAIT_TIMEOUT_MS = 5_000;
    private static final double PDF_TIMEOUT_MS = 20_000;
    /** 요청 하나가 기다리는 최대 시간 (앞 요청 대기 시간 포함) */
    private static final long REQUEST_TIMEOUT_SEC = 60;

    @Value("${playwright.browsers-path}")
    private String browsersPath;
    @Value("${playwright.auto-install:false}")
    private boolean autoInstall;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "resume-pdf");
        t.setDaemon(true);
        return t;
    });

    // worker 스레드에서만 사용
    private Playwright playwright;
    private Browser browser;

    public List<TemplateResponseDto> getTemplateList(TemplateRequestDto requestDto) {
        return templateMapper.selectTemplateList(requestDto);
    }

    public TemplateDetailResponseDto getTemplateDetail(TemplateRequestDto requestDto) {
        return templateMapper.selectTemplateDetail(requestDto);
    }

    @Transactional
    public void deleteTemplate(TemplateRequestDto requestDto) {
        templateMapper.deleteTemplate(requestDto);
    }

    /** 내 템플릿 (사용 중, 비공개, 작성자 본인) 인가 */
    public boolean isOwner(TemplateDetailResponseDto t, Long userId) {
        return t != null && userId != null && "Y".equals(t.getUseYn())
                && !Boolean.TRUE.equals(t.getIsPublic()) && Objects.equals(t.getCreatedBy(), userId);
    }

    /**
     * 템플릿 직접 만들기 저장 후 템플릿 번호를 돌려준다.
     *  - 기본(공개) 템플릿은 고칠 수 없고, 편집해서 저장하면 내 템플릿으로 새로 추가된다.
     *  - 내 템플릿을 편집 중이고 saveAsNew 가 아니면 덮어쓰기
     *  - 그 외에는 새 템플릿으로 추가
     *  - resumeId 가 있으면 그 이력서에 연결
     */
    @Transactional
    public Long saveTemplate(TemplateRequestDto requestDto) {
        templateValidator.validateOrThrow(requestDto.getTemplateName(), requestDto.getHtmlContent(), requestDto.getCssContent());
        if (requestDto.getResumeId() != null && templateMapper.countMyResume(requestDto.getResumeId(), requestDto.getUserId()) == 0) {
            throw new IllegalArgumentException("이력서를 찾을 수 없습니다.");
        }

        TemplateDetailResponseDto base = requestDto.getBaseTemplateId() == null ? null
                : templateMapper.selectTemplateDetail(TemplateRequestDto.builder().templateId(requestDto.getBaseTemplateId()).build());
        boolean overwrite = !Boolean.TRUE.equals(requestDto.getSaveAsNew()) && isOwner(base, requestDto.getUserId());

        TemplateDto t = TemplateDto.builder()
                .templateName(requestDto.getTemplateName().trim())
                .description(StringUtils.hasText(requestDto.getDescription()) ? requestDto.getDescription().trim() : "직접 만든 템플릿")
                .htmlContent(requestDto.getHtmlContent() == null ? "" : requestDto.getHtmlContent())
                .cssContent(requestDto.getCssContent() == null ? "" : requestDto.getCssContent())
                .isPublic(false)
                .createdBy(requestDto.getUserId())
                .build();

        if (overwrite) {
            t.setTemplateId(base.getTemplateId());
            if (templateMapper.updateTemplate(t) == 0) {
                throw new IllegalStateException("템플릿을 저장하지 못했습니다.");
            }
        } else {
            templateMapper.insertTemplate(t);
        }

        if (requestDto.getResumeId() != null) {
            templateMapper.updateResumeTemplate(requestDto.getResumeId(), t.getTemplateId());
        }
        return t.getTemplateId();
    }

    /**
     * @param url     미리보기 페이지 주소 (서버 자신에게 요청하는 내부 주소)
     * @param cookies 로그인 세션 쿠키 (미리보기 페이지의 권한 확인을 그대로 통과하기 위해)
     */
    public byte[] render(String url, List<Cookie> cookies) {
        Future<byte[]> future = worker.submit(() -> doRender(url, cookies));
        try {
            return future.get(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("PDF 생성 시간이 초과됐어요.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("PDF 생성에 실패했어요.", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PDF 생성이 중단됐어요.", e);
        }
    }

    private byte[] doRender(String url, List<Cookie> cookies) {
        ensureBrowser();
        String origin = originOf(url);

        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(794, 1123)
                .setLocale("ko-KR"))) {

            if (cookies != null && !cookies.isEmpty()) {
                context.addCookies(cookies);
            }

            // 서버 자신 이외의 요청은 차단 — 외부 URL 때문에 렌더링이 멈추는 것 방지
            // 단, CSS·폰트는 미리보기 화면과 똑같이 보이도록 허용 (허용 호스트는 미리보기 페이지 CSP 가 제한)
            context.route("**/*", route -> {
                String u = route.request().url();
                String type = route.request().resourceType();
                if (u.startsWith(origin) || u.startsWith("data:") || u.startsWith("blob:")
                        || "stylesheet".equals(type) || "font".equals(type)) {
                    route.resume();
                } else {
                    route.abort();
                }
            });

            Page page = context.newPage();
            page.setDefaultTimeout(PDF_TIMEOUT_MS);

            Response response = page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.LOAD)
                    .setTimeout(NAVIGATE_TIMEOUT_MS));
            if (response != null && response.status() >= 400) {
                throw new IllegalStateException("미리보기 페이지 응답 오류 (HTTP " + response.status() + ")");
            }
            if (!page.url().startsWith(url)) {
                // 로그인 페이지 등으로 리다이렉트된 경우 — 쿠키(세션)가 안 넘어간 것
                throw new IllegalStateException("미리보기 페이지가 다른 주소로 이동됨 : " + page.url());
            }

            // resume-preview.js 가 다 그리면 body[data-rendered='true'] 를 붙인다
            try {
                page.waitForSelector("body[data-rendered='true']",
                        new Page.WaitForSelectorOptions().setTimeout(RENDER_WAIT_TIMEOUT_MS));
            } catch (TimeoutError e) {
                log.warn("미리보기 렌더 완료 표시가 없어 현재 화면으로 PDF 생성 : {}", url);
            }

            // 웹폰트 로딩 대기 (최대 FONT_WAIT_TIMEOUT_MS)
            page.evaluate("ms => Promise.race(["
                    + "document.fonts.ready.then(() => true),"
                    + "new Promise(r => setTimeout(() => r(false), ms))])", FONT_WAIT_TIMEOUT_MS);

            page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.PRINT));
            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setPreferCSSPageSize(true)
                    .setMargin(new Margin().setTop("0").setRight("0").setBottom("0").setLeft("0")));
        }
    }

    /** "http://localhost:8080/ctx/web/..." → "http://localhost:8080" */
    private String originOf(String url) {
        URI uri = URI.create(url);
        return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
    }

    private void ensureBrowser() {
        if (browser != null && browser.isConnected()) return;

        closeQuietly();
        Path path = Paths.get(browsersPath).toAbsolutePath();
        log.info("PDF 용 Chromium 시작 (브라우저 경로: {})", path);

        Map<String, String> env = new HashMap<>();
        env.put("PLAYWRIGHT_BROWSERS_PATH", path.toString());
        if (!autoInstall) {
            env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");   // 운영 : 다운로드 금지
        }
        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(env));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
    }

    private void closeQuietly() {
        try { if (browser != null) browser.close(); } catch (Exception ignored) { }
        try { if (playwright != null) playwright.close(); } catch (Exception ignored) { }
        browser = null;
        playwright = null;
    }

    @PreDestroy
    public void shutdown() {
        try {
            worker.submit(this::closeQuietly).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("PDF 브라우저 종료 중 오류", e);
        } finally {
            worker.shutdownNow();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        worker.submit(() -> {
            try {
                ensureBrowser();
            } catch (Exception e) {
                log.warn("PDF 브라우저 미리 실행 실패 (첫 요청 때 다시 시도)", e);
            }
        });
    }
}
