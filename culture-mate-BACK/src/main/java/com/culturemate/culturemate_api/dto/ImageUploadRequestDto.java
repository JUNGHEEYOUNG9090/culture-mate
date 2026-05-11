package com.culturemate.culturemate_api.dto;

import com.culturemate.culturemate_api.domain.ImageTarget;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
public class ImageUploadRequestDto {
    
    @NotNull(message = "파일은 필수입니다")
    @Size(min = 1, message = "최소 1개 이상의 파일이 필요합니다")
    private List<MultipartFile> files;
    
    @NotNull(message = "이미지 타겟은 필수입니다")
    private ImageTarget imageTarget;
    
    @NotNull(message = "대상 ID는 필수입니다")
    private Long targetId;
}