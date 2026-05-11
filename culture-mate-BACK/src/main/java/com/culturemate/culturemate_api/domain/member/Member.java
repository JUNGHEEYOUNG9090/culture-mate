package com.culturemate.culturemate_api.domain.member;

import com.culturemate.culturemate_api.domain.community.Board;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member {

  //=== 필드 ===//
  @Id @GeneratedValue
  @Column(name = "member_id")
  private Long id;

  @Column(unique = true)
  private String loginId;

  private String password;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private Role role = Role.MEMBER;


  private Instant joinedAt;
  private Instant updatedAt;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private MemberStatus status = MemberStatus.ACTIVE;

  @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private MemberDetail memberDetail;

  @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<Board> boards = new ArrayList<>();

  @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<InterestEvents> interestEvents = new ArrayList<>();

  @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<InterestTogethers> interestTogethers = new ArrayList<>();

  //=== 생성/수정 로직 ===//
  @PrePersist
  public void onCreate() {
    this.joinedAt = Instant.now();
  }

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }

  public void changeStatus(MemberStatus newstatus) {
    this.status = newstatus;
  }

  public void changePassword(String newPassword) {
    this.password = newPassword;
  }

  public void changeRole(Role newRole) {
    this.role = newRole;
  }
}
