package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.community.Board;
import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.event.EventType;
import com.culturemate.culturemate_api.domain.member.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

  // 작성자로 조회 (N+1 해결: ManyToOne 관계만 포함)
  @EntityGraph(attributePaths = {"author", "event"})
  List<Board> findByAuthor(Member author);

  // 이벤트로 조회 (N+1 해결)
  @EntityGraph(attributePaths = {"author", "event"})
  List<Board> findByEvent(Event event);

  // 이벤트 타입으로 조회 (N+1 해결)
  @EntityGraph(attributePaths = {"author", "event"})
  List<Board> findByEventType(EventType eventType);

  // Title로 조회 (N+1 해결)
  @EntityGraph(attributePaths = {"author", "event"})
  List<Board> findByTitleContaining(String keyword);

  // 이벤트 + 제목 키워드 조건으로 조회 (N+1 해결)
  @EntityGraph(attributePaths = {"author", "event"})
  List<Board> findByEventAndTitleContaining(Event event, String keyword);

  // 통합 검색 메서드 (N+1 해결)
  @EntityGraph(attributePaths = {"author", "event", "author.memberDetail"})
  @Query("SELECT b FROM Board b " +
         "LEFT JOIN b.author.memberDetail md " +
         "WHERE (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
         "AND (:authorNickname IS NULL OR LOWER(md.nickname) LIKE LOWER(CONCAT('%', :authorNickname, '%'))) " +
         "AND (:event IS NULL OR b.event = :event) " +
         "AND (:eventType IS NULL OR b.event.eventType = :eventType)")
  List<Board> findBySearch(@Param("keyword") String keyword,
                          @Param("authorNickname") String authorNickname,
                          @Param("event") Event event,
                          @Param("eventType") EventType eventType);

  // 원자적 좋아요 카운트 업데이트
  @Modifying
  @Query("UPDATE Board b SET b.likeCount = b.likeCount + :increment WHERE b.id = :boardId")
  void updateLikeCount(@Param("boardId") Long boardId, @Param("increment") int increment);
}
