package com.culturemate.culturemate_api.facade;

import com.culturemate.culturemate_api.domain.chatting.ChatRoom;
import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.Region;
import com.culturemate.culturemate_api.domain.together.Together;
import com.culturemate.culturemate_api.dto.TogetherDto;
import com.culturemate.culturemate_api.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Together 관련 복합 비즈니스 로직을 처리하는 Facade
 * - 여러 서비스를 조합하여 복잡한 워크플로우 제공
 * - Controller의 복잡성 제거
 * - 트랜잭션 경계 관리
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TogetherFacade {

  private final TogetherService togetherService;
  private final MemberService memberService;
  private final EventService eventService;
  private final RegionService regionService;
  private final ChatRoomService chatRoomService;

  /**
   * Together 생성과 동시에 채팅방 생성 및 호스트 참여 처리
   * - Together 엔티티 생성
   * - 호스트를 참여자로 자동 추가
   * - 채팅방 생성 및 호스트 추가
   *
   * @param requestDto Together 생성 요청 데이터
   * @return 생성된 Together 엔티티
   */
  @Transactional
  public Together createTogetherWithChatRoom(TogetherDto.Request requestDto) {
    // 1. 필요한 엔티티들 조회
    Event event = eventService.findById(requestDto.getEventId());
    Member host = memberService.findById(requestDto.getHostId());
    Region region = regionService.findExact(requestDto.getRegion());

    // 2. Together 생성 (호스트 참여자 추가 포함)
    Together savedTogether = togetherService.create(event, host, region, requestDto);

    // 3. 채팅방 생성 및 호스트 추가
    ChatRoom chatRoom = chatRoomService.createChatRoom(savedTogether);
    chatRoomService.addMemberToRoom(chatRoom.getId(), host.getId());

    return savedTogether;
  }

  /**
   * 참여 승인과 동시에 채팅방 참여 처리
   * - 참여 상태를 APPROVED로 변경
   * - 채팅방에 참여자 추가
   * - 알림 발송 등 추가 처리 가능
   *
   * @param togetherId Together ID
   * @param participantId 참여자 ID
   * @param hostId 호스트 ID (권한 검증용)
   */
  @Transactional
  public void approveParticipationWithChatRoom(Long togetherId, Long participantId, Long hostId) {
    // 1. 참여 승인 처리 (권한 검증 포함)
    togetherService.approveParticipation(togetherId, participantId, hostId);

    // 2. 채팅방에 참여자 추가
    Together together = togetherService.findById(togetherId);
    ChatRoom chatRoom = chatRoomService.findGroupChatByTogether(together);
    chatRoomService.addMemberToRoom(chatRoom.getId(), participantId);

    // 3. 추가 처리 (알림 등) - 향후 확장 가능
    // notificationService.notifyParticipationApproved(together, participantId);
  }

  /**
   * 참여 거절 처리
   * - 참여 상태를 REJECTED로 변경
   * - 필요시 알림 발송
   *
   * @param togetherId Together ID
   * @param participantId 참여자 ID
   * @param hostId 호스트 ID (권한 검증용)
   */
  @Transactional
  public void rejectParticipation(Long togetherId, Long participantId, Long hostId) {
    // 1. 참여 거절 처리 (권한 검증 포함)
    togetherService.rejectParticipation(togetherId, participantId, hostId);

    // 2. 추가 처리 (알림 등) - 향후 확장 가능
    // notificationService.notifyParticipationRejected(together, participantId);
  }


  /**
   * Together 삭제와 함께 관련 채팅방도 삭제
   * - Together 삭제
   * - 관련 채팅방 삭제
   * - 관련 이미지 삭제 등
   *
   * @param togetherId Together ID
   * @param requesterId 요청자 ID (권한 검증용)
   */
  @Transactional
  public void deleteTogetherWithRelatedData(Long togetherId, Long requesterId) {
    // 1. Together 삭제 (권한 검증 및 관련 데이터 삭제 포함)
    // TogetherService.delete()에서 이미 채팅방, 참여자, 관심 등록 모두 삭제 처리
    togetherService.delete(togetherId, requesterId);

    // 2. 추가 정리 작업 (이미지 등) - 향후 확장 가능
    // imageService.deleteTogetherImages(togetherId);
  }
}