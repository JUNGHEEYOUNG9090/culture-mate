package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.event.EventType;
import com.culturemate.culturemate_api.domain.member.InterestEventTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterestEventTypesRepository extends JpaRepository<InterestEventTypes, Long> {

  // 특정 멤버의 관심 이벤트 타입들 조회
  List<InterestEventTypes> findByMemberDetailId(Long memberDetailId);

  // 특정 멤버의 관심 이벤트 타입들 삭제
  void deleteByMemberDetailId(Long memberDetailId);

  // 특정 이벤트 타입의 인기도 조회
  @Query("SELECT COUNT(iet) FROM InterestEventTypes iet WHERE iet.eventType = :eventType")
  Long countByEventType(@Param("eventType") EventType eventType);
}