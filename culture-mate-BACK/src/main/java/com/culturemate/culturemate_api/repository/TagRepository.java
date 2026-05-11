package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.statistics.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, String> {

  // 단일 태그 조회 (ID가 tag이므로)
  Optional<Tag> findByTag(String tag);

  // 여러 태그를 한 번에 조회
  List<Tag> findByTagIn(List<String> tags);
}