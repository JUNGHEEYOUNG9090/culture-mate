package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.repository.InterestTagsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

  private final InterestTagsRepository interestTagsRepository;

  // 특정 태그 사용 횟수 조회
  public Long getTagUsageCount(String tagName) {
    return interestTagsRepository.countByTag(tagName);
  }

  // 모든 태그별 사용 횟수 조회
  public List<Object[]> getAllTagUsageCounts() {
    return interestTagsRepository.findTagUsageCounts();
  }

  // 인기 태그 목록 조회 (상위 N개)
  public List<Object[]> getPopularTags(int limit) {
    return interestTagsRepository.findTopTagsWithCount(limit);
  }
}