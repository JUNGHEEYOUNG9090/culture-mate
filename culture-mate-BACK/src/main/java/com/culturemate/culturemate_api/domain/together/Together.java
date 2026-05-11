package com.culturemate.culturemate_api.domain.together;

import com.culturemate.culturemate_api.domain.Region;
import com.culturemate.culturemate_api.domain.RegionSnapshot;
import com.culturemate.culturemate_api.domain.chatting.ChatRoom;
import com.culturemate.culturemate_api.domain.chatting.ChatRoomType;
import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.member.InterestEvents;
import com.culturemate.culturemate_api.domain.member.InterestTogethers;
import com.culturemate.culturemate_api.domain.member.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Together {

  //=== 필드 ===//
  @Id @GeneratedValue
  @Column(name = "together_id")
  private long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  @Setter
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "host_id", nullable = false)
  private Member host;

  // OneToMany는 실제 저장되는 속성이 아니고, 관계 매핑용
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "together", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Participants> participants = new ArrayList<>();

  @Column(nullable = false)
  @Setter
  private String title;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "region_id", nullable = false)
  private Region region;           // 지역ID (생성/수정용)

  // 조회 성능 최적화용 지역 스냅샷 (N+1 쿼리 문제 해결)
  @Embedded
  private RegionSnapshot regionSnapshot;

  @Column(nullable = false)
  @Setter
  private String meetingLocation;    // 모임장소 (카페명, 지하철역 등)

  @Column(nullable = false)
  @Setter
  private LocalDate meetingDate;

  @Column(nullable = false)
  @Setter
  private Integer maxParticipants;

  //  @Formula("(SELECT COUNT(*) FROM participants p WHERE p.together_id = id)")
  @Setter
  @Builder.Default
  private Integer participantCount = 1; // 호스트 포함

  @Column(length = 2000)
  @Setter
  private String content;

  @Setter
  @Builder.Default
  private boolean hostRecruitingEnabled = true; // 호스트가 명시적으로 설정한 모집 상태

  @Setter
  @Builder.Default
  private Integer interestCount = 0;

  @Setter
  private String thumbnailImagePath;  // 썸네일 이미지 경로

  @Setter 
  private String mainImagePath;       // 원본 이미지 경로

  private Instant createdAt;
  private Instant updatedAt;

  @OneToMany(mappedBy = "together", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<InterestTogethers> interestTogethers = new ArrayList<>();

  // Together ↔ ChatRooms 1:N 관계 (GROUP_CHAT + APPLICATION_CHAT)
  @OneToMany(mappedBy = "together", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<ChatRoom> chatRooms = new ArrayList<>();

  //=== 생성/수정 로직 ===//
  @PrePersist
  public void onCreate() {
    this.createdAt = Instant.now();
  }

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }

  /**
   * 지역 스냅샷 업데이트
   * Region 엔티티 변경 시 반드시 호출하여 스냅샷 동기화
   * 
   * @param region 새로운 지역 정보
   */
  public void updateRegionSnapshot(Region region) {
    this.regionSnapshot = RegionSnapshot.from(region);
  }

  /**
   * 지역 정보 변경 시 스냅샷 자동 동기화
   * setRegion() 호출 시 자동으로 스냅샷이 업데이트됨
   *
   * @param region 새로운 지역 정보
   */
  public void setRegion(Region region) {
    this.region = region;
    updateRegionSnapshot(region);
  }

  //=== 채팅방 관련 편의 메서드 ===//

  /**
   * 그룹 채팅방 조회 (GROUP_CHAT 타입)
   * Together당 하나만 존재해야 함
   *
   * @return 그룹 채팅방 또는 null
   */
  public ChatRoom getGroupChatRoom() {
    return chatRooms.stream()
        .filter(room -> room.getType() == ChatRoomType.GROUP_CHAT)
        .findFirst()
        .orElse(null);
  }

  /**
   * 신청용 채팅방 목록 조회 (APPLICATION_CHAT 타입)
   * 신청자별로 여러 개 존재 가능
   *
   * @return 신청용 채팅방 리스트
   */
  public List<ChatRoom> getApplicationChatRooms() {
    return chatRooms.stream()
        .filter(room -> room.getType() == ChatRoomType.APPLICATION_CHAT)
        .collect(Collectors.toList());
  }

  /**
   * 특정 신청자와의 채팅방 조회
   *
   * @param applicant 신청자
   * @return 해당 신청자와의 채팅방 또는 null
   */
  public ChatRoom getApplicationChatRoom(Member applicant) {
    return chatRooms.stream()
        .filter(room -> room.getType() == ChatRoomType.APPLICATION_CHAT)
        .filter(room -> room.getApplicant() != null && room.getApplicant().equals(applicant))
        .findFirst()
        .orElse(null);
  }

}
