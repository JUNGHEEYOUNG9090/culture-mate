package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.Region;
import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.event.EventType;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.together.ParticipationStatus;
import com.culturemate.culturemate_api.domain.together.Together;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface TogetherRepository extends JpaRepository<Together, Long> {

  @EntityGraph(attributePaths = {"event", "host", "region", "chatRooms"})
  List<Together> findByHost(Member host);
  
  // 참여자의 모든 참여 내역 (상태 무관) => 특정 참여자의 신청내역을 조회할 때 사용
  @EntityGraph(attributePaths = {"event", "host", "region", "chatRooms"})
  @Query("SELECT t FROM Together t " +
         "JOIN t.participants p " +
         "WHERE p.participant = :member " +
         "ORDER BY p.id DESC")
  List<Together> findByParticipantAll(@Param("member") Member participant);

  // 참여자의 특정 상태 참여 내역
  @EntityGraph(attributePaths = {"event", "host", "region", "chatRooms"})
  @Query("SELECT t FROM Together t " +
         "JOIN t.participants p " +
         "WHERE p.participant = :member " +
         "AND p.status = :status " +
         "ORDER BY p.id DESC")
  List<Together> findByParticipantAndStatus(@Param("member") Member participant, @Param("status") ParticipationStatus status);

//  통합 검색
  @EntityGraph(attributePaths = {"event", "host", "region", "chatRooms"})
  @Query("""
  SELECT t
  FROM Together t
  JOIN t.event e
  WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:startDate IS NULL OR t.meetingDate >= :startDate)
    AND (:endDate   IS NULL OR t.meetingDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    AND (:eventId   IS NULL OR e.id = :eventId)
    AND t.region IN :regions
    """)
  List<Together> findBySearch(@Param("keyword") String keyword,
                              @Param("regions") List<Region> regions,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("eventType") EventType eventType,
                              @Param("eventId") Long eventId);

  // 지역 조건 없는 검색 (기존 쿼리에서 region 조건만 제거)
  @EntityGraph(attributePaths = {"event", "host"})
  @Query("""
  SELECT t
  FROM Together t
  JOIN t.event e
  WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:startDate IS NULL OR t.meetingDate >= :startDate)
    AND (:endDate   IS NULL OR t.meetingDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    AND (:eventId   IS NULL OR e.id = :eventId)
    """)
  List<Together> findBySearchWithoutRegion(@Param("keyword") String keyword,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("eventType") EventType eventType,
                              @Param("eventId") Long eventId);

  // 통합 검색 (Pageable 지원) - 오버로딩
  @EntityGraph(attributePaths = {"event", "host", "region", "chatRooms"})
  @Query("""
  SELECT t
  FROM Together t
  JOIN t.event e
  WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:startDate IS NULL OR t.meetingDate >= :startDate)
    AND (:endDate   IS NULL OR t.meetingDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    AND (:eventId   IS NULL OR e.id = :eventId)
    AND t.region IN :regions
    """)
  List<Together> findBySearch(@Param("keyword") String keyword,
                              @Param("regions") List<Region> regions,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("eventType") EventType eventType,
                              @Param("eventId") Long eventId,
                              Pageable pageable);

  // 지역 조건 없는 검색 (Pageable 지원) - 오버로딩
  @EntityGraph(attributePaths = {"event", "host"})
  @Query("""
  SELECT t
  FROM Together t
  JOIN t.event e
  WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:startDate IS NULL OR t.meetingDate >= :startDate)
    AND (:endDate   IS NULL OR t.meetingDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    AND (:eventId   IS NULL OR e.id = :eventId)
    """)
  List<Together> findBySearchWithoutRegion(@Param("keyword") String keyword,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("eventType") EventType eventType,
                              @Param("eventId") Long eventId,
                              Pageable pageable);

  // 원자적 참여자 수 카운트 업데이트 (단순)
  @Modifying
  @Query("UPDATE Together t SET t.participantCount = t.participantCount + :increment WHERE t.id = :togetherId")
  void updateParticipantCount(@Param("togetherId") Long togetherId, @Param("increment") int increment);

  // 원자적 관심수 카운트 업데이트
  @Modifying
  @Query("UPDATE Together t SET t.interestCount = t.interestCount + :increment WHERE t.id = :togetherId")
  void updateInterestCount(@Param("togetherId") Long togetherId, @Param("increment") int increment);

  // 검색 조건에 해당하는 전체 개수 조회 (지역 조건 포함)
  @Query("""
  SELECT COUNT(t)
  FROM Together t
  JOIN t.event e
  WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:startDate IS NULL OR t.meetingDate >= :startDate)
    AND (:endDate   IS NULL OR t.meetingDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    AND (:eventId   IS NULL OR e.id = :eventId)
    AND t.region IN :regions
    """)
  long countBySearch(@Param("keyword") String keyword,
                     @Param("regions") List<Region> regions,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     @Param("eventType") EventType eventType,
                     @Param("eventId") Long eventId);

  // 검색 조건에 해당하는 전체 개수 조회 (지역 조건 없음)
  @Query("""
  SELECT COUNT(t)
  FROM Together t
  JOIN t.event e
  WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:startDate IS NULL OR t.meetingDate >= :startDate)
    AND (:endDate   IS NULL OR t.meetingDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    AND (:eventId   IS NULL OR e.id = :eventId)
    """)
  long countBySearchWithoutRegion(@Param("keyword") String keyword,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  @Param("eventType") EventType eventType,
                                  @Param("eventId") Long eventId);

  // 최신 활성 모임 조회 (메인 페이지용)
  @EntityGraph(attributePaths = {"event", "host", "regionSnapshot"})
  @Query("""
    SELECT t FROM Together t
    WHERE t.hostRecruitingEnabled = true
    AND t.meetingDate > :today
    AND t.participantCount < t.maxParticipants
    ORDER BY t.createdAt DESC
    """)
  List<Together> findRecentActiveWithLimit(@Param("today") LocalDate today, Pageable pageable);

}
