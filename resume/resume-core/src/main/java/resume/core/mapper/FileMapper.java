package resume.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import resume.core.dto.FileDto;

import java.util.List;

@Mapper
public interface FileMapper {
    int insertFile(FileDto fileDto);
    FileDto selectFile(FileDto fileDto);
}
