package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.event.EventReview;
import com.culturemate.culturemate_api.domain.member.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventReviewRepository extends JpaRepository<EventReview, Long> {

  @EntityGraph(attributePaths = {"event", "member"})
  List<EventReview> findByEvent(Event event);
  
  @EntityGraph(attributePaths = {"event", "member"})
  List<EventReview> findByEventOrderByCreatedAtDesc(Event event);

  @EntityGraph(attributePaths = {"event", "member"})
  List<EventReview> findByMember(Member member);

  List<EventReview> findByMemberId(Long memberId);

  boolean existsByMemberAndEvent(Member member, Event event);
}
