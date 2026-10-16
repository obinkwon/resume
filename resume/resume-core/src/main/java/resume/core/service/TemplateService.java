package resume.core.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.Media;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import resume.core.dto.TemplateDetailResponseDto;
import resume.core.dto.TemplateRequestDto;
import resume.core.dto.TemplateResponseDto;
import resume.core.mapper.TemplateMapper;

import java.util.List;
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

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "resume-pdf");
        t.setDaemon(true);
        return t;
    });

    // ↓ worker 스레드에서만 사용
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
        ensureBrowser();

        // 요청마다 새 컨텍스트 = 쿠키·캐시가 다른 사용자와 섞이지 않음
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(794, 1123)          // A4 @96dpi (미리보기와 같은 폭)
                .setLocale("ko-KR"))) {

            if (cookies != null && !cookies.isEmpty()) {
                context.addCookies(cookies);
            }

            Page page = context.newPage();
            page.setDefaultTimeout(PAGE_TIMEOUT_MS);

            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            // resume-preview.js 가 템플릿을 다 그리면 body[data-rendered="true"]
            page.waitForSelector("body[data-rendered='true']");
            // 웹폰트(Pretendard, Noto Sans KR)까지 다 받은 뒤에 출력
            page.evaluate("() => document.fonts.ready.then(() => true)");

            page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.PRINT));

            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)        // 색 헤더 등 배경색 포함
                    .setPreferCSSPageSize(true)      // resume-preview.css 의 @page 설정 사용
                    .setMargin(new Margin().setTop("0").setRight("0").setBottom("0").setLeft("0")));
        }
    }

    private void ensureBrowser() {
        if (browser != null && browser.isConnected()) return;

        closeQuietly();
        log.info("PDF 용 Chromium 시작");
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                // Docker·리눅스 서버에서 권한 문제로 안 뜰 때 필요
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
}
