package resume;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"oauth.core", "resume.web"})
public class ResumeWebMainApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeWebMainApplication.class, args);
	}

}
