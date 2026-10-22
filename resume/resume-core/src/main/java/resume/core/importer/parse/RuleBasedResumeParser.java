package resume.core.importer.parse;

import org.springframework.stereotype.Component;
import resume.core.dto.ResumeImportResultDto;
import resume.core.dto.ResumeImportResultDto.Certificate;
import resume.core.dto.ResumeImportResultDto.CoverLetter;
import resume.core.dto.ResumeImportResultDto.Education;
import resume.core.dto.ResumeImportResultDto.Experience;
import resume.core.dto.ResumeImportResultDto.Link;
import resume.core.dto.ResumeImportResultDto.Profile;
import resume.core.dto.ResumeImportResultDto.Project;
import resume.core.dto.ResumeImportResultDto.Skill;
import resume.core.importer.ResumeFileType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 규칙 기반 이력서 파서 (한국어·영어 이력서)
 *
 *  1) 섹션 제목(경력, 학력, Experience …)으로 줄을 섹션별로 나눔
 *  2) 경력·학력·프로젝트는 '기간이 있는 줄'을 기준으로 항목을 나눔
 *  3) 자기소개서는 문항 제목(지원동기, [성장과정], 1. … 등)으로 항목을 나눔
 *
 * 자동으로 나눈 결과라 틀릴 수 있음 → 화면에서 사용자가 확인 후 저장하는 용도
 */
@Component
public class RuleBasedResumeParser implements ResumeParser {

    // ---------- DB 컬럼 길이 (넘으면 저장 시 오류가 나므로 자름) ----------
    private static final int LEN_NAME = 100, LEN_EMAIL = 100, LEN_PHONE = 30, LEN_ADDRESS = 255;
    private static final int LEN_COMPANY = 100, LEN_POSITION = 100;
    private static final int LEN_PROJECT = 150, LEN_ROLE = 100, LEN_URL = 255;
    private static final int LEN_SCHOOL = 150, LEN_MAJOR = 150, LEN_DEGREE = 100;
    private static final int LEN_CERT = 150, LEN_ISSUER = 150;
    private static final int LEN_SKILL = 100, LEN_CATEGORY = 50, LEN_LINK_NAME = 100, LEN_COVER_TITLE = 200;

    /** 섹션마다 최대 항목 수 */
    private static final int MAX_ITEMS = 30;
    private static final int MAX_SKILLS = 60;
    private static final int MAX_LINKS = 20;

