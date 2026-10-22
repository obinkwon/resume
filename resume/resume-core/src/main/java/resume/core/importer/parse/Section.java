package resume.core.importer.parse;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 이력서 섹션 + 섹션 제목 사전 (국내 구직 사이트 양식 기준, 영문 제목 포함)
 */
enum Section {
    /** 첫 섹션 제목 전 (이름·연락처가 보통 여기) */
    HEAD,
    PROFILE,
    INTRO,
    COVER,
    EXPERIENCE,
    PROJECT,
    EDUCATION,
    CERTIFICATE,
    SKILL,
    LINK,
    /** 병역·수상·활동 등 지금 폼에 없는 섹션 (앞 섹션과 섞이지 않게 끊는 용도) */
    OTHER;

    private static final Map<String, Section> HEADINGS = new HashMap<>();

    static {
        put(PROFILE, "인적사항", "기본정보", "개인정보", "인적정보", "기본인적사항", "personalinformation",
                "personalinfo", "contact", "연락처");
        put(INTRO, "간단소개", "간단소개글", "한줄소개", "소개", "소개글", "자기소개글요약", "자기소개요약", "요약", "프로필요약", "프로필",
                "about", "aboutme", "summary", "profile", "introduction", "intro");
        put(COVER, "자기소개서", "자기소개", "자소서", "자기소개서항목", "coverletter", "personalstatement");
        put(EXPERIENCE, "경력", "경력사항", "경력기술서", "경력정보", "직장경력", "근무경력", "회사경력", "경력및업무",
                "경력상세", "경력요약", "workexperience", "experience", "experiences", "career", "employment",
                "professionalexperience", "workhistory");
        put(PROJECT, "프로젝트", "프로젝트경험", "주요프로젝트", "프로젝트경력", "수행프로젝트", "프로젝트이력",
                "포트폴리오프로젝트", "projects", "project", "personalprojects", "sideprojects");
        put(EDUCATION, "학력", "학력사항", "학력정보", "최종학력", "학력및전공", "education", "academicbackground");
        put(CERTIFICATE, "자격증", "자격사항", "자격증사항", "자격면허", "자격/면허", "자격증/면허", "자격증및어학",
                "자격증/어학", "어학/자격증", "자격·어학", "자격증·어학", "어학", "어학능력", "어학자격", "어학사항",
                "외국어", "외국어능력", "자격증및외국어", "certificates", "certifications", "certificate",
                "certification", "licenses", "license", "languages");
        put(SKILL, "기술", "기술스택", "보유기술", "스킬", "보유스킬", "기술역량", "활용기술", "업무스킬",
                "보유역량", "skills", "skill", "techstack", "technicalskills", "technologies", "기술및역량");
        put(LINK, "링크", "외부링크", "포트폴리오", "포트폴리오링크", "links", "link", "sns", "웹사이트",
                "portfolio", "website");
        put(OTHER, "병역", "병역사항", "수상", "수상경력", "수상내역", "수상이력", "대외활동", "활동", "활동사항",
                "교육", "교육이수", "교육사항", "교육이력", "교육수료", "해외경험", "해외연수", "연수", "봉사활동",
                "희망근무조건", "희망조건", "취업우대", "취업우대사항", "우대사항", "보훈", "기타", "기타사항",
                "awards", "activities", "military", "training", "volunteer", "references", "참고사항",
                "인턴·대외활동/해외경험", "인턴·대외활동", "대외활동/해외경험", "인턴", "인턴십", "지원분야", "희망직무",
                "희망근무조건", "취업우대·병역", "우대사항·병역");
    }

    private static void put(Section s, String... keys) {
        for (String k : keys) HEADINGS.put(k, s);
    }

    // ---------- 섹션 제목 ----------

    private static final Pattern PAREN = Pattern.compile("[(（\\[].*?[)）\\]]");
    private static final Pattern LEAD = Pattern.compile("^[\\[(【〔<〈《■□●○◆◇▶▷►▣◈※•·ㆍ\\-*#>\\d.)]+");
    private static final Pattern TAIL = Pattern.compile("[\\])】〕>〉》:：]+$");

    /** 제목 비교용 : 괄호 내용·공백·번호·글머리표 제거, 소문자 */
    static String key(String line) {
        String s = PAREN.matcher(line).replaceAll("");
        s = s.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        s = LEAD.matcher(s).replaceAll("");
        s = TAIL.matcher(s).replaceAll("");
        return s;
    }

    /** 줄 전체가 섹션 제목이면 그 섹션 (아니면 null) */
    static Section heading(String line) {
        if (line.length() > 30) return null;
        String k = key(line);
        if (k.isEmpty()) {
            // "[경력]" 처럼 괄호로 감싼 제목은 괄호를 벗겨서 다시 확인
            String inner = line.replaceAll("^[\\[(【〔<〈《]+|[\\])】〕>〉》]+$", "");
            if (inner.equals(line)) return null;
            k = key(inner);
        }
        return HEADINGS.get(k);
    }

    // ---------- 자기소개서 문항 ----------

    /** 자기소개서 문항 제목에 자주 쓰이는 말 */
    static final List<String> COVER_KEYWORDS = List.of(
            "성장과정", "성장배경", "성격의장단점", "성격장단점", "장단점", "지원동기", "지원이유", "입사동기",
            "입사후포부", "입사후계획", "포부", "직무역량", "직무관련경험", "직무경험", "핵심역량", "생활신조",
            "가치관", "강점", "협업경험", "갈등해결", "실패경험", "극복경험", "도전경험", "문제해결", "리더십",
            "성공경험", "역경");

    /** 섹션 제목 없이 바로 나와도 자기소개서로 볼 문항 */
    static final Set<String> STRONG_COVER_TITLES = Set.of(
            "성장과정", "성장배경", "성격의장단점", "성격장단점", "지원동기", "입사동기", "입사후포부", "생활신조",
            "성장과정및가치관", "지원동기및입사후포부");

    // ---------- 표 머리 행 ("회사명 | 기간 | 직무") ----------

    static final Set<String> HEADER_WORDS = Set.of(
            "회사명", "회사", "기간", "근무기간", "재직기간", "직무", "직급", "직책", "직위", "담당업무", "업무", "부서",
            "학교명", "학교", "전공", "학위", "졸업여부", "구분", "입학일", "졸업일", "재학기간",
            "자격증명", "자격증", "발행처", "발급기관", "발행기관", "취득일", "등급", "점수", "급수", "프로젝트명",
            "역할", "내용", "기술", "비고", "소재지", "학점", "상태", "연월", "날짜", "기관", "시험명", "취득일자",
            "연봉", "근무지", "고용형태");
}