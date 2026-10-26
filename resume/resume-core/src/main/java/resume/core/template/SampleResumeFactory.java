package resume.core.template;

import org.springframework.stereotype.Component;
import resume.core.dto.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 이력서 없이 템플릿을 만들 때 쓰는 가상의 이력서.
 * 9개 섹션이 모두 채워져 있어서 템플릿의 모든 부분을 확인할 수 있다.
 * DB 에 저장하지 않고 매번 새로 만든다.
 */
@Component
public class SampleResumeFactory {

    /** 사진 자리 확인용 (CSP img-src data: 허용) */
    private static final String SAMPLE_PHOTO = "data:image/svg+xml;charset=UTF-8,"
            + "%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='160' viewBox='0 0 120 160'%3E"
            + "%3Crect width='120' height='160' fill='%23dfe3ea'/%3E"
            + "%3Ccircle cx='60' cy='62' r='26' fill='%23aab2bf'/%3E"
            + "%3Cpath d='M18 160c4-34 22-52 42-52s38 18 42 52z' fill='%23aab2bf'/%3E"
            + "%3C/svg%3E";

    public ResumeDetailResponseDto create() {
        ResumeDetailResponseDto r = new ResumeDetailResponseDto();
        r.setResumeId(0L);
        r.setTitle("샘플 이력서");
        r.setProfile(profile());
        r.setCoverLetters(coverLetters());
        r.setExperiences(experiences());
        r.setProjects(projects());
        r.setEducations(educations());
        r.setCertificates(certificates());
        r.setSkills(skills());
        r.setLinks(links());
        return r;
    }

    private ProfileDto profile() {
        ProfileDto p = new ProfileDto();
        p.setName("김이력");
        p.setEmail("resume.sample@example.com");
        p.setPhone("010-1234-5678");
        p.setAddress("서울특별시 마포구");
        p.setProfileImageUrl(SAMPLE_PHOTO);
        p.setIntroduction("사용자가 불편함을 느끼기 전에 먼저 고치는 5년 차 백엔드 개발자입니다. "
                + "주문·결제 시스템을 운영하며 응답 속도를 절반으로 줄였고, 팀의 배포 과정을 자동화했습니다.");
        return p;
    }

    private List<CoverLetterDto> coverLetters() {
        List<CoverLetterDto> list = new ArrayList<>();
        list.add(coverLetter(1, "지원 동기",
                "많은 사람이 매일 쓰는 서비스를 안정적으로 만드는 일에 보람을 느낍니다. "
                        + "귀사의 대규모 트래픽 환경에서 그동안 쌓은 성능 개선 경험을 더 크게 펼치고 싶어 지원했습니다."));
        list.add(coverLetter(2, "성장 과정과 강점",
                "문제를 끝까지 파고드는 습관이 있습니다. 장애가 나면 원인을 찾아 문서로 남기고, "
                        + "같은 문제가 다시 생기지 않도록 모니터링과 테스트를 먼저 보강합니다."));
        return list;
    }

    private List<ExperienceDto> experiences() {
        List<ExperienceDto> list = new ArrayList<>();
        list.add(experience(1, "주식회사 샘플커머스", "백엔드 개발자 · 선임",
                LocalDate.of(2023, 3, 1), null, true,
                "주문·결제 API 설계와 운영을 맡고 있습니다. 조회 쿼리 개선과 캐시 도입으로 평균 응답 시간을 420ms 에서 190ms 로 줄였습니다."));
        list.add(experience(2, "예시소프트", "웹 개발자",
                LocalDate.of(2020, 1, 1), LocalDate.of(2023, 2, 28), false,
                "사내 관리자 시스템을 Spring Boot 로 새로 만들고, Jenkins 배포 파이프라인을 구성했습니다."));
        return list;
    }

