package com.culturemate.culturemate_api.domain.together;

import com.culturemate.culturemate_api.domain.chatting.ChatRoom;
import com.culturemate.culturemate_api.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Participants {

  //=== 필드 ===//
  @Id @GeneratedValue
  @Column(name = "participant_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "together_id")
  private Together together;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member participant;

  // 동행 요청 승인/거절
  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private ParticipationStatus status = ParticipationStatus.PENDING;

  // 신청용 1:1 채팅방 (호스트-신청자 간 개별 채팅, nullable로 설정하여 기존 데이터 호환성 유지)
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_chat_room_id", nullable = true)
  private ChatRoom applicationChatRoom;

  // 신청 메시지 (신청 시 첨부 메시지)
  @Setter
  private String message;

  // 생성/수정 시간
  private Instant createdAt;
  private Instant updatedAt;

  @PrePersist
  public void onCreate() {
    this.createdAt = Instant.now();
  }

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }

  // 신청용 채팅방 설정
  public void setApplicationChatRoom(ChatRoom applicationChatRoom) {
    this.applicationChatRoom = applicationChatRoom;
  }

}
