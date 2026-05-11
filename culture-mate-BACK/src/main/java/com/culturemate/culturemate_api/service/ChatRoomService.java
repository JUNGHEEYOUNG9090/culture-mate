package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.chatting.ChatMember;
import com.culturemate.culturemate_api.domain.chatting.ChatMessage;
import com.culturemate.culturemate_api.domain.chatting.ChatRoom;
import com.culturemate.culturemate_api.domain.chatting.ChatRoomType;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.together.Participants;
import com.culturemate.culturemate_api.domain.together.ParticipationStatus;
import com.culturemate.culturemate_api.domain.together.Together;
import com.culturemate.culturemate_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final ChatMemberRepository chatMemberRepository;
  private final MemberService memberService;
  // NOTE: 순환 참조를 피하기 위해 Together 도메인의 Repository를 직접 참조. 리팩토링 대상.
  private final ParticipantsRepository participantsRepository;

  // 채팅방 생성
  @Transactional
  public ChatRoom createChatRoom(Together together) {
    ChatRoom chatRoom = ChatRoom.builder()
      .roomName(together.getTitle())
      .together(together)
      .build();
    return chatRoomRepository.save(chatRoom);
  }

  // 채팅방 생성 (일반)
  @Transactional
  public ChatRoom createChatRoom() {
    ChatRoom chatRoom = ChatRoom.builder()
      .build();
    return chatRoomRepository.save(chatRoom);
  }

  // 신청용 1:1 채팅방 생성 (호스트-신청자 매칭)
  @Transactional
  public ChatRoom createApplicationChatRoom(Together together, Member applicant) {
    String roomName = String.format("[%s] 신청 문의", together.getTitle());

    ChatRoom applicationChatRoom = ChatRoom.builder()
      .roomName(roomName)
      .together(together)
      .type(ChatRoomType.APPLICATION_CHAT)
      .applicant(applicant)
      .build();

    ChatRoom savedChatRoom = chatRoomRepository.save(applicationChatRoom);

    // 호스트와 신청자를 채팅방에 자동 추가
    addMemberToRoom(savedChatRoom.getId(), together.getHost().getId());
    addMemberToRoom(savedChatRoom.getId(), applicant.getId());

    return savedChatRoom;
  }

  // 모든 채팅방 검색
  public List<ChatRoom> findAllRoom() {
    return chatRoomRepository.findAll();
  }

  // 특정 채팅방 검색
  public ChatRoom findById(Long roomId) {
    return chatRoomRepository.findById(roomId)
      .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다: " + roomId));
  }

  // 특정 동행의 그룹 채팅방 검색 (안전한 조회)
  public ChatRoom findGroupChatByTogether(Together together) {
    return chatRoomRepository.findGroupChatByTogether(together)
      .orElseThrow(() -> new IllegalArgumentException("해당 모집글의 그룹 채팅방이 존재하지 않습니다.: " + together.getId()));
  }

  // 특정 동행의 채팅방 검색 (기존 호환성 유지 - 그룹챗 반환)
  public ChatRoom findByTogether(Together together) {
    return findGroupChatByTogether(together);
  }

  // 특정 동행의 모든 채팅방 검색
  public List<ChatRoom> findAllChatRoomsByTogether(Together together) {
    List<ChatRoom> allRooms = new ArrayList<>();
    allRooms.addAll(chatRoomRepository.findByTogetherAndType(together, ChatRoomType.GROUP_CHAT));
    allRooms.addAll(chatRoomRepository.findByTogetherAndType(together, ChatRoomType.APPLICATION_CHAT));
    return allRooms;
  }

  // 특정 동행의 신청용 채팅방들 검색
  public List<ChatRoom> findApplicationChatsByTogether(Together together) {
    return chatRoomRepository.findApplicationChatsByTogether(together);
  }

  // 채팅방 멤버 추가 (내부용, 권한 검증 없음)
  @Transactional
  public void addMemberToRoom(Long roomId, Long memberId) {
    ChatRoom chatRoom = findById(roomId);
    Member member = memberService.findById(memberId);

    if (chatMemberRepository.findByChatRoomAndMember(chatRoom, member).isPresent()) {
      return;
    }

    ChatMember chatMember = ChatMember.builder()
      .chatRoom(chatRoom)
      .member(member)
      .build();
    chatMemberRepository.save(chatMember);
  }

  /**
   * 채팅방에 멤버를 추가 (권한 검증 포함)
   * - Together 채팅방의 경우, 호스트 또는 승인된 멤버만 참여 가능
   */
  @Transactional
  public void checkAndAddMemberToRoom(Long roomId, Long memberId) {
    ChatRoom chatRoom = findById(roomId);
    Together together = chatRoom.getTogether();

    // 1. Together와 연결된 채팅방인 경우 권한 검증
    if (together != null) {
      boolean isHost = together.getHost().getId().equals(memberId);

      Participants participation = participantsRepository.findByTogetherIdAndParticipantId(together.getId(), memberId);
      boolean isApproved = participation != null && participation.getStatus() == ParticipationStatus.APPROVED;

      if (!isHost && !isApproved) {
        throw new SecurityException("이 채팅방에 참여할 권한이 없습니다. 호스트의 승인이 필요합니다.");
      }
    }
    // else: Together와 연결되지 않은 일반 채팅방은 누구나 참여 가능 (현재 정책)

    // 2. 권한이 있으면 멤버 추가
    addMemberToRoom(roomId, memberId);
  }


  // 특정 모집글의 그룹 톡방에 멤버 추가
  @Transactional
  public void addMemberToRoomByTogether(Together together, Long memberId) {
    chatRoomRepository.findGroupChatByTogether(together).ifPresent(chatRoom ->
      addMemberToRoom(chatRoom.getId(), memberId)
    );
  }

  // 채팅방에서 멤버 제거
  @Transactional
  public void removeMemberFromRoom(Long roomId, Long memberId) {
    ChatRoom chatRoom = findById(roomId);
    Member member = memberService.findById(memberId);
    
    chatMemberRepository.deleteByChatRoomAndMember(chatRoom, member);
  }

  // 특정 모집글의 그룹 톡방에서 멤버 제거
  @Transactional
  public void removeMemberFromRoomByTogether(Together together, Long memberId) {
    chatRoomRepository.findGroupChatByTogether(together).ifPresent(chatRoom ->
      removeMemberFromRoom(chatRoom.getId(), memberId)
    );
  }

  // 채팅
  @Transactional
  public ChatMessage sendMessage(Long roomId, Long senderId, String content) {
    // 1. 메시지 내용 검증
    validateMessageContent(content);

    // 2. 발신자 조회
    Member sender = memberService.findById(senderId);

    // 3. 채팅방 조회
    ChatRoom room = findById(roomId);

    // 4. 채팅방 멤버 권한 확인
    ChatMember author = chatMemberRepository.findByChatRoomAndMember(room, sender)
        .orElse(null);

    // 4-1. ChatMember가 없는 경우, Together 호스트인지 확인
    if (author == null && room.getTogether() != null) {
      Together together = room.getTogether();

      // 호스트이면서 HOST 상태로 참여자에 등록되어 있는지 확인
      if (together.getHost().getId().equals(senderId)) {
        // 호스트를 ChatMember로 자동 추가 (이미 있어야 하지만 혹시 누락된 경우 대비)
        author = ChatMember.builder()
            .chatRoom(room)
            .member(sender)
            .build();
        author = chatMemberRepository.save(author);
      } else {
        // 일반 참여자인 경우 Participants 테이블에서 HOST 또는 APPROVED 상태 확인
        Participants participant = participantsRepository
            .findByTogetherIdAndParticipantId(together.getId(), sender.getId());
        boolean isValidParticipant = participant != null &&
            (participant.getStatus() == ParticipationStatus.HOST ||
             participant.getStatus() == ParticipationStatus.APPROVED);

        if (isValidParticipant) {
          // 유효한 참여자이지만 ChatMember에 없는 경우 추가
          author = ChatMember.builder()
              .chatRoom(room)
              .member(sender)
              .build();
          author = chatMemberRepository.save(author);
        }
      }
    }

    // 4-2. 여전히 author가 null이면 권한 없음
    if (author == null) {
      throw new IllegalArgumentException("해당 채팅방의 참여자가 아닙니다");
    }

    // 5. 메시지 저장
    ChatMessage message = ChatMessage.builder()
        .chatRoom(room)
        .author(author)
        .content(content)
        .createdAt(Instant.now())
        .build();

    System.out.println("DEBUG: 메시지 저장 시도 - roomId: " + room.getId() + ", authorId: " + author.getId() + ", content: " + content);

    try {
      ChatMessage savedMessage = chatMessageRepository.save(message);
      System.out.println("DEBUG: 메시지 저장 성공 - messageId: " + savedMessage.getId() + ", createdAt: " + savedMessage.getCreatedAt());
      return savedMessage;
    } catch (Exception e) {
      System.out.println("ERROR: 메시지 저장 실패 - " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }

  // 메시지 내용 검증
  private void validateMessageContent(String content) {
    if (content == null || content.trim().isEmpty()) {
      throw new IllegalArgumentException("메시지 내용이 비어있습니다");
    }
    if (content.length() > 1000) {
      throw new IllegalArgumentException("메시지가 너무 깁니다 (최대 1000자)");
    }
  }

  // 이전대화 불러오기 (페이지네이션 적용)
  public Page<ChatMessage> getMessagesByRoomId(Long roomId, Pageable pageable) {
    return chatMessageRepository.findByChatRoomId(roomId, pageable);
  }

  // 특정 멤버가 참여중인 채팅방 목록 조회
  public List<ChatRoom> findRoomsByMember(Long memberId) {
    Member member = memberService.findById(memberId);
    List<ChatMember> chatMembers = chatMemberRepository.findByMember(member);
    return chatMembers.stream()
        .map(ChatMember::getChatRoom)
        .collect(Collectors.toList());
  }

  // 멤버가 해당 채팅방에 접근 권한이 있는지 확인
  public boolean canAccessChatRoom(Long roomId, Long memberId) {
    ChatRoom chatRoom = findById(roomId);
    Member member = memberService.findById(memberId);

    // 1. ChatMember로 직접 참여한 경우 (일반 채팅방 또는 Together 승인된 참여자)
    if (chatMemberRepository.findByChatRoomAndMember(chatRoom, member).isPresent()) {
      return true;
    }

    // 2. Together 채팅방의 경우 호스트 권한 확인
    if (chatRoom.getTogether() != null) {
      Together together = chatRoom.getTogether();
      // 호스트인 경우
      if (together.getHost().getId().equals(memberId)) {
        return true;
      }
    }

    return false;
  }

  // 간단한 멤버십 확인 (ChatMember 테이블만 확인)
  public boolean isMemberOfRoom(Long roomId, Long memberId) {
    ChatRoom chatRoom = findById(roomId);
    Member member = memberService.findById(memberId);
    return chatMemberRepository.findByChatRoomAndMember(chatRoom, member).isPresent();
  }
}