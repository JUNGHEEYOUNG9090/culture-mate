package com.culturemate.culturemate_api.dto;

import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.MemberStatus;
import com.culturemate.culturemate_api.domain.member.Role;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.Collection;

@Getter
public class AuthenticatedUser extends User {
  private final Long memberId;
  private final String loginId;
  private final Role role;
  private final MemberStatus status;
  private final String nickname;

  @JsonCreator
  public AuthenticatedUser(
      @JsonProperty("memberId") Long memberId,
      @JsonProperty("loginId") String loginId,
      @JsonProperty("role") Role role,
      @JsonProperty("status") MemberStatus status,
      @JsonProperty("nickname") String nickname,
      @JsonProperty("password") String password,
      @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities) {
    super(
        loginId != null ? loginId : "unknown",
        password != null ? password : "",
        authorities != null ? authorities : new ArrayList<>()
    );
    this.memberId = memberId;
    this.loginId = loginId;
    this.role = role;
    this.status = status;
    this.nickname = nickname;
  }

  /**
   * Member 엔티티에서 경량 AuthenticatedUser 생성
   */
  public static AuthenticatedUser from(Member member, Collection<? extends GrantedAuthority> authorities) {
    if (member == null) {
      throw new IllegalArgumentException("Member cannot be null");
    }

    String nickname = member.getMemberDetail() != null ?
        member.getMemberDetail().getNickname() : member.getLoginId();

    return new AuthenticatedUser(
        member.getId(),
        member.getLoginId(),
        member.getRole(),
        member.getStatus(),
        nickname,
        member.getPassword(),
        authorities
    );
  }

  // 기존 호환성을 위한 편의 메서드들
  public Long getMemberId() {
    return memberId;
  }

  public String getLoginId() {
    return loginId;
  }

  public String getNickname() {
    return nickname;
  }

  public Role getRole() {
    return role;
  }

  public MemberStatus getStatus() {
    return status;
  }
}