    private List<ProjectDto> projects() {
        List<ProjectDto> list = new ArrayList<>();
        list.add(project(1, "결제 시스템 개편", "백엔드 리드",
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 9, 30),
                "여러 결제 대행사를 하나의 인터페이스로 묶어 새 결제 수단을 하루 만에 붙일 수 있게 했습니다.",
                "Java, Spring Boot, PostgreSQL, Redis", "https://example.com/payment"));
        list.add(project(2, "이력서 빌더", "개인 프로젝트",
                LocalDate.of(2025, 6, 1), null,
                "한 번 입력한 이력서를 템플릿만 바꿔 여러 형태의 PDF 로 내보내는 서비스입니다.",
                "Spring Boot, Thymeleaf, MyBatis, Playwright", null));
        return list;
    }

    private List<EducationDto> educations() {
        List<EducationDto> list = new ArrayList<>();
        list.add(education(1, "한국대학교", "컴퓨터공학과", "학사",
                LocalDate.of(2014, 3, 1), LocalDate.of(2020, 2, 28), "졸업 프로젝트: 실시간 버스 도착 알림 앱"));
        return list;
    }

    private List<CertificateDto> certificates() {
        List<CertificateDto> list = new ArrayList<>();
        list.add(certificate(1, "정보처리기사", "한국산업인력공단", LocalDate.of(2019, 11, 22), null));
        list.add(certificate(2, "SQLD", "한국데이터산업진흥원", LocalDate.of(2021, 6, 18), "SQL 개발자"));
        return list;
    }

    private List<SkillDto> skills() {
        List<SkillDto> list = new ArrayList<>();
        list.add(skill(1, "Java", "언어", 5));
        list.add(skill(2, "Spring Boot", "프레임워크", 5));
        list.add(skill(3, "PostgreSQL", "데이터베이스", 4));
        list.add(skill(4, "Redis", "데이터베이스", 3));
        list.add(skill(5, "JavaScript", "언어", 3));
        list.add(skill(6, "Docker", "도구", 3));
        return list;
    }

    private List<LinkDto> links() {
        List<LinkDto> list = new ArrayList<>();
        list.add(link(1, "GITHUB", "GitHub", "https://github.com/sample"));
        list.add(link(2, "BLOG", "기술 블로그", "https://sample.tistory.com"));
        return list;
    }

    /* ---------- 만드는 도구 ---------- */

    private CoverLetterDto coverLetter(int order, String title, String content) {
        CoverLetterDto d = new CoverLetterDto();
        d.setTitle(title);
        d.setContent(content);
        d.setSortOrder(order);
        return d;
    }

    private ExperienceDto experience(int order, String company, String position,
                                     LocalDate start, LocalDate end, boolean current, String description) {
        ExperienceDto d = new ExperienceDto();
        d.setCompanyName(company);
        d.setPosition(position);
        d.setStartDate(start);
        d.setEndDate(end);
        d.setIsCurrent(current);
        d.setDescription(description);
        d.setUseYn("Y");
        d.setSortOrder(order);
        return d;
    }

    private ProjectDto project(int order, String name, String role, LocalDate start, LocalDate end,
                               String description, String techStack, String demoUrl) {
        ProjectDto d = new ProjectDto();
        d.setProjectName(name);
        d.setRole(role);
        d.setStartDate(start);
        d.setEndDate(end);
        d.setDescription(description);
        d.setTechStack(techStack);
        d.setDemoUrl(demoUrl);
        d.setUseYn("Y");
        d.setSortOrder(order);
        return d;
    }

    private EducationDto education(int order, String school, String major, String degree,
                                   LocalDate start, LocalDate end, String description) {
        EducationDto d = new EducationDto();
        d.setSchoolName(school);
        d.setMajor(major);
        d.setDegree(degree);
        d.setStartDate(start);
        d.setEndDate(end);
        d.setDescription(description);
        d.setUseYn("Y");
        d.setSortOrder(order);
        return d;
    }

    private CertificateDto certificate(int order, String name, String issuer, LocalDate issued, String description) {
        CertificateDto d = new CertificateDto();
        d.setCertificateName(name);
        d.setIssuer(issuer);
        d.setIssuedDate(issued);
        d.setDescription(description);
        d.setUseYn("Y");
        d.setSortOrder(order);
        return d;
    }

    private SkillDto skill(int order, String name, String category, int proficiency) {
        SkillDto d = new SkillDto();
        d.setSkillName(name);
        d.setCategory(category);
        d.setProficiency(proficiency);
        d.setUseYn("Y");
        d.setSortOrder(order);
        return d;
    }

    private LinkDto link(int order, String type, String displayName, String url) {
        LinkDto d = new LinkDto();
        d.setLinkType(type);
        d.setDisplayName(displayName);
        d.setUrl(url);
        d.setUseYn("Y");
        d.setSortOrder(order);
        return d;
    }
}
