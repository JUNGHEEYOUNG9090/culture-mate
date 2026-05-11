package com.culturemate.culturemate_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.culturemate.culturemate_api.domain.together.Participants;
import com.culturemate.culturemate_api.domain.together.ParticipationStatus;

@Repository
public interface ParticipantsRepository extends JpaRepository<Participants, Long> {

//  특정 Together의 모든 참여자 조회
  @EntityGraph(attributePaths = {"participant"})
  List<Participants> findByTogetherId(Long togetherId);

//  특정 Together에 특정 Member가 참여하는지 조회
  @Query("SELECT p FROM Participants p WHERE p.together.id = :togetherId AND p.participant.id = :participantId")
  Participants findByTogetherIdAndParticipantId(@Param("togetherId") Long togetherId, 
                                                @Param("participantId") Long participantId);

//  특정 Together에 특정 Member가 참여하는지 존재 여부만 확인
//  Spring Data JPA 메서드명 규칙 사용 (더 간단)
  boolean existsByTogetherIdAndParticipantId(Long togetherId, Long participantId);

//  모든 신청자 조회 (상태 무관)
  @EntityGraph(attributePaths = {"participant"})
  List<Participants> findAllByTogetherId(Long togetherId);

//  특정 상태의 참여자만 조회
  @EntityGraph(attributePaths = {"participant"})
  @Query("SELECT p FROM Participants p WHERE p.together.id = :togetherId AND p.status = :status")
  List<Participants> findByTogetherIdAndStatus(@Param("togetherId") Long togetherId, @Param("status") ParticipationStatus status);

//  여러 상태의 참여자 조회 (예: APPROVED + HOST)
  @EntityGraph(attributePaths = {"participant", "participant.memberDetail"})
  @Query("SELECT p FROM Participants p WHERE p.together.id = :togetherId AND p.status IN :statuses")
  List<Participants> findByTogetherIdAndStatusIn(@Param("togetherId") Long togetherId, @Param("statuses") List<ParticipationStatus> statuses);

  // 호스트가 받은 신청 목록 조회 (채팅방 정보는 lazy loading으로 처리)
  @EntityGraph(attributePaths = {"participant", "participant.memberDetail", "together", "together.event"})
  @Query("SELECT p FROM Participants p WHERE p.together.host.id = :hostId AND p.status IN :statuses ORDER BY p.createdAt DESC")
  List<Participants> findByTogether_HostIdAndStatusInOrderByCreatedAtDesc(
      @Param("hostId") Long hostId,
      @Param("statuses") List<ParticipationStatus> statuses);

//  특정 Together에 대한 모든 참여자 삭제
  void deleteByTogetherId(Long togetherId);

  // 참여자가 보낸 신청 목록 조회
  @EntityGraph(attributePaths = {"participant", "participant.memberDetail", "together", "together.event", "together.host", "together.host.memberDetail"})
  @Query("SELECT p FROM Participants p WHERE p.participant.id = :participantId AND p.status IN :statuses ORDER BY p.createdAt DESC")
  List<Participants> findByParticipant_IdAndStatusInOrderByCreatedAtDesc(
      @Param("participantId") Long participantId,
      @Param("statuses") List<ParticipationStatus> statuses);

}
