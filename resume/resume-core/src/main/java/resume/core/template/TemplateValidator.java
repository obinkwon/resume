package resume.core.template;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 사용자 템플릿(HTML/CSS) 저장 전 검사.
 * - 치환자는 resume-preview.html 의 data-section 9개만 허용, 섹션마다 한 번만
 * - 치환자는 태그 사이에만 (속성 값 안 금지)
 * - script, iframe 등 위험 태그 / on* 이벤트 속성 / javascript: 금지
 */
@Component
public class TemplateValidator {

    public static final List<String> ALLOWED_SECTIONS = List.of(
            "profile", "intro", "cover", "experience", "project",
            "education", "certificate", "skill", "link");

    public static final int MAX_LENGTH = 100_000;
    public static final int MAX_NAME_LENGTH = 50;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([^}]*?)\\s*}}");
    private static final Pattern PLACEHOLDER_IN_TAG = Pattern.compile("<[^>]*\\{\\{[^}]*}}[^>]*>");
    private static final Pattern FORBIDDEN_TAG = Pattern.compile(
            "<\\s*(script|iframe|frame|frameset|object|embed|link|meta|base|form|input|button|textarea|select|style)[\\s>/]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_ATTR = Pattern.compile("<[^>]+\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_URL = Pattern.compile("(javascript|vbscript)\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_STYLE_CLOSE = Pattern.compile("</\\s*style", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_EXPRESSION = Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE);

    /** 문제가 있으면 IllegalArgumentException (메시지는 줄바꿈으로 구분) */
    public void validateOrThrow(String templateName, String html, String css) {
        List<String> errors = new ArrayList<>();

        String name = templateName == null ? "" : templateName.trim();
        if (name.isEmpty()) errors.add("템플릿 이름을 입력해 주세요.");
        else if (name.length() > MAX_NAME_LENGTH) errors.add("템플릿 이름은 " + MAX_NAME_LENGTH + "자까지 쓸 수 있습니다.");

        errors.addAll(validate(html, css));
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
    }

    public List<String> validate(String html, String css) {
        String h = html == null ? "" : html;
        String c = css == null ? "" : css;
        List<String> errors = new ArrayList<>();

        if (h.length() > MAX_LENGTH) errors.add("HTML 이 너무 깁니다. (최대 " + MAX_LENGTH + "자)");
        if (c.length() > MAX_LENGTH) errors.add("CSS 가 너무 깁니다. (최대 " + MAX_LENGTH + "자)");

        Set<String> used = new HashSet<>();
        Matcher m = PLACEHOLDER.matcher(h);
        while (m.find()) {
            String name = m.group(1);
            if (!ALLOWED_SECTIONS.contains(name)) {
                errors.add("없는 치환자입니다: {{" + name + "}}");
            } else if (!used.add(name)) {
                errors.add("{{" + name + "}} 를 두 번 이상 썼습니다.");
            }
        }

        if (PLACEHOLDER_IN_TAG.matcher(h).find()) {
            errors.add("치환자는 태그 안(속성 값)에 넣을 수 없습니다.");
        }

        Matcher tag = FORBIDDEN_TAG.matcher(h);
        Set<String> foundTags = new HashSet<>();
        while (tag.find()) {
            String t = tag.group(1).toLowerCase(Locale.ROOT);
            if (foundTags.add(t)) errors.add("<" + t + "> 태그는 쓸 수 없습니다.");
        }

        if (EVENT_ATTR.matcher(h).find()) errors.add("onclick 같은 이벤트 속성은 쓸 수 없습니다.");
        if (JS_URL.matcher(h).find() || JS_URL.matcher(c).find()) errors.add("javascript: 주소는 쓸 수 없습니다.");
        if (CSS_STYLE_CLOSE.matcher(c).find()) errors.add("CSS 에 </style> 은 쓸 수 없습니다.");
        if (CSS_EXPRESSION.matcher(c).find()) errors.add("CSS expression() 은 쓸 수 없습니다.");

        return errors;
    }
}