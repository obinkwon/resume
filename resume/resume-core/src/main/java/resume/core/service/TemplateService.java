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
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.dto.TemplateResponseDto;
import resume.core.mapper.TemplateMapper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateMapper templateMapper;
    /** 페이지 로딩 + 렌더링 대기 */
    private static final double PAGE_TIMEOUT_MS = 30_000;
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
        long t0 = System.currentTimeMillis();
        ensureBrowser();
        long t1 = System.currentTimeMillis();

        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(794, 1123)
                .setLocale("ko-KR"))) {

            if (cookies != null && !cookies.isEmpty()) {
                context.addCookies(cookies);
            }

            Page page = context.newPage();
            page.setDefaultTimeout(PAGE_TIMEOUT_MS);

            // 느린 요청 찾기용 (1초 넘게 걸린 요청만 출력)
            page.onRequestFinished(req -> {
                double ms = req.timing().responseEnd;
                if (ms > 1000) log.info("느린 요청 {}ms : {}", (long) ms, req.url());
            });

            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD));
            long t2 = System.currentTimeMillis();

            page.waitForSelector("body[data-rendered='true']");
            long t3 = System.currentTimeMillis();

            page.evaluate("() => document.fonts.ready.then(() => true)");
            long t4 = System.currentTimeMillis();

            page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.PRINT));
            byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setPreferCSSPageSize(true)
                    .setMargin(new Margin().setTop("0").setRight("0").setBottom("0").setLeft("0")));
            long t5 = System.currentTimeMillis();

            log.info("PDF 시간 — 브라우저 {}ms / 페이지 로딩 {}ms / 렌더 대기 {}ms / 폰트 {}ms / PDF {}ms / 합계 {}ms",
                    t1 - t0, t2 - t1, t3 - t2, t4 - t3, t5 - t4, t5 - t0);
            return pdf;
        }
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
                long start = System.currentTimeMillis();
                ensureBrowser();
                log.info("PDF 브라우저 준비 완료 ({}ms)", System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("PDF 브라우저 미리 실행 실패 (첫 요청 때 다시 시도)", e);
            }
        });
    }
}
