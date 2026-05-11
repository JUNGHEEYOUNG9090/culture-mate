package com.culturemate.culturemate_api.domain.chatting;

import com.culturemate.culturemate_api.domain.community.Board;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.together.Together;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatRoom {
  @Id
  @GeneratedValue
  @Column(name = "room_id")
  private Long id;

  private String roomName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "together_id", nullable = true)
  private Together together;

  // 채팅방 타입 구분 (nullable로 설정하여 기존 데이터 호환성 유지)
  @Enumerated(EnumType.STRING)
  @Column(nullable = true)
  @Builder.Default
  private ChatRoomType type = ChatRoomType.GROUP_CHAT;

  // 신청용 채팅방의 경우 신청자 정보 (APPLICATION_CHAT 타입인 경우에만 사용)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "applicant_id", nullable = true)
  private Member applicant;

  @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<ChatMember> chatMembers = new ArrayList<>();

  // N+1 문제를 피하기 위해 Formula 사용
  @Formula("(select count(*) from chat_member cm where cm.room_id = room_id)")
  private int chatMemberCount;

  @Column(nullable = false)
  private Instant createdAt;

  //=== 생성/수정 로직 ===//
  @PrePersist
  public void onCreate() {
    this.createdAt = Instant.now();
  }
}
