package resume.core.record;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// 이력서 파일 가져오기 결과
public record ResumeImportResult(
        String fileType,
        /* 인식한 사이트 : generic(일반) / jobkorea … */
        String source,
        /* 화면 표시 이름 : 일반 / 잡코리아 … */
        String sourceName,
        Profile profile,
        List<Link> links,
        List<Skill> skills,
        List<Education> educations,
        List<Certificate> certificates,
        List<Experience> experiences,
        List<Project> projects,
        List<CoverLetter> coverLetters,
        /* 사용자에게 보여줄 안내 (직무를 못 찾은 경력 등) */
        List<String> warnings
) {

    public record Profile(String name, String email, String phone, String address, String introduction) {
    }

    public record Link(String linkType, String displayName, String url) {
    }

    public record Skill(String skillName, String category, Integer proficiency) {
    }

    public record Education(String schoolName, String major, String degree,
                            String startDate, String endDate, String description) {
    }

    public record Certificate(String certificateName, String issuer, String issuedDate, String description) {
    }

    public record Experience(String companyName, String position, String startDate, String endDate,
                             @JsonProperty("isCurrent") Boolean isCurrent, String description) {
    }

    public record Project(String projectName, String startDate, String endDate, String role,
                          String description, String techStack, String demoUrl) {
    }

    public record CoverLetter(String title, String content, Integer maxLength) {
    }
}
