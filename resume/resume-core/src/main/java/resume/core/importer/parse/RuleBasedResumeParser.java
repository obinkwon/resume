package resume.core.importer.parse;

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
 * 규칙 기반 이력서 파서의 공통 기반 (한국어·영어 이력서)
 *
 *  1) 섹션 제목(경력, 학력, Experience …)으로 줄을 섹션별로 나눔
 *  2) 경력·학력·프로젝트는 '기간이 있는 줄'을 기준으로 항목을 나눔
 *  3) 자기소개서는 문항 제목(지원동기, [성장과정], 1. … 등)으로 항목을 나눔
 *
 * 구현
 *  - GenericResumeParser  : 어느 사이트인지 모를 때 (넓게 받음)
 *  - JobKoreaResumeParser : 잡코리아 PDF 전용 (아래 '사이트별로 바꾸는 부분' 을 덮어씀)
 *  - 어느 파서를 쓸지는 ResumeParserRouter 가 정함
 *
 * 자동으로 나눈 결과라 틀릴 수 있음 → 화면에서 사용자가 확인 후 저장하는 용도
 */
public abstract class RuleBasedResumeParser implements ResumeParser {

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
    protected static final Pattern LABEL_INLINE = Pattern.compile("^([^:：]{1,15}?)\\s*[:：]\\s*(.*)$");
    private static final Pattern DURATION = Pattern.compile(
            "[(（]?\\s*(?:총\\s*)?(?:\\d+\\s*년\\s*)?\\d+\\s*(?:개월|년)\\s*[)）]?|[(（]\\s*\\d+\\s*(?:yrs?|years?|mos?|months?)[^)）]*[)）]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EDGE_JUNK = Pattern.compile("^[\\s|:：,·\\-–~]+|[\\s|:：,·\\-–~]+$");
    /** 짝이 없는 가장자리 괄호, 빈 괄호 */
    private static final Pattern EDGE_PAREN = Pattern.compile("^[)）\\]]+|[(（\\[]+$|[(（]\\s*[)）]");
    private static final Pattern BULLET = Pattern.compile("^\\s*(?:[-–•·*●○▪◦■□▶►✓✔※]|\\d{1,2}[.)]\\s)");
    private static final Pattern SENTENCE_END = Pattern.compile(".*(?:다|요|니다|음|함|임|됨|[.!?。])[.!?。\"”’)]*$");
    /** 문단 끝 : 마침표류 또는 '…다' ('요·음·함' 은 글자 단위 줄바꿈과 헷갈려서 마침표가 있을 때만) */
    protected static final Pattern PARAGRAPH_END = Pattern.compile(".*(?:[.!?。…:：)」”\"]|다)$");
    private static final Pattern GPA = Pattern.compile("(?:학점|GPA|평점)?\\s*[:：]?\\s*\\d\\.\\d{1,2}\\s*/\\s*\\d\\.\\d{1,2}", Pattern.CASE_INSENSITIVE);

    protected static final Pattern SCHOOL = Pattern.compile(".*(?:대학교|대학원|대학|고등학교|고교|학교|학점은행제|검정고시|university|college|institute|school|academy).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCHOOL_SPLIT = Pattern.compile("^(.*?(?:대학교|대학원|대학|고등학교|University|College))\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAJOR = Pattern.compile(".*(?:학과|학부|전공|공학|과학|과)$|.*\\b(?:major|engineering|science)\\b.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEGREE = Pattern.compile("(전문학사|학사|석사|박사|bachelor(?:'s)?|master(?:'s)?|ph\\.?\\s?d|doctorate)", Pattern.CASE_INSENSITIVE);
    protected static final Pattern EDU_STATUS = Pattern.compile("(졸업예정|졸업|재학|휴학|중퇴|수료|편입|graduated|enrolled)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISSUER = Pattern.compile(
            ".*(?:공단|협회|진흥원|위원회|재단|학회|협의회|센터|평가원|연구원|상공회의소|ETS|YBM|Oracle|Microsoft|AWS|Amazon|Google|Cisco|PMI|CompTIA|Linux|Adobe).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPANY = Pattern.compile(
            ".*(?:주식회사|\\(주\\)|㈜|\\(유\\)|유한회사|재단법인|사단법인|\\b(?:inc|corp|corporation|co\\.?,?\\s*ltd|ltd|llc|gmbh)\\b).*",
            Pattern.CASE_INSENSITIVE);

    /** "남 1999년 (만 30세)" */
    private static final Pattern GENDER_AGE = Pattern.compile("^(?:남|여)(?:자|성)?\\s*[,·]?\\s*(?:(?:19|20)\\d{2}\\s*년?생?)?\\s*[(（]?\\s*만?\\s*\\d{1,2}\\s*세\\s*[)）]?$");
    /** 이름 줄 : "홍길동", "홍 길 동", "홍길동 신입", "홍길동 경력 3년" */
    private static final Pattern NAME_LINE = Pattern.compile("^([가-힣](?:\\s?[가-힣]){1,3})(?:\\s+(?:신입|경력(?:\\s*\\d+\\s*년)?(?:\\s*\\d+\\s*개월)?))?$");
    /** 주소 앞 우편번호 : "(10114) 경기 …" */
    private static final Pattern POSTCODE = Pattern.compile("^[(\\[]?\\d{5}[)\\]]?\\s*");
    /** 경력기술서의 "프로젝트 명 : …" */
    private static final Pattern PROJECT_LABEL_LINE = Pattern.compile("^(?:프로젝트\\s*(?:명|이름)|project\\s*name)\\s*[:：]", Pattern.CASE_INSENSITIVE);
    /** 기술 태그만 나열된 줄 : "MySQL Javascript Spring Framework JAVA React" */
    private static final Pattern SKILL_TAGS = Pattern.compile("^[A-Za-z][A-Za-z0-9.+#/\\-]*(?:\\s+[A-Za-z][A-Za-z0-9.+#/\\-]*){2,}$");
    private static final Pattern ASCII_WORD = Pattern.compile("[A-Za-z][A-Za-z0-9.+#\\-]*[A-Za-z0-9+#]|[A-Za-z]");
    /** PDF 에서 이보다 긴 줄이 문장 중간에 끝나면 화면 폭 때문에 줄이 바뀐 것으로 봄 */
    private static final int WRAP_MIN = 40;

    private static final Pattern COVER_MAX = Pattern.compile("[(（\\[]?\\s*(?:최대\\s*)?(\\d{1,2},\\d{3}|\\d{2,4})\\s*자\\s*(?:이내|이하|내외|제한|까지)?\\s*[)）\\]]?");
    private static final Pattern COVER_COUNTER = Pattern.compile("^\\s*(?:글자\\s*수|공백\\s*(?:포함|제외)|\\(\\s*공백).*");
    private static final Pattern COVER_PROMPT = Pattern.compile(".*(?:주세요|주십시오|하시오|하십시오|바랍니다|\\?)\\s*[.)）]?\\s*(?:[(（\\[].*[)）\\]])?\\s*$");
    private static final Pattern COVER_NUMBERED = Pattern.compile("^\\s*(?:Q\\s*)?\\d{1,2}\\s*[.)]\\s*\\S.*|^\\s*(?:문항|질문)\\s*\\d+.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern COVER_BRACKETED = Pattern.compile("^\\s*[\\[【<〈《].{1,60}[\\]】>〉》]\\s*$");
    private static final Pattern COVER_TITLE_CLEAN = Pattern.compile("^\\s*(?:Q\\s*)?\\d{1,2}\\s*[.)]\\s*|^\\s*(?:문항|질문)\\s*\\d+\\s*[.):：]?\\s*", Pattern.CASE_INSENSITIVE);

    private static final Set<String> LBL_NAME = Set.of("이름", "성명", "name", "성명한글", "한글성명", "한글이름");
    private static final Set<String> LBL_ADDRESS = Set.of("주소", "거주지", "현주소", "address", "거주지역", "주소지");
    private static final Set<String> LBL_COMPANY = Set.of("회사명", "회사", "기업명", "근무처", "company");
    private static final Set<String> LBL_POSITION = Set.of("직무", "주요직무", "직책", "직급", "직위", "포지션", "position", "담당직무", "title", "role");
    private static final Set<String> LBL_PROJECT = Set.of("프로젝트명", "프로젝트", "project");
    private static final Set<String> LBL_ROLE = Set.of("역할", "담당", "담당역할", "담당업무", "role", "포지션", "position");
    private static final Set<String> LBL_TECH = Set.of("사용기술", "보유기술", "활용기술", "기술스택", "기술", "사용언어", "개발환경", "환경", "techstack",
            "tech", "stack", "skills", "skill", "기술환경", "사용툴", "tools", "언어", "사용기술스택");
    private static final Set<String> LBL_URL = Set.of("url", "링크", "데모", "시연", "github", "깃허브", "배포", "주소", "link",
            "demo", "시연영상", "배포주소", "repository", "repo", "저장소");
    private static final Set<String> LBL_MAJOR = Set.of("전공", "학과", "major");
    private static final Set<String> LBL_ISSUER = Set.of("발급기관", "발행처", "발행기관", "시행기관", "주관", "issuer");
    /** 인적 사항 줄 — 페이지 중간에 나와도 섹션 내용에 섞이지 않게 따로 모음 */
    private static final Set<String> LBL_CONTACT = Set.of("이름", "성명", "휴대폰", "휴대전화", "핸드폰", "전화", "전화번호", "연락처",
            "email", "이메일", "메일", "주소", "거주지", "현주소", "생년월일");
    /** 연봉은 이력서 폼에 칸이 없어서 가져오지 않음 */
    private static final Set<String> LBL_SALARY = Set.of("연봉", "희망연봉", "현재연봉", "직전연봉");
    /** 기술 이름이 아닌 영문 단어 (사용 기술 문장에서 기술만 뽑을 때 뺌) */
    private static final Set<String> NOT_TECH = Set.of("db", "api", "ui", "ux", "web", "was", "and", "or", "the", "with");

    // =========================================================
    // 사이트별로 바꾸는 부분 (사이트 전용 파서가 덮어씀)
    // =========================================================

    /** 이 파서가 맡는 사이트 (결과의 source 로 나감) */
    public abstract ResumeSource source();

    /** 줄 다듬기 — 섹션 나누기 전에 한 번 (기본 : 그대로) */
    protected List<String> preprocess(List<String> lines) {
        return lines;
    }

    /** 맨 위 요약 카드(같은 섹션이 뒤에 다시 나옴)를 버릴지 (기본 : 안 버림) */
    protected boolean dropSummaryCards() {
        return false;
    }

    /** 페이지마다 반복되는 섹션 제목이라 무시할지 (기본 : 아님) */
    protected boolean isRepeatedPageHeading(String line, Section heading, Section current) {
        return false;
    }

    /**
     * 섹션 제목과 상관없이 줄 모양으로 섹션을 정할 때 (기본 : null = 지금 섹션 그대로)
     *  - 사람인 PDF 처럼 섹션 제목이 내용과 떨어져서 나오는 경우에 사용
     * @param currentLines 지금 섹션에 이미 들어간 줄들
     */
    protected Section routeLine(String line, Section current, List<String> currentLines) {
        return null;
    }

    /** 첫 섹션 전의 문장형 줄을 간단 소개로 쓸지 (기본 : 안 씀) */
    protected boolean useHeadlineAsIntro() {
        return false;
    }

    // =========================================================
    // 공통 처리
    // =========================================================

    @Override
    public ResumeImportResultDto parse(String text, ResumeFileType fileType) {
        // PDF 는 화면 폭에서 줄이 바뀌므로 문장 중간 줄바꿈을 이어 붙임
        boolean wrapped = fileType == ResumeFileType.PDF;
        List<String> allLines = preprocess(toLines(text));
        Split split = splitSections(allLines);
        Map<Section, List<String>> sections = split.sections();

        Profile profile = parseProfile(String.join("\n", allLines), sections, split.profileLines(), allLines, wrapped);
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

        return new ResumeImportResultDto(fileType.getExtension(), source().getCode(), source().getLabel(),
                profile, links, skills, educations,
                certificates, experiences, projects, coverLetters, warnings);
    }

    // =========================================================
    // 1) 섹션 나누기
    // =========================================================

    @Override
    public Map<String, List<String>> debugSections(String text) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        Split split = splitSections(preprocess(toLines(text)));
        split.sections().forEach((section, lines) ->
                out.put(section.name(), lines.stream().filter(l -> !l.isEmpty()).toList()));
        out.put("(인적사항 줄)", split.profileLines());
        return out;
    }

    /** 글자 → 앞뒤 공백을 지운 줄 목록 (빈 줄 유지) */
    protected static String labelKeyOf(String s) {
        return labelKey(s);
    }

    protected static List<String> toLines(String text) {
        List<String> out = new ArrayList<>();
        for (String raw : text.split("\n", -1)) out.add(raw.strip());
        return out;
    }

    /** 섹션 제목 하나와 그 아래 줄들 */
    private record Segment(Section section, List<String> lines) {
        long count() {
            return lines.stream().filter(l -> !l.isEmpty()).count();
        }
    }

    /** sections = 섹션별 줄, profileLines = 따로 모은 인적 사항 줄 */
    private record Split(Map<Section, List<String>> sections, List<String> profileLines) {
    }

    private Split splitSections(List<String> all) {
        List<Segment> segments = new ArrayList<>();
        List<String> profileLines = new ArrayList<>();
        Segment cur = new Segment(Section.HEAD, new ArrayList<>());
        segments.add(cur);

        for (String line : all) {
            if (line.isEmpty()) {
                cur.lines().add("");
                continue;
            }

            // 인적 사항 줄 (잡코리아는 페이지 중간에 인적 사항이 다시 나옴)
            Matcher lm = LABEL_INLINE.matcher(line);
            if (lm.matches() && line.length() <= 80) {
                String key = labelKey(lm.group(1));
                if (LBL_SALARY.contains(key)) continue;
                if (LBL_CONTACT.contains(key) && !URL.matcher(line).find()) {
                    profileLines.add(line);
                    continue;
                }
            }
            if (GENDER_AGE.matcher(line).matches()) {
                String name = popNameLine(cur.lines());          // 바로 위 줄이 이름
                if (name != null) profileLines.add("이름: " + name);
                continue;
            }

            Section heading = headingOf(line);
            if (heading != null) {
                if (isRepeatedPageHeading(line, heading, cur.section())) continue;
                cur = new Segment(heading, new ArrayList<>());
                segments.add(cur);
                continue;
            }
            Section routed = routeLine(line, cur.section(), cur.lines());
            if (routed != null && routed != cur.section()) {
                cur = new Segment(routed, new ArrayList<>());
                segments.add(cur);
            }
            // 경력기술서의 "프로젝트 명 : …" 부터는 프로젝트
            if (cur.section() != Section.PROJECT && PROJECT_LABEL_LINE.matcher(line).find()) {
                cur = new Segment(Section.PROJECT, new ArrayList<>());
                segments.add(cur);
            }
            // 섹션 제목 없이 자기소개서 문항이 바로 나오는 경우 : "성장과정"
            if (cur.section() != Section.COVER && isStrongCoverTitle(line)) {
                cur = new Segment(Section.COVER, new ArrayList<>());
                segments.add(cur);
            }
            if (cur.section() != Section.COVER && isHeaderRow(line)) continue;
            cur.lines().add(line);
        }
        return new Split(mergeSegments(segments), profileLines);
    }

    /** 줄 전체가 섹션 제목인지 ("경력 총 5년 7개월", "경력 | 총 3년" 도 제목) */
    private static Section headingOf(String line) {
        Section h = Section.heading(line);
        if (h != null || line.length() > 40) return h;
        String noDuration = DURATION.matcher(line).replaceAll("").replaceAll("\\s*\\|\\s*$", "").strip();
        if (!noDuration.equals(line) && !noDuration.isEmpty()) return Section.heading(noDuration);
        return null;
    }

    /** 바로 위 줄이 '홍길동' 같은 이름이면 꺼냄 */
    private static String popNameLine(List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            String l = lines.get(i);
            if (l.isEmpty()) continue;
            Matcher nm = NAME_LINE.matcher(l);
            if (nm.matches()) {
                lines.remove(i);
                return nm.group(1).replace(" ", "");
            }
            return null;
        }
        return null;
    }

    /**
     * 섹션 합치기
     *  - dropSummaryCards() 이면 맨 위 요약 카드(학력/경력/자격증 요약)가 있고 같은 섹션이 뒤에 다시 나올 때
     *    요약은 버림 (요약 안의 기술 태그 줄만 살림)
     */
    private Map<Section, List<String>> mergeSegments(List<Segment> segments) {
        Map<Section, List<String>> map = new EnumMap<>(Section.class);
        List<String> salvagedSkills = new ArrayList<>();
        boolean inSummary = dropSummaryCards();

        for (int i = 0; i < segments.size(); i++) {
            Segment s = segments.get(i);
            if (s.section() != Section.HEAD && inSummary) {
                boolean small = s.count() <= 5 && s.lines().stream().allMatch(l -> l.length() <= 50);
                int idx = i;
                boolean repeated = segments.stream().skip(idx + 1)
                        .anyMatch(o -> o.section() == s.section() && o.count() > 0);
                if (small && repeated) {
                    s.lines().stream().filter(l -> SKILL_TAGS.matcher(l).matches()).forEach(salvagedSkills::add);
                    continue;
                }
                if (!small) inSummary = false;          // 큰 섹션이 나오면 요약 카드 구간 끝
            }
            map.computeIfAbsent(s.section(), k -> new ArrayList<>()).addAll(s.lines());
        }
        boolean hasSkills = map.getOrDefault(Section.SKILL, List.of()).stream().anyMatch(l -> !l.isEmpty());
        if (!hasSkills && !salvagedSkills.isEmpty()) {
            map.computeIfAbsent(Section.SKILL, k -> new ArrayList<>()).addAll(salvagedSkills);
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

    private Profile parseProfile(String text, Map<Section, List<String>> sections, List<String> profileLines,
                                 List<String> allLines, boolean wrapped) {
        List<String> head = new ArrayList<>(profileLines);
        head.addAll(concat(sections, Section.HEAD, Section.PROFILE));

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
        if (name == null) {
            String writer = labeled(allLines, Set.of("작성자"));     // "작성자 : 홍길동"
            if (writer != null && writer.replace(" ", "").matches("[가-힣]{2,5}")) name = writer.replace(" ", "");
        }

        String address = labeled(head, LBL_ADDRESS);
        if (address != null && URL.matcher(address).find()) address = null;
        if (address != null) address = POSTCODE.matcher(address).replaceFirst("");
        List<String> introLines = lines(sections, Section.INTRO).stream()
                .filter(l -> l.isEmpty() || !isUrlOnly(l)).toList();
        String intro = joinText(introLines, wrapped);
        if (intro.isEmpty() && useHeadlineAsIntro()) intro = headline(lines(sections, Section.HEAD));

        return new Profile(cut(name, LEN_NAME), cut(email, LEN_EMAIL), cut(phone, LEN_PHONE),
                cut(address, LEN_ADDRESS), blankToNull(intro));
    }

    /**
     * 섹션 제목 전의 한 줄 소개 (잡코리아 이력서 제목 등) → 간단 소개
     *  - 이름·연락처·날짜·표가 아닌 첫 문장형 줄
     */
    private static String headline(List<String> head) {
        int checked = 0;
        for (String l : head) {
            if (l.isEmpty()) continue;
            if (++checked > 4) break;
            if (l.length() < 10 || l.length() > 120 || l.indexOf(' ') < 0) continue;
            if (l.replace(" ", "").contains("이력서") || l.matches(".*[\\d@|:：].*")) continue;
            if (!l.matches(".*[가-힣].*")) continue;
            return l;
        }
        return "";
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
                String type = displayName != null && displayName.contains("포트폴리오") ? "portfolio" : linkType(url);
                links.put(key, new Link(type, cut(displayName, LEN_LINK_NAME), url));
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
        if (u.contains("github.io")) return "portfolio";                // GitHub Pages = 개인 사이트
        if (u.contains("github.com") || u.contains("gitlab.com")) return "github";
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
                if (part.matches(".*[가-힣].*") && part.strip().contains(" ")) {
                    // 문장 → 영문 기술 이름만 ("JSP를 중점으로 Spring 프레임워크를" → JSP, Spring), 없으면 건너뜀
                    String tech = techList(part);
                    if (tech != null) for (String w : tech.split(", ")) addSkill(skills, w, category);
                    continue;
                }
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
            "spring boot", "spring cloud", "spring batch", "spring security", "spring data", "spring mvc", "spring framework",
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
        // 섹션 첫 줄부터 기간이 있으면 '기간 → 이름' 순서 (잡코리아·사람인) → 위 줄을 제목으로 끌어오지 않음
        boolean dateFirst = idx.get(0) == 0;
        int[] starts = new int[n];
        String[] rests = new String[n];
        for (int k = 0; k < n; k++) {
            int i = idx.get(k);
            rests[k] = cleanRest(periods.get(k).removeFrom(lines.get(i)));
            int prevLimit = k == 0 ? -1 : idx.get(k - 1);
            int start = i;
            // 기간만 있는 줄이면 바로 위 1~2줄(짧은 제목 줄)을 제목으로
            if (rests[k].isEmpty() && !dateFirst) {
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
            List<String> body = logicalLines(b.body(), wrapped);
            body.removeIf(RuleBasedResumeParser::isDurationOnly);  // "3년 11개월"

            // 회사 : 라벨 → '주식회사·(주)' 가 붙은 칸 → 본문의 짧은 회사 줄 → 첫 칸
            String company = takeLabeled(body, LBL_COMPANY, LEN_COMPANY);
            if (company == null) company = takeFirst(t, COMPANY);
            if (company == null) company = takeShortLine(body, COMPANY);
            if (company == null && !t.isEmpty()) company = t.remove(0);
            if (company == null) continue;

            String position = takeLabeled(body, LBL_POSITION, 50);
            if (position == null && !t.isEmpty()) position = t.remove(0);
            if (position == null && body.size() > 1 && isTitleLike(body.get(0))) position = body.remove(0);
            body.addAll(0, t);                                      // 남은 칸 (예: 부서·직급, 표의 담당업무)

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
            List<String> body = logicalLines(b.body(), wrapped);
            // 제목 줄이 "프로젝트 명 : 에스원 쇼룸" 이면 라벨 떼기
            for (int i = 0; i < t.size(); i++) {
                Matcher hm = LABEL_INLINE.matcher(t.get(i));
                if (hm.matches() && LBL_PROJECT.contains(labelKey(hm.group(1))) && !hm.group(2).isBlank()) {
                    t.set(i, hm.group(2).strip());
                }
            }

            String name = takeLabeled(body, LBL_PROJECT, LEN_PROJECT);
            if (name == null && !t.isEmpty()) name = t.remove(0);
            if (name == null) continue;

            String role = takeLabeled(body, LBL_ROLE, 60);
            if (role == null && !t.isEmpty()) role = String.join(" · ", t);

            String tech = takeLabeled(body, LBL_TECH, 500);
            if (tech != null) tech = techList(tech);

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

            // 본문에 따로 나오는 졸업 상태 줄 ("졸업")
            for (int i = 0; i < body.size() && status == null; i++) {
                Matcher sm = EDU_STATUS.matcher(body.get(i));
                if (body.get(i).length() <= 6 && sm.matches()) {
                    status = sm.group(1);
                    body.remove(i);
                }
            }

            // 학교 : '대학교·고등학교' 가 붙은 칸 → 본문의 짧은 학교 줄 (잡코리아는 기간 다음 줄에 학교) → 첫 칸
            String school = takeFirst(t, SCHOOL);
            if (school == null) school = takeShortLine(body, SCHOOL);
            if (school == null && !t.isEmpty()) school = t.remove(0);
            if (school == null) continue;

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
            String issuer = null;
            // 칸 하나가 통째로 기관명이면 기관 (순서 상관없음)
            for (int i = 0; t.size() >= 2 && i < t.size(); i++) {
                if (!t.get(i).contains(" ") && ISSUER.matcher(t.get(i)).matches()) {
                    issuer = t.remove(i);
                    break;
                }
            }
            // 공백으로 이어진 칸 안의 기관 단어 : "정보처리기사 한국산업인력공단", "최종합격 한국산업인력공단", "TOEIC 900점 ETS"
            for (int i = 0; issuer == null && i < t.size(); i++) {
                List<String> words = new ArrayList<>(Arrays.asList(t.get(i).split("\\s+")));
                for (int j = (i == 0 ? 1 : 0); j < words.size(); j++) {
                    if (ISSUER.matcher(words.get(j)).matches()) {
                        issuer = words.remove(j);
                        if (i == 0) {
                            // 첫 칸 = 자격증명 + 기관 + (나머지는 설명)
                            t.set(0, String.join(" ", words.subList(0, j)));
                            if (words.size() > j) t.add(1, String.join(" ", words.subList(j, words.size())));
                        } else if (words.isEmpty()) {
                            t.remove(i);
                        } else {
                            t.set(i, String.join(" ", words));
                        }
                        break;
                    }
                }
            }
            String name = t.remove(0);
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
        Set<Integer> sentenceTitles = sentenceTitles(lines);

        for (int idx = 0; idx < lines.size(); idx++) {
            String line = lines.get(idx);
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

            if (sentenceTitles.contains(idx) || isCoverTitle(line)) {
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

    /**
     * 키워드 없는 소제목 찾기
     *  - 잡코리아 : "성능과 확장성을 고민하는 개발자입니다." / 사람인 : "어릴 적 전공을 택한 계기" 다음에 긴 본문
     *  - 40자 이하, 앞 줄은 문장이 끝났고, 다음 줄은 긴 본문
     *  - 섹션 첫 줄이거나 두 개 이상일 때만 소제목으로 인정 (우연히 맞는 짧은 문장 제외)
     */
    private static Set<Integer> sentenceTitles(List<String> lines) {
        List<Integer> nonEmpty = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) if (!lines.get(i).isEmpty()) nonEmpty.add(i);

        Set<Integer> found = new java.util.LinkedHashSet<>();
        for (int k = 0; k < nonEmpty.size(); k++) {
            String line = lines.get(nonEmpty.get(k));
            if (line.length() > 40 || BULLET.matcher(line).find() || COVER_COUNTER.matcher(line).matches()) continue;
            String prev = k == 0 ? null : lines.get(nonEmpty.get(k - 1));
            String next = k + 1 < nonEmpty.size() ? lines.get(nonEmpty.get(k + 1)) : null;
            if (prev != null && !PARAGRAPH_END.matcher(prev).matches()) continue;
            if (next == null || next.length() < 50) continue;
            found.add(nonEmpty.get(k));
        }
        boolean startsWithTitle = !nonEmpty.isEmpty() && found.contains(nonEmpty.get(0));
        return startsWithTitle || found.size() >= 2 ? found : Set.of();
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
            } else if (pendingBreak || !wrapped || prev.length() < WRAP_MIN
                    || BULLET.matcher(line).find() || PARAGRAPH_END.matcher(prev).matches()
                    || isUrlOnly(prev) || isUrlOnly(line)) {             // URL 줄은 이어 붙이지 않음
                sb.append('\n').append(line);
            } else {
                sb.append(' ').append(line);
            }
            prev = line;
            pendingBreak = false;
        }
        return sb.toString().strip();
    }

    /** PDF 는 화면 폭에서 나뉜 줄을 이어서 '한 줄 = 한 문단·항목' 으로 */
    private static List<String> logicalLines(List<String> lines, boolean wrapped) {
        List<String> nonEmpty = lines.stream().filter(l -> !l.isEmpty()).toList();
        if (!wrapped) return new ArrayList<>(nonEmpty);
        String joined = joinText(lines, true);
        return joined.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(joined.split("\n")));
    }

    private static boolean isDurationOnly(String line) {
        return DURATION.matcher(line).find() && DURATION.matcher(line).replaceAll("").replaceAll("[\\s()（）]", "").isEmpty();
    }

    /** 칸 목록에서 패턴에 맞는 첫 칸을 꺼냄 */
    private static String takeFirst(List<String> tokens, Pattern p) {
        for (int i = 0; i < tokens.size(); i++) {
            if (p.matcher(tokens.get(i)).matches()) return tokens.remove(i);
        }
        return null;
    }

    /** 본문에서 패턴에 맞는 짧은 줄(40자 이하)을 꺼냄 */
    private static String takeShortLine(List<String> body, Pattern p) {
        for (int i = 0; i < body.size(); i++) {
            String l = body.get(i);
            if (l.length() <= 40 && !SENTENCE_END.matcher(l).matches() && p.matcher(l).matches()) return body.remove(i).strip();
        }
        return null;
    }

    /**
     * 사용 기술 값 정리
     *  - "Java, Spring / MySQL" → "Java, Spring, MySQL"
     *  - 문장이면 영문 기술 이름만 : "Java와 Thymeleaf로 개발, DB는 Oracle" → "Java, Thymeleaf, Oracle"
     */
    private static String techList(String value) {
        String v = value.strip();
        if (!v.matches(".*[가-힣]{2,}.*")) return String.join(", ", SKILL_SPLIT.split(v));
        List<String> words = new ArrayList<>();
        Matcher m = ASCII_WORD.matcher(v);
        while (m.find()) words.add(m.group());
        Map<String, String> uniq = new LinkedHashMap<>();
        for (String w : mergeKnownPairs(words)) {
            String key = w.toLowerCase(Locale.ROOT);
            if (!NOT_TECH.contains(key)) uniq.putIfAbsent(key, w);
        }
        return uniq.isEmpty() ? null : String.join(", ", uniq.values());
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
