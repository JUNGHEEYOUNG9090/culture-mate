package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.Role;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import com.culturemate.culturemate_api.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AuthService implements UserDetailsService {

  private final MemberRepository memberRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("사용자 인증 시도: {}", username);

    try {
      var result = memberRepository.findByLoginId(username);

      if(result.isEmpty()){
        log.warn("존재하지 않는 사용자 인증 시도: {}", username);
        throw new UsernameNotFoundException("존재하지 않는 사용자입니다: " + username);
      }

      var member = result.get();
      log.debug("사용자 인증 성공: {} (ID: {}, Role: {}, Status: {})",
          member.getLoginId(), member.getId(), member.getRole(), member.getStatus());

      List<GrantedAuthority> authorities = new ArrayList<>();
      authorities.add(new SimpleGrantedAuthority(member.getRole().name()));

      return AuthenticatedUser.from(member, authorities);
    } catch (UsernameNotFoundException e) {
      throw e;
    } catch (Exception e) {
      log.error("사용자 인증 처리 중 오류 발생: {}", username, e);
      throw e;
    }
  }


  // 권한 검증
  public void validateProfileAccess(Long profileMemberId, Long requesterId) {
    Member requester = memberRepository.findById(requesterId)
        .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

    // ADMIN은 모든 프로필 수정/삭제 가능
    if (requester.getRole() == Role.ADMIN) {
      log.debug("관리자 권한으로 프로필 접근 허용: admin={}, target={}", requesterId, profileMemberId);
      return;
    }

    // 일반 사용자는 본인 프로필만
    if (!profileMemberId.equals(requesterId)) {
      log.warn("권한 없는 프로필 접근 시도: requester={}, target={}", requesterId, profileMemberId);
      throw new IllegalArgumentException("본인의 프로필만 수정/삭제할 수 있습니다.");
    }

    log.debug("프로필 접근 권한 확인 완료: user={}", requesterId);
  }

  public void validateAdminAccess(Long requesterId) {
    Member requester = memberRepository.findById(requesterId)
        .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

    if (requester.getRole() != Role.ADMIN) {
      log.warn("관리자 권한 필요 작업에 일반 사용자 접근 시도: user={}, role={}",
          requesterId, requester.getRole());
      throw new IllegalArgumentException("관리자 권한이 필요합니다.");
    }

    log.debug("관리자 권한 확인 완료: user={}", requesterId);
  }

}


