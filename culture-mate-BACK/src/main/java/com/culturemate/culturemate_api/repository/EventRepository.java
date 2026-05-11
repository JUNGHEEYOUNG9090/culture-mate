package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.Region;
import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.event.EventType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

  @Override
  @EntityGraph(attributePaths = {"ticketPrice", "region"})
  Optional<Event> findById(Long id);

//  통합 검색
  @EntityGraph(attributePaths = {"region", "ticketPrice"})
  @Query("""
  SELECT e FROM Event e
  WHERE (:keyword IS NULL OR :keyword = '' OR e.title LIKE concat('%', :keyword, '%'))
    AND e.region IN :regions
    AND (:startDate IS NULL OR e.endDate >= :startDate)
    AND (:endDate IS NULL OR e.startDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    """)
  List<Event> findBySearch(@Param("keyword") String keyword,
                          @Param("regions") List<Region> regions,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate,
                          @Param("eventType") EventType eventType);

  // 지역 조건 없는 검색
  @EntityGraph(attributePaths = {"region", "ticketPrice"})
  @Query("""
  SELECT e FROM Event e
  WHERE (:keyword IS NULL OR :keyword = '' OR e.title LIKE concat('%', :keyword, '%'))
    AND (:startDate IS NULL OR e.endDate >= :startDate)
    AND (:endDate IS NULL OR e.startDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    """)
  List<Event> findBySearchWithoutRegion(@Param("keyword") String keyword,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("eventType") EventType eventType);

  // 통합 검색 (Pageable 지원) - 오버로딩
  @EntityGraph(attributePaths = {"region", "ticketPrice"})
  @Query("""
  SELECT e FROM Event e
  WHERE (:keyword IS NULL OR :keyword = '' OR e.title LIKE concat('%', :keyword, '%'))
    AND e.region IN :regions
    AND (:startDate IS NULL OR e.endDate >= :startDate)
    AND (:endDate IS NULL OR e.startDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    """)
  List<Event> findBySearch(@Param("keyword") String keyword,
                          @Param("regions") List<Region> regions,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate,
                          @Param("eventType") EventType eventType,
                          Pageable pageable);

  // 지역 조건 없는 검색 (Pageable 지원) - 오버로딩
  @EntityGraph(attributePaths = {"region", "ticketPrice"})
  @Query("""
  SELECT e FROM Event e
  WHERE (:keyword IS NULL OR :keyword = '' OR e.title LIKE concat('%', :keyword, '%'))
    AND (:startDate IS NULL OR e.endDate >= :startDate)
    AND (:endDate IS NULL OR e.startDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    """)
  List<Event> findBySearchWithoutRegion(@Param("keyword") String keyword,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("eventType") EventType eventType,
                                       Pageable pageable);

  // 검색 조건에 해당하는 전체 개수 조회 (지역 조건 포함)
  @Query("""
  SELECT COUNT(e) FROM Event e
  WHERE (:keyword IS NULL OR :keyword = '' OR e.title LIKE concat('%', :keyword, '%'))
    AND e.region IN :regions
    AND (:startDate IS NULL OR e.endDate >= :startDate)
    AND (:endDate IS NULL OR e.startDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    """)
  long countBySearch(@Param("keyword") String keyword,
                     @Param("regions") List<Region> regions,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     @Param("eventType") EventType eventType);

  // 검색 조건에 해당하는 전체 개수 조회 (지역 조건 없음)
  @Query("""
  SELECT COUNT(e) FROM Event e
  WHERE (:keyword IS NULL OR :keyword = '' OR e.title LIKE concat('%', :keyword, '%'))
    AND (:startDate IS NULL OR e.endDate >= :startDate)
    AND (:endDate IS NULL OR e.startDate <= :endDate)
    AND (:eventType IS NULL OR e.eventType = :eventType)
    """)
  long countBySearchWithoutRegion(@Param("keyword") String keyword,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  @Param("eventType") EventType eventType);

  // 원자적 관심수 카운트 업데이트
  @Modifying
  @Query("UPDATE Event e SET e.interestCount = e.interestCount + :increment WHERE e.id = :eventId")
  void updateInterestCount(@Param("eventId") Long eventId, @Param("increment") int increment);

  // 원자적 리뷰수 카운트 업데이트
  @Modifying
  @Query("UPDATE Event e SET e.reviewCount = e.reviewCount + :increment WHERE e.id = :eventId")
  void updateReviewCount(@Param("eventId") Long eventId, @Param("increment") int increment);

  // 최신 활성 이벤트 조회 (메인 페이지용)
  @EntityGraph(attributePaths = {"region", "ticketPrice"})
  @Query("""
    SELECT e FROM Event e
    WHERE e.endDate >= :today
    ORDER BY e.createdAt DESC
    """)
  List<Event> findRecentActiveWithLimit(@Param("today") LocalDate today, Pageable pageable);

}
