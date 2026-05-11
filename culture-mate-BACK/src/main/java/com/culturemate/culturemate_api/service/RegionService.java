package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.Region;
import com.culturemate.culturemate_api.dto.RegionDto;
import com.culturemate.culturemate_api.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

  private final RegionRepository regionRepository;

  public Region findById(Long regionId) {
    return regionRepository.findById(regionId)
      .orElseThrow(() -> new IllegalArgumentException("지역이 존재하지 않습니다."));
  }

  public List<Region> findAll() {
    return regionRepository.findAll();
  }

  public List<Region> findByHierarchy(RegionDto.Request regionDto) {
    if (regionDto == null || !regionDto.hasRegion()) {
      // 지역 조건이 없으면 모든 지역 반환
      return regionRepository.findAll();
    }
    
    // 전처리: "전체", 빈문자열 → null 변환
    String level1 = normalizeRegionLevel(regionDto.getLevel1());
    String level2 = normalizeRegionLevel(regionDto.getLevel2());
    String level3 = normalizeRegionLevel(regionDto.getLevel3());
    
    // 1단계: 정확한 타겟 Region 찾기
    Region targetRegion = regionRepository.findExactRegion(level1, level2, level3);
    
    if (targetRegion == null) {
      return new ArrayList<>();
    }
    
    // 2단계: 타겟 자체 + 하위 지역들을 명확히 분리해서 수집 (NULL 처리 개선)
    List<Region> result = new ArrayList<>();
    result.add(targetRegion);  // 자기 자신 먼저 추가
    
    // 3단계: 하위 지역들 별도 조회 및 추가
    List<Region> descendants = regionRepository.findAllDescendants(targetRegion.getId());
    result.addAll(descendants);
    
    // 4단계: ID 기반 중복 제거 (안전장치)
    return result.stream()
        .distinct()  // Region 엔티티의 equals/hashCode 기반 중복 제거
        .collect(Collectors.toList());
  }

  public Region findExact(RegionDto.Request regionDto) {
    if (regionDto == null || !regionDto.hasRegion()) {
      return null;
    }
    
    return regionRepository.findExactRegion(
      regionDto.getLevel1(), 
      regionDto.getLevel2(), 
      regionDto.getLevel3()
    );
  }


  @Transactional
  public Region create(String level1, String level2, String level3) {
    // 이미 존재하면 기존 것을 반환 (초기화에서 중복 생성 방지)
    Region existingRegion = regionRepository.findExactRegion(level1, level2, level3);
    if (existingRegion != null) {
      System.out.println("이미 존재하는 지역: " + level1 + "-" + level2 + "-" + level3);
      return existingRegion;
    }
    
    // 계층형 구조에서는 부모를 먼저 찾아야 함
    Region parent = null;
    String currentRegionName = null;
    
    if (level3 != null) {
      // level3 생성 시 level2를 부모로 설정
      parent = regionRepository.findExactRegion(level1, level2, null);
      if (parent == null) {
        // 부모가 없으면 먼저 생성
        parent = create(level1, level2, null);
      }
      currentRegionName = level3;
    } else if (level2 != null) {
      // level2 생성 시 level1을 부모로 설정
      parent = regionRepository.findExactRegion(level1, null, null);
      if (parent == null) {
        // 부모가 없으면 먼저 생성
        parent = create(level1, null, null);
      }
      currentRegionName = level2;
    } else {
      // level1만 있으면 parent는 null (최상위)
      currentRegionName = level1;
    }
    
    Region newRegion = Region.builder()
      .regionName(currentRegionName)
      .parent(parent)
      .build();
      
    return regionRepository.save(newRegion);
  }

  @Transactional
  public void update(Region newRegion) {
    if(!regionRepository.existsById(newRegion.getId())) {
      throw new IllegalArgumentException("지역이 존재하지 않습니다.");
    }
    regionRepository.save(newRegion);
  }

  @Transactional
  public void delete(Long regionId) {
    regionRepository.deleteById(regionId);
  }
  
  /**
   * 지역 레벨 값을 정규화: "전체", 빈문자열 → null 변환
   */
  private String normalizeRegionLevel(String level) {
    if (level == null || level.trim().isEmpty() || "전체".equals(level)) {
      return null;
    }
    return level;
  }


}