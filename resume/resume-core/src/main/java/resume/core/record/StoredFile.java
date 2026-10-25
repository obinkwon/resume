package resume.core.record;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

// 조회 결과
public record StoredFile(Resource resource, MediaType contentType, long size) {
}
