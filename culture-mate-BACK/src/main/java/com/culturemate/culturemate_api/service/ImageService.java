package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.Image;
import com.culturemate.culturemate_api.domain.ImageTarget;
import com.culturemate.culturemate_api.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class ImageService {

  private final ImageRepository imageRepository;

  @Value("${custom.path.upload.default}")
  private String uploadDefaultDir;

  // ===== 외부 사용 메서드 (Public API) =====

  // 단일 이미지 업로드 (메인 이미지)
  public String uploadSingleImage(MultipartFile file, ImageTarget imageTarget, String subPath) throws IOException {
    String baseDir = getUploadDirectory(imageTarget);
    String uploadDir = (subPath != null && !subPath.trim().isEmpty())
        ? Paths.get(baseDir, subPath).toString()
        : baseDir;
    String fileName = generateUniqueFileName(file.getOriginalFilename());

    // 디렉토리 자동 생성
    createDirectoryIfNotExists(uploadDir);

    // 파일 저장
    Path filePath = Paths.get(uploadDir, fileName);
    Files.write(filePath, file.getBytes());
    
    // 웹 접근 경로 생성
    String webPath = "/images/" + imageTarget.getPath() + 
                     (subPath != null && !subPath.trim().isEmpty() ? "/" + subPath : "") + 
                     "/" + fileName;
    
    System.out.println(imageTarget.name() + " 이미지 업로드 성공: " + webPath);
    return webPath;
  }

  // 썸네일 이미지 업로드 (항상 thumbnail 하위에 저장)
  public String uploadThumbnail(MultipartFile file, ImageTarget imageTarget) throws IOException {
    String baseDir = getUploadDirectory(imageTarget);
    String uploadDir = Paths.get(baseDir, "thumbnail").toString();
    String fileName = generateUniqueFileName(file.getOriginalFilename());

    // 디렉토리 자동 생성
    createDirectoryIfNotExists(uploadDir);

    // 썸네일 생성 및 저장
    createThumbnail(file, Paths.get(uploadDir, fileName));
    
    // 웹 접근 경로 생성
    String webPath = "/images/" + imageTarget.getPath() + "/thumbnail/" + fileName;
    
    System.out.println(imageTarget.name() + " 썸네일 업로드 성공: " + webPath);
    return webPath;
  }

  // 리스트형 이미지 업로드용 & DB 저장 (EVENT_CONTENT, BOARD_CONTENT, MEMBER_GALLERY 등)
  public List<String> uploadMultipleImages(List<MultipartFile> files, ImageTarget imageTarget, Long targetId) {
    String uploadDir = getUploadDirectory(imageTarget);
    createDirectoryIfNotExists(uploadDir);
    List<String> webPaths = new java.util.ArrayList<>();

    for (MultipartFile file : files) {
      try {
        String originalFilename = file.getOriginalFilename();
        // 새로운 파일명 생성 (중복 방지)
        String newFileName = generateUniqueFileName(originalFilename);

        // 파일 저장
        Path filePath = Paths.get(uploadDir, newFileName);
        Files.write(filePath, file.getBytes());

        // 웹 접근 경로 생성 (static 기준으로)
        String webPath = "/images/" + imageTarget.getPath() + "/" + newFileName;
        webPaths.add(webPath);
        
        // DB에 이미지 정보 저장
        Image image = Image.builder()
            .targetType(imageTarget)
            .targetId(targetId)
            .path(webPath)
            .build();
        imageRepository.save(image);

      } catch (IOException e) {
        StringBuilder stb = new StringBuilder();
        stb.append("========== 이미지 업로드 실패 ==========").append("\n")
          .append("파일명: ").append(file.getOriginalFilename()).append("\n")
          .append("오류: ").append(e.getMessage()).append("\n");
        System.out.println(stb.toString());
      }
    }
    return webPaths;
  }

  // 리스트형 이미지 조회
  public List<Image> getImagesByTargetTypeAndId(ImageTarget targetType, Long targetId) {
    return imageRepository.findByTargetTypeAndTargetId(targetType, targetId);
  }

  // 해당 타겟의 모든 이미지 삭제
  public void deleteAllImagesByTarget(ImageTarget targetType, Long targetId) {
    List<Image> imagesToDelete = imageRepository.findByTargetTypeAndTargetId(targetType, targetId);

    // 물리적 파일 삭제
    for (Image image : imagesToDelete) {
      deletePhysicalFile(image.getPath());
    }

    // DB에서 삭제
    imageRepository.deleteByTargetTypeAndTargetId(targetType, targetId);
  }

  // 물리적 파일 삭제 (경로로 직접 삭제)
  public void deletePhysicalFiles(String... filePaths) {
    for (String path : filePaths) {
      if (path != null && !path.trim().isEmpty()) {
        deletePhysicalFile(path);
      }
    }
  }

  // 경로로 이미지 삭제 (DB + 물리적 파일) - 권한 검증은 외부에서 처리
  public void deleteImageByPath(String webPath) {
    // 1. DB에서 해당 경로의 Image 엔티티 찾기
    Image image = imageRepository.findByPath(webPath)
        .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다: " + webPath));

    // 2. 물리적 파일 삭제
    deletePhysicalFile(webPath);

    // 3. DB 레코드 삭제
    imageRepository.delete(image);
    
    System.out.println("이미지 삭제 완료: " + webPath);
  }


  // ===== 내부 편의 메서드 (Private Utilities) =====

  private String getUploadDirectory(ImageTarget imageTarget) {
    return Paths.get(uploadDefaultDir, imageTarget.getPath()).toString();
  }

  private void createDirectoryIfNotExists(String dirPath) {
    try {
      Files.createDirectories(Paths.get(dirPath));
    } catch (IOException e) {
      throw new RuntimeException("디렉토리 생성 실패: " + dirPath, e);
    }
  }

  private String generateUniqueFileName(String originalFilename) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String uuid = UUID.randomUUID().toString().substring(0, 8);
    String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    
    return timestamp + "_" + uuid + extension;
  }

  private void createThumbnail(MultipartFile originalFile, Path thumbnailPath) throws IOException {
    BufferedImage originalImage = ImageIO.read(originalFile.getInputStream());

    // 썸네일 크기 설정 (최대 400x400)
    int thumbnailWidth = 400;
    int thumbnailHeight = 400;
    
    // 비율 유지하면서 리사이즈
    double scaleX = (double) thumbnailWidth / originalImage.getWidth();
    double scaleY = (double) thumbnailHeight / originalImage.getHeight();
    double scale = Math.min(scaleX, scaleY);
    
    int newWidth = (int) (originalImage.getWidth() * scale);
    int newHeight = (int) (originalImage.getHeight() * scale);
    
    // 리사이즈된 이미지 생성
    BufferedImage thumbnailImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = thumbnailImage.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
    g2d.dispose();
    
    // 썸네일 저장
    String format = thumbnailPath.toString().substring(thumbnailPath.toString().lastIndexOf(".") + 1);
    ImageIO.write(thumbnailImage, format, thumbnailPath.toFile());
  }

  private void deletePhysicalFile(String webPath) {
    try {
      // 웹 경로를 실제 파일 경로로 변환
      // webPath 형식: /images/event/main/filename.jpg
      // uploadDefaultDir 기준 상대 경로로 변환
      String relativePath = webPath.replace("/images/", "");
      Path filePath = Paths.get(uploadDefaultDir, relativePath);

      if (Files.exists(filePath)) {
        Files.delete(filePath);
        System.out.println("파일 삭제 성공: " + filePath);
      } else {
        System.out.println("파일이 존재하지 않음: " + filePath);
      }
    } catch (IOException e) {
      System.err.println("파일 삭제 실패: " + webPath + ", 오류: " + e.getMessage());
    }
  }


}