    // ---------- 패턴 ----------
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)(?:\\+82[-\\s]?|0)(1[016789])[-.\\s)]*(\\d{3,4})[-.\\s]*(\\d{4})(?!\\d)");
    private static final Pattern URL = Pattern.compile(
            "(?i)(?:https?://|www\\.)[^\\s|<>\"'()\\[\\]]+"
          + "|\\b(?:github\\.com|gitlab\\.com|linkedin\\.com|velog\\.io|brunch\\.co\\.kr|medium\\.com|blog\\.naver\\.com"
          + "|[a-z0-9-]+\\.tistory\\.com|[a-z0-9-]+\\.github\\.io|[a-z0-9-]+\\.notion\\.site|notion\\.so)(?:/[^\\s|<>\"'()\\[\\]]*)?");

    /** 표 칸 / 2칸 이상 공백 / 앞뒤 띄운 슬래시 / 가운뎃점 으로 칸 나누기 */
    private static final Pattern TOKEN_SPLIT = Pattern.compile("\\s*[|ㅣ\\t·•ㆍ]\\s*|\\s{2,}|\\s+/\\s+");
    private static final Pattern SKILL_SPLIT = Pattern.compile("\\s*[,，、|·•ㆍ▪]\\s*|\\s+/\\s+");
    private static final Pattern LABEL_INLINE = Pattern.compile("^([^:：]{1,15}?)\\s*[:：]\\s*(.*)$");
    private static final Pattern DURATION = Pattern.compile(
            "[(（]?\\s*(?:총\\s*)?(?:\\d+\\s*년\\s*)?\\d+\\s*(?:개월|년)\\s*[)）]?|[(（]\\s*\\d+\\s*(?:yrs?|years?|mos?|months?)[^)）]*[)）]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EDGE_JUNK = Pattern.compile("^[\\s|:：,·\\-–~]+|[\\s|:：,·\\-–~]+$");
    /** 짝이 없는 가장자리 괄호, 빈 괄호 */
    private static final Pattern EDGE_PAREN = Pattern.compile("^[)）\\]]+|[(（\\[]+$|[(（]\\s*[)）]");
    private static final Pattern BULLET = Pattern.compile("^\\s*(?:[-–•·*●○▪◦■□▶►✓✔※]|\\d{1,2}[.)]\\s)");
    private static final Pattern SENTENCE_END = Pattern.compile(".*(?:다|요|니다|음|함|임|됨|[.!?。])[.!?。\"”’)]*$");
    private static final Pattern PARAGRAPH_END = Pattern.compile(".*(?:[.!?。…:：)」”\"]|다|요|음|함|임)$");
    private static final Pattern GPA = Pattern.compile("(?:학점|GPA|평점)?\\s*[:：]?\\s*\\d\\.\\d{1,2}\\s*/\\s*\\d\\.\\d{1,2}", Pattern.CASE_INSENSITIVE);

    private static final Pattern SCHOOL = Pattern.compile(".*(?:대학교|대학원|대학|고등학교|고교|학교|university|college|institute|school|academy).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCHOOL_SPLIT = Pattern.compile("^(.*?(?:대학교|대학원|고등학교|University|College))\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAJOR = Pattern.compile(".*(?:학과|학부|전공|공학|과학|과)$|.*\\b(?:major|engineering|science)\\b.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEGREE = Pattern.compile("(전문학사|학사|석사|박사|bachelor(?:'s)?|master(?:'s)?|ph\\.?\\s?d|doctorate)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EDU_STATUS = Pattern.compile("(졸업예정|졸업|재학|휴학|중퇴|수료|편입|graduated|enrolled)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISSUER = Pattern.compile(
            ".*(?:공단|협회|진흥원|위원회|재단|학회|협의회|센터|평가원|연구원|ETS|YBM|Oracle|Microsoft|AWS|Amazon|Google|Cisco|PMI|CompTIA|Linux|Adobe|대한상공회의소|상공회의소|한국).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COVER_MAX = Pattern.compile("[(（\\[]?\\s*(?:최대\\s*)?(\\d{1,2},\\d{3}|\\d{2,4})\\s*자\\s*(?:이내|이하|내외|제한|까지)?\\s*[)）\\]]?");
    private static final Pattern COVER_COUNTER = Pattern.compile("^\\s*(?:글자\\s*수|공백\\s*(?:포함|제외)|\\(\\s*공백).*");
    private static final Pattern COVER_PROMPT = Pattern.compile(".*(?:주세요|주십시오|하시오|하십시오|바랍니다|\\?)\\s*[.)）]?\\s*(?:[(（\\[].*[)）\\]])?\\s*$");
    private static final Pattern COVER_NUMBERED = Pattern.compile("^\\s*(?:Q\\s*)?\\d{1,2}\\s*[.)]\\s*\\S.*|^\\s*(?:문항|질문)\\s*\\d+.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern COVER_BRACKETED = Pattern.compile("^\\s*[\\[【<〈《].{1,60}[\\]】>〉》]\\s*$");
    private static final Pattern COVER_TITLE_CLEAN = Pattern.compile("^\\s*(?:Q\\s*)?\\d{1,2}\\s*[.)]\\s*|^\\s*(?:문항|질문)\\s*\\d+\\s*[.):：]?\\s*", Pattern.CASE_INSENSITIVE);

    private static final Set<String> LBL_NAME = Set.of("이름", "성명", "name", "성명한글", "한글성명", "한글이름");
    private static final Set<String> LBL_ADDRESS = Set.of("주소", "거주지", "현주소", "address", "거주지역", "주소지");
    private static final Set<String> LBL_COMPANY = Set.of("회사명", "회사", "기업명", "근무처", "company");
    private static final Set<String> LBL_POSITION = Set.of("직무", "직책", "직급", "직위", "포지션", "position", "담당직무", "title", "role");
    private static final Set<String> LBL_PROJECT = Set.of("프로젝트명", "프로젝트", "project");
    private static final Set<String> LBL_ROLE = Set.of("역할", "담당", "담당역할", "담당업무", "role", "포지션", "position");
    private static final Set<String> LBL_TECH = Set.of("사용기술", "기술스택", "기술", "사용언어", "개발환경", "환경", "techstack",
            "tech", "stack", "skills", "skill", "기술환경", "사용툴", "tools", "언어", "사용기술스택");
    private static final Set<String> LBL_URL = Set.of("url", "링크", "데모", "시연", "github", "깃허브", "배포", "주소", "link",
            "demo", "시연영상", "배포주소", "repository", "repo", "저장소");
    private static final Set<String> LBL_MAJOR = Set.of("전공", "학과", "major");
    private static final Set<String> LBL_ISSUER = Set.of("발급기관", "발행처", "발행기관", "시행기관", "주관", "issuer");

    @Override
    public ResumeImportResultDto parse(String text, ResumeFileType fileType) {
        // PDF 는 화면 폭에서 줄이 바뀌므로 문장 중간 줄바꿈을 이어 붙임
        boolean wrapped = fileType == ResumeFileType.PDF;
        Map<Section, List<String>> sections = splitSections(text);

        Profile profile = parseProfile(text, sections, wrapped);
        List<Link> links = parseLinks(concat(sections, Section.HEAD, Section.PROFILE, Section.INTRO, Section.LINK));
        List<Skill> skills = parseSkills(lines(sections, Section.SKILL));
        List<Experience> experiences = parseExperiences(lines(sections, Section.EXPERIENCE), wrapped);
        List<Project> projects = parseProjects(lines(sections, Section.PROJECT), wrapped);
        List<Education> educations = parseEducations(lines(sections, Section.EDUCATION), wrapped);
        List<Certificate> certificates = parseCertificates(lines(sections, Section.CERTIFICATE));
        List<CoverLetter> coverLetters = parseCoverLetters(lines(sections, Section.COVER), wrapped);

        List<String> warnings = new ArrayList<>();
        boolean noSections = skills.isEmpty() && experiences.isEmpty() && projects.isEmpty()
                && educations.isEmpty() && certificates.isEmpty() && coverLetters.isEmpty()
                && profile.introduction() == null;
        if (noSections) {
            warnings.add("경력·학력 같은 섹션 제목을 찾지 못해서 인적 사항만 가져왔어요. 나머지는 직접 입력해 주세요.");
        }
        long noPosition = experiences.stream().filter(e -> e.position() == null).count();
        if (noPosition > 0) {
            warnings.add("직무를 찾지 못한 경력이 " + noPosition + "개 있어요. 저장 전에 직무를 입력해 주세요.");
        }

        return new ResumeImportResultDto(fileType.getExtension(), profile, links, skills, educations,
                certificates, experiences, projects, coverLetters, warnings);
    }

    // =========================================================
    // 1) 섹션 나누기
    // =========================================================

    private Map<Section, List<String>> splitSections(String text) {
        Map<Section, List<String>> map = new EnumMap<>(Section.class);
        Section current = Section.HEAD;

        for (String raw : text.split("\n", -1)) {
            String line = raw.strip();
            if (line.isEmpty()) {
                map.computeIfAbsent(current, k -> new ArrayList<>()).add("");
                continue;
            }
            Section heading = Section.heading(line);
            if (heading != null) {
                current = heading;
                continue;
            }
            // 표 첫 칸이 섹션 제목인 경우 : "경력 | 총 3년 2개월"
            int bar = line.indexOf(" | ");
            if (bar > 0) {
                Section first = Section.heading(line.substring(0, bar));
                String rest = line.substring(bar + 3);
                if (first != null && DURATION.matcher(rest).replaceAll("").replaceAll("[|\\s]", "").length() <= 2) {
                    current = first;
                    continue;
                }
            }
            // 섹션 제목 없이 자기소개서 문항이 바로 나오는 경우 : "성장과정"
            if (current != Section.COVER && isStrongCoverTitle(line)) {
                current = Section.COVER;
            }
            if (current != Section.COVER && isHeaderRow(line)) continue;
            map.computeIfAbsent(current, k -> new ArrayList<>()).add(line);
        }
        return map;
    }

    private static List<String> lines(Map<Section, List<String>> map, Section s) {
        return map.getOrDefault(s, List.of());
    }

    private static List<String> concat(Map<Section, List<String>> map, Section... keys) {
        List<String> all = new ArrayList<>();
        for (Section s : keys) all.addAll(lines(map, s));
        return all;
    }

    /** "회사명 | 기간 | 직무" 같은 표 머리 행 */
    private static boolean isHeaderRow(String line) {
        List<String> cells = Arrays.stream(line.split("\\s*\\|\\s*")).filter(c -> !c.isBlank()).toList();
        if (cells.size() < 2) return false;
        return cells.stream().allMatch(c -> Section.HEADER_WORDS.contains(labelKey(c)));
    }

    // =========================================================
    // 2) 인적 사항 · 링크
    // =========================================================

    private Profile parseProfile(String text, Map<Section, List<String>> sections, boolean wrapped) {
        List<String> head = concat(sections, Section.HEAD, Section.PROFILE);

        String email = firstMatch(EMAIL, text);
        String phone = null;
        Matcher pm = MOBILE.matcher(text);
        if (pm.find()) phone = "01" + pm.group(1).substring(1) + "-" + pm.group(2) + "-" + pm.group(3);

        String name = labeled(head, LBL_NAME);
        if (name != null) {
            Matcher ko = Pattern.compile("[가-힣]{2,5}").matcher(name);
            name = ko.find() ? ko.group() : name;
        } else {
            name = guessName(head);
        }

        String address = labeled(head, LBL_ADDRESS);
        List<String> introLines = lines(sections, Section.INTRO).stream()
                .filter(l -> l.isEmpty() || !isUrlOnly(l)).toList();
        String intro = joinText(introLines, wrapped);

        return new Profile(cut(name, LEN_NAME), cut(email, LEN_EMAIL), cut(phone, LEN_PHONE),
                cut(address, LEN_ADDRESS), blankToNull(intro));
    }

    /** 맨 위 몇 줄 중 '홍길동' / '홍 길 동' / '홍길동 (Hong Gildong)' 같은 줄 */
    private String guessName(List<String> head) {
        int checked = 0;
        for (String line : head) {
            if (line.isEmpty()) continue;
            if (++checked > 6) break;
            if (line.contains("이력서") || EMAIL.matcher(line).find()) continue;
            String compact = line.replace(" ", "");
            if (compact.matches("[가-힣]{2,4}")) return compact;
            Matcher m = Pattern.compile("^([가-힣]{2,4})\\s*[(/|]").matcher(line);
            if (m.find()) return m.group(1);
            if (line.matches("[A-Z][a-z]+(?: [A-Z][a-z]+){1,2}")) return line;
        }
        return null;
    }

    private List<Link> parseLinks(List<String> lines) {
        Map<String, Link> links = new LinkedHashMap<>();
        for (String line : lines) {
            Matcher m = URL.matcher(line);
            while (m.find() && links.size() < MAX_LINKS) {
                if (m.start() > 0 && line.charAt(m.start() - 1) == '@') continue;    // 이메일 주소의 도메인
                String url = normalizeUrl(m.group());
                if (url.length() > LEN_URL) continue;
                String key = url.toLowerCase(Locale.ROOT).replaceAll("/+$", "");
                if (links.containsKey(key)) continue;

                // "GitHub : https://…" → 표시 이름 'GitHub'
                String before = trimEdges(line.substring(0, m.start()));
                String displayName = !before.isEmpty() && before.length() <= 20 ? before : null;
                links.put(key, new Link(linkType(url), cut(displayName, LEN_LINK_NAME), url));
            }
        }
        return new ArrayList<>(links.values());
    }

    private static String normalizeUrl(String raw) {
        String url = raw.replaceAll("[.,;:!?。]+$", "");
        return url.matches("(?i)^https?://.*") ? url : "https://" + url;
    }

    /** resume-form.js 의 LINK_TYPES 와 같은 값 */
    private static String linkType(String url) {
        String u = url.toLowerCase(Locale.ROOT);
        if (u.contains("github.com") || u.contains("github.io") || u.contains("gitlab.com")) return "github";
        if (u.contains("linkedin.com")) return "linkedin";
        if (u.contains("velog.io") || u.contains("tistory.com") || u.contains("blog.naver.com")
                || u.contains("medium.com") || u.contains("brunch.co.kr") || u.contains("/blog")) return "blog";
        if (u.contains("notion.site") || u.contains("notion.so") || u.contains("portfolio")) return "portfolio";
        return "website";
    }

    // =========================================================
    // 3) 기술 스택
    // =========================================================

    private List<Skill> parseSkills(List<String> lines) {
        Map<String, Skill> skills = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.isEmpty()) continue;
            if (line.length() > 25 && SENTENCE_END.matcher(line).matches() && !line.contains(",")) continue;

            String category = null;
            String value = line;
            Matcher lm = LABEL_INLINE.matcher(line);
            String[] cells = line.split("\\s*\\|\\s*");
            if (lm.matches() && !lm.group(1).matches("(?i).*https?.*") && !lm.group(2).isBlank()) {
                category = lm.group(1).strip();                      // "Backend : Java, Spring"
                value = lm.group(2);
            } else if (cells.length == 2 && cells[1].contains(",")) {
                category = cells[0].strip();                         // "Backend | Java, Spring"
                value = cells[1];
            }

            for (String part : SKILL_SPLIT.split(value)) {
                List<String> words = part.length() > 30 && part.contains(" ") && !part.matches(".*[가-힣].*")
                        ? Arrays.asList(part.split("\\s+"))          // "Java Spring JPA MySQL" (태그가 한 줄로 나온 경우)
                        : List.of(part);
                for (String w : mergeKnownPairs(words)) {
                    addSkill(skills, w, category);
                    if (skills.size() >= MAX_SKILLS) return new ArrayList<>(skills.values());
                }
            }
        }
        return new ArrayList<>(skills.values());
    }

    /** 공백으로 나눈 기술 이름 중 두 단어짜리 ("Spring Boot") 다시 합치기 */
    private static final Set<String> SKILL_PAIRS = Set.of(
            "spring boot", "spring cloud", "spring batch", "spring security", "spring data", "spring mvc",
            "react native", "google cloud", "github actions", "gitlab ci", "sql server", "ms sql", "apache kafka",
            "machine learning", "deep learning", "node js", "vue js", "react query", "power bi", "unreal engine",
            "adobe xd", "after effects", "premiere pro");

    private static List<String> mergeKnownPairs(List<String> words) {
        if (words.size() < 2) return words;
        List<String> out = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            if (i + 1 < words.size() && SKILL_PAIRS.contains((words.get(i) + " " + words.get(i + 1)).toLowerCase(Locale.ROOT))) {
                out.add(words.get(i) + " " + words.get(i + 1));
                i++;
            } else {
                out.add(words.get(i));
            }
        }
        return out;
    }

    private void addSkill(Map<String, Skill> skills, String raw, String category) {
        String s = BULLET.matcher(raw).replaceAll("").strip();
        Integer level = null;
        Matcher lv = Pattern.compile("[(（\\[]\\s*(상|중|하|고급|중급|초급|上|中|下)\\s*[)）\\]]|\\s(고급|중급|초급|advanced|intermediate|beginner)$",
                Pattern.CASE_INSENSITIVE).matcher(s);
        if (lv.find()) {
            String w = (lv.group(1) != null ? lv.group(1) : lv.group(2)).toLowerCase(Locale.ROOT);
            level = switch (w) {
                case "상", "고급", "上", "advanced" -> 5;
                case "중", "중급", "中", "intermediate" -> 3;
                default -> 1;
            };
            s = (s.substring(0, lv.start()) + s.substring(lv.end())).strip();
        }
        s = trimEdges(s);
        if (s.isEmpty() || s.length() > 40 || s.matches("\\d+") || SENTENCE_END.matcher(s).matches() && s.length() > 15) return;

        String key = s.toLowerCase(Locale.ROOT);
        if (!skills.containsKey(key)) {
            skills.put(key, new Skill(cut(s, LEN_SKILL), cut(category, LEN_CATEGORY), level));
        }
    }

    // =========================================================
    // 4) 경력 · 프로젝트 · 학력 — 기간이 있는 줄로 항목 나누기
    // =========================================================

    /** header = 제목 줄 (기간을 뺀 나머지), body = 그 아래 줄들 */
    private record Block(String header, Period period, List<String> body) {
    }

    private List<Block> splitBlocks(List<String> raw, boolean allowSingleDate) {
        List<String> lines = raw.stream().filter(l -> !l.isEmpty()).toList();
        if (lines.isEmpty()) return List.of();

        List<Integer> idx = new ArrayList<>();
        List<Period> periods = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Period p = Period.find(lines.get(i), allowSingleDate);
            // 긴 설명 문장 안의 날짜는 항목 시작으로 보지 않음
            if (p != null && (lines.get(i).length() <= 80 || !SENTENCE_END.matcher(lines.get(i)).matches())) {
                idx.add(i);
                periods.add(p);
            }
        }
        if (idx.isEmpty()) {
            // 기간이 하나도 없으면 첫 줄을 제목으로 하는 항목 하나
            return List.of(new Block(lines.get(0), null, new ArrayList<>(lines.subList(1, lines.size()))));
        }

        int n = idx.size();
        int[] starts = new int[n];
        String[] rests = new String[n];
        for (int k = 0; k < n; k++) {
            int i = idx.get(k);
            rests[k] = cleanRest(periods.get(k).removeFrom(lines.get(i)));
            int prevLimit = k == 0 ? -1 : idx.get(k - 1);
            int start = i;
            // 기간만 있는 줄이면 바로 위 1~2줄(짧은 제목 줄)을 제목으로
            if (rests[k].isEmpty()) {
                for (int back = 1; back <= 2; back++) {
                    int j = i - back;
                    if (j <= prevLimit || !isTitleLike(lines.get(j))) break;
                    start = j;
                }
            }
            starts[k] = start;
        }

        List<Block> blocks = new ArrayList<>();
        for (int k = 0; k < n && blocks.size() < MAX_ITEMS; k++) {
            int i = idx.get(k);
            int end = k + 1 < n ? starts[k + 1] : lines.size();

            List<String> head = new ArrayList<>();
            if (k == 0 && starts[0] > 0 && starts[0] <= 2) {
                // 섹션 맨 위의 짧은 줄 (예: 회사명) 이 남았으면 첫 항목 제목에 붙임
                List<String> lead = lines.subList(0, starts[0]);
                if (lead.stream().allMatch(this::isTitleLike)) head.addAll(lead);
            }
            head.addAll(lines.subList(starts[k], i));
            if (!rests[k].isEmpty()) head.add(rests[k]);

            List<String> body = new ArrayList<>(lines.subList(Math.min(i + 1, end), Math.max(i + 1, end)));
            blocks.add(new Block(String.join(" | ", head), periods.get(k), body));
        }
        return blocks;
    }

    private static String cleanRest(String s) {
        String r = DURATION.matcher(s).replaceAll(" ");
        r = r.replaceAll("(?i)(?:재직|근무|재학|활동|진행)?\\s*기간\\s*[:：]?", " ");
        r = r.replaceAll("\\s*\\|\\s*(?:\\|\\s*)+", " | ");
        return trimEdges(r.strip());
    }

    private boolean isTitleLike(String s) {
        int max = s.contains(" | ") ? 100 : 40;        // 표 행은 칸이 여러 개라 더 길어도 제목
        return !s.isEmpty() && s.length() <= max && !BULLET.matcher(s).find()
                && !SENTENCE_END.matcher(s).matches() && Period.find(s, true) == null && !URL.matcher(s).find();
    }

    private static String trimEdges(String s) {
        String v = s;
        for (int i = 0; i < 3; i++) {
            String next = EDGE_PAREN.matcher(EDGE_JUNK.matcher(v).replaceAll("")).replaceAll("").strip();
            if (next.equals(v)) break;
            v = next;
        }
        return v;
    }

    private static List<String> tokens(String header) {
        List<String> list = new ArrayList<>();
        for (String t : TOKEN_SPLIT.split(header)) {
            String v = trimEdges(DURATION.matcher(t).replaceAll(""));
            if (v.isEmpty() || Section.HEADER_WORDS.contains(labelKey(v))) continue;
            list.add(v);
        }
        return list;
    }

    private List<Experience> parseExperiences(List<String> lines, boolean wrapped) {
        List<Experience> list = new ArrayList<>();
        for (Block b : splitBlocks(lines, false)) {
            List<String> t = tokens(b.header());
            List<String> body = new ArrayList<>(b.body());

            String company = takeLabeled(body, LBL_COMPANY, LEN_COMPANY);
            if (company == null && !t.isEmpty()) company = t.remove(0);
            if (company == null) continue;

            String position = takeLabeled(body, LBL_POSITION, 50);
            if (position == null && !t.isEmpty()) position = t.remove(0);
            if (position == null && body.size() > 1 && isTitleLike(body.get(0))) position = body.remove(0);
            body.addAll(0, t);                                      // 남은 칸 (예: 표의 담당업무)

            Period p = b.period();
            boolean current = p != null && p.current();
            list.add(new Experience(cut(company, LEN_COMPANY), cut(position, LEN_POSITION),
                    p == null ? null : p.start(), current || p == null ? null : p.end(), current,
                    blankToNull(joinText(body, wrapped))));
        }
        return list;
    }

    private List<Project> parseProjects(List<String> lines, boolean wrapped) {
        List<Project> list = new ArrayList<>();
        for (Block b : splitBlocks(lines, false)) {
            List<String> t = tokens(b.header());
            List<String> body = new ArrayList<>(b.body());

            String name = takeLabeled(body, LBL_PROJECT, LEN_PROJECT);
            if (name == null && !t.isEmpty()) name = t.remove(0);
            if (name == null) continue;

            String role = takeLabeled(body, LBL_ROLE, 60);
            if (role == null && !t.isEmpty()) role = String.join(" · ", t);

            String tech = takeLabeled(body, LBL_TECH, 500);
            if (tech != null) tech = String.join(", ", SKILL_SPLIT.split(tech.strip()));

            String url = takeLabeled(body, LBL_URL, LEN_URL);
            url = url == null ? null : firstMatch(URL, url);
            if (url == null) {
                // 제목 줄이나 본문에 있는 첫 URL (URL 만 있는 줄은 설명에서 뺌)
                url = firstMatch(URL, b.header());
                for (int i = 0; url == null && i < body.size(); i++) {
                    url = firstMatch(URL, body.get(i));
                    if (url != null && isUrlOnly(body.get(i))) body.remove(i);
                }
            }
            if (url != null) url = normalizeUrl(url);

            Period p = b.period();
            list.add(new Project(cut(name, LEN_PROJECT), p == null ? null : p.start(),
                    p == null || p.current() ? null : p.end(), cut(role, LEN_ROLE),
                    blankToNull(joinText(body, wrapped)), tech, url != null && url.length() <= LEN_URL ? url : null));
        }
        return list;
    }

    private List<Education> parseEducations(List<String> lines, boolean wrapped) {
        List<Education> list = new ArrayList<>();
        for (Block b : splitBlocks(lines, true)) {
            List<String> body = new ArrayList<>(b.body());
            List<String> extra = new ArrayList<>();

            String header = b.header();
            Matcher gpa = GPA.matcher(header);
            if (gpa.find()) {
                extra.add(gpa.group().strip());
                header = header.substring(0, gpa.start()) + " " + header.substring(gpa.end());
            }

            // 학위·졸업 상태는 칸에 붙어 있어도 떼어냄 ("컴퓨터공학과 학사")
            String degree = null;
            String status = null;
            List<String> t = new ArrayList<>();
            for (String tok : tokens(header)) {
                Matcher dm = DEGREE.matcher(tok);
                if (degree == null && dm.find()) {
                    degree = dm.group(1);
                    tok = (tok.substring(0, dm.start()) + tok.substring(dm.end())).strip();
                }
                Matcher sm = EDU_STATUS.matcher(tok);
                if (status == null && sm.find()) {
                    status = sm.group(1);
                    tok = (tok.substring(0, sm.start()) + tok.substring(sm.end())).strip();
                }
                tok = trimEdges(tok);
                if (tok.isEmpty()) continue;
                // "한국대학교 컴퓨터공학과" → 학교 / 전공 으로 나눔
                Matcher sp = SCHOOL_SPLIT.matcher(tok);
                if (sp.matches()) {
                    t.add(sp.group(1).strip());
                    t.add(sp.group(2).strip());
                } else {
                    t.add(tok);
                }
            }

            String school = t.stream().filter(x -> SCHOOL.matcher(x).matches()).findFirst().orElse(t.isEmpty() ? null : t.get(0));
            if (school == null) continue;
            t.remove(school);

            String major = takeLabeled(body, LBL_MAJOR, LEN_MAJOR);
            if (major == null) {
                major = t.stream().filter(x -> MAJOR.matcher(x).matches()).findFirst().orElse(t.isEmpty() ? null : t.get(0));
            }
            if (major != null) t.remove(major);
            extra.addAll(t);

            if (degree == null) {
                // 본문 첫 줄에 학위가 있는 경우
                for (String l : body) {
                    Matcher dm = DEGREE.matcher(l);
                    if (l.length() <= 40 && dm.find()) {
                        degree = dm.group(1);
                        break;
                    }
                }
            }

            String desc = joinText(body, wrapped);
            if (!extra.isEmpty()) desc = String.join(" · ", extra) + (desc.isEmpty() ? "" : "\n" + desc);

            Period p = b.period();
            String start = null;
            String end = null;
            if (p != null) {
                if (p.end() == null && !p.current() && !b.header().contains("~")) {
                    end = p.start();                                // 날짜 하나 = 졸업일 ("2020.02 졸업")
                } else {
                    start = p.start();
                    end = p.current() ? null : p.end();
                }
            }
            list.add(new Education(cut(school, LEN_SCHOOL), cut(major, LEN_MAJOR),
                    cut(degree != null ? degree : status, LEN_DEGREE), start, end, blankToNull(desc)));
        }
        return list;
    }

    // =========================================================
    // 5) 자격증 — 한 줄에 하나
    // =========================================================

    private List<Certificate> parseCertificates(List<String> lines) {
        List<Certificate> list = new ArrayList<>();
        for (String line : lines) {
            if (line.isEmpty()) continue;

            Matcher lm = LABEL_INLINE.matcher(line);
            if (lm.matches() && LBL_ISSUER.contains(labelKey(lm.group(1))) && !list.isEmpty()) {
                Certificate last = list.remove(list.size() - 1);       // "발급기관 : 한국산업인력공단" → 앞 자격증에
                list.add(new Certificate(last.certificateName(), cut(lm.group(2).strip(), LEN_ISSUER), last.issuedDate(), last.description()));
                continue;
            }

            Period p = Period.find(line, true);
            String rest = p == null ? line : cleanRest(p.removeFrom(line));
            rest = BULLET.matcher(rest).replaceAll("").strip();

            if (rest.length() > 60 && !list.isEmpty()) {
                Certificate last = list.remove(list.size() - 1);       // 긴 줄 = 앞 자격증 설명
                String desc = last.description() == null ? rest : last.description() + "\n" + rest;
                list.add(new Certificate(last.certificateName(), last.issuer(), last.issuedDate(), desc));
                continue;
            }

            List<String> t = tokens(rest);
            if (t.isEmpty()) continue;
            String name = t.remove(0);
            String issuer = null;
            for (int i = 0; i < t.size(); i++) {
                if (ISSUER.matcher(t.get(i)).matches()) {
                    issuer = t.remove(i);
                    break;
                }
            }
            if (issuer == null && name.contains(" ")) {
                // 공백으로만 이어진 경우 : "정보처리기사 한국산업인력공단" / "TOEIC 900점 ETS"
                String[] words = name.split("\\s+");
                for (int i = 1; i < words.length; i++) {
                    if (ISSUER.matcher(words[i]).matches()) {
                        issuer = words[i];
                        List<String> after = Arrays.asList(words).subList(i + 1, words.length);
                        t.addAll(0, after);
                        name = String.join(" ", Arrays.asList(words).subList(0, i));
                        break;
                    }
                }
            }
            list.add(new Certificate(cut(name, LEN_CERT), cut(issuer, LEN_ISSUER),
                    p == null ? null : p.start(), t.isEmpty() ? null : String.join(" · ", t)));
            if (list.size() >= MAX_ITEMS) break;
        }
        return list;
    }

    // =========================================================
    // 6) 자기소개서 — 문항 제목으로 나누기
    // =========================================================

    private List<CoverLetter> parseCoverLetters(List<String> lines, boolean wrapped) {
        List<CoverLetter> list = new ArrayList<>();
        String title = null;
        Integer max = null;
        List<String> buf = new ArrayList<>();

        for (String line : lines) {
            if (line.isEmpty()) {
                buf.add("");
                continue;
            }
            if (COVER_COUNTER.matcher(line).matches()) continue;

            // "지원동기 : 저는 …" (제목과 내용이 한 줄)
            Matcher inline = LABEL_INLINE.matcher(line);
            if (inline.matches() && inline.group(2).length() >= 10 && hasCoverKeyword(Section.key(inline.group(1)))) {
                addCover(list, title, max, buf, wrapped);
                title = inline.group(1).strip();
                max = null;
                buf.add(inline.group(2).strip());
                continue;
            }

            if (isCoverTitle(line)) {
                addCover(list, title, max, buf, wrapped);
                Matcher mm = COVER_MAX.matcher(line);
                String t = line;
                max = null;
                if (mm.find()) {
                    max = Integer.parseInt(mm.group(1).replace(",", ""));
                    t = line.substring(0, mm.start()) + line.substring(mm.end());
                }
                title = cleanCoverTitle(t);
                continue;
            }
            buf.add(line);
        }
        addCover(list, title, max, buf, wrapped);
        return list;
    }

    private void addCover(List<CoverLetter> list, String title, Integer max, List<String> buf, boolean wrapped) {
        String content = joinText(buf, wrapped);
        buf.clear();
        if (content.isEmpty() || list.size() >= MAX_ITEMS) return;
        String t = title == null || title.isBlank() ? "자기소개서" : title;
        list.add(new CoverLetter(cut(t, LEN_COVER_TITLE), content, max));
    }

    private boolean isCoverTitle(String line) {
        if (line.length() > 200) return false;
        String k = Section.key(line);
        // 기업 공채 문항 : "…에 대해 기술해 주십시오. (500자)"
        if (COVER_PROMPT.matcher(line).matches() && line.length() <= 200 && !BULLET.matcher(line).find()) return true;
        if (line.length() > 60 || SENTENCE_END.matcher(line).matches()) return false;
        if (COVER_BRACKETED.matcher(line).matches()) return true;
        if (k.length() <= 30 && hasCoverKeyword(k)) return true;
        return COVER_NUMBERED.matcher(line).matches() && line.length() <= 40;
    }

    private boolean isStrongCoverTitle(String line) {
        if (line.length() > 30 || SENTENCE_END.matcher(line).matches()) return false;
        String k = Section.key(COVER_MAX.matcher(line).replaceAll(""));
        return Section.STRONG_COVER_TITLES.contains(k);
    }

    private static boolean hasCoverKeyword(String key) {
        return Section.COVER_KEYWORDS.stream().anyMatch(key::contains);
    }

    private static String cleanCoverTitle(String s) {
        String t = COVER_TITLE_CLEAN.matcher(s.strip()).replaceAll("");
        t = t.replaceAll("^[\\[【<〈《(]+|[\\]】>〉》)]+$", "").replaceAll("[:：]\\s*$", "");
        return t.strip();
    }

    // =========================================================
    // 공통
    // =========================================================

    /**
     * 여러 줄을 글로 합치기
     *  - 빈 줄, 글머리표, 문장이 끝난 줄 다음은 줄바꿈
     *  - PDF(wrapped) 는 그 밖의 줄바꿈을 공백으로 이어 붙임 (화면 폭 때문에 나뉜 줄)
     */
    private static String joinText(List<String> lines, boolean wrapped) {
        StringBuilder sb = new StringBuilder();
        boolean pendingBreak = false;
        String prev = null;
        for (String line : lines) {
            if (line.isEmpty()) {
                pendingBreak = true;
                continue;
            }
            if (sb.length() == 0) {
                sb.append(line);
            } else if (pendingBreak || !wrapped || BULLET.matcher(line).find() || PARAGRAPH_END.matcher(prev).matches()) {
                sb.append('\n').append(line);
            } else {
                sb.append(' ').append(line);
            }
            prev = line;
            pendingBreak = false;
        }
        return sb.toString().strip();
    }

    /**
     * 'label : 값' 또는 표 칸 'label | 값' 에서 값 찾기
     */
    private static String labeled(List<String> lines, Set<String> labels) {
        for (String line : lines) {
            if (line.isEmpty()) continue;
            String[] cells = line.split("\\s*\\|\\s*");
            for (int i = 0; i < cells.length; i++) {
                String c = cells[i].strip();
                Matcher m = LABEL_INLINE.matcher(c);
                if (m.matches() && labels.contains(labelKey(m.group(1))) && !m.group(2).isBlank()) {
                    return m.group(2).strip();
                }
                if (labels.contains(labelKey(c)) && i + 1 < cells.length && !cells[i + 1].isBlank()) {
                    return cells[i + 1].strip();
                }
            }
        }
        return null;
    }

    /**
     * 본문에서 'label : 값' 줄을 찾아 값을 꺼내고 그 줄은 지움
     *  - 'label' 만 있는 줄이면 다음 줄을 값으로
     *  - 값이 maxLen 보다 길면 설명으로 보고 그대로 둠
     */
    private static String takeLabeled(List<String> body, Set<String> labels, int maxLen) {
        for (int i = 0; i < body.size(); i++) {
            String line = BULLET.matcher(body.get(i)).replaceAll("").strip();
            Matcher m = LABEL_INLINE.matcher(line);
            if (m.matches() && labels.contains(labelKey(m.group(1)))) {
                String v = m.group(2).strip();
                if (v.isEmpty() && i + 1 < body.size() && !body.get(i + 1).isEmpty() && body.get(i + 1).length() <= maxLen) {
                    v = body.get(i + 1).strip();
                    body.remove(i + 1);
                }
                if (v.isEmpty() || v.length() > maxLen) continue;
                body.remove(i);
                return v;
            }
        }
        return null;
    }

    private static String labelKey(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[\\s()（）\\[\\]·*•\\-]", "");
    }

    private static boolean isUrlOnly(String line) {
        Matcher m = URL.matcher(line);
        if (!m.find()) return false;
        String rest = (line.substring(0, m.start()) + line.substring(m.end())).replaceAll("[\\s:：|\\-]", "");
        return rest.length() <= 12;     // "GitHub :" 정도는 URL 만 있는 줄로 봄
    }

    private static String firstMatch(Pattern p, String s) {
        if (s == null) return null;
        Matcher m = p.matcher(s);
        return m.find() ? m.group() : null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        String v = s.strip();
        if (v.isEmpty()) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
