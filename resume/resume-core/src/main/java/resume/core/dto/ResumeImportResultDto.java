package resume.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 이력서 파일 가져오기 결과 (DB 에 저장하지 않음)
 *  - 필드 이름을 이력서 상세 조회(/api/resume/{id}) 응답과 같게 맞춰서
 *    이력서 작성 폼의 '가져오기'(LIST_SECTIONS.from, addItems) 를 그대로 쓸 수 있음
 *  - 날짜는 'yyyy-MM-dd' 문자열 (일이 없으면 1일)
 */
public record ResumeImportResultDto(
        String fileType,
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
