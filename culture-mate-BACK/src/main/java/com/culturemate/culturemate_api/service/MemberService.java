package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.MemberDetail;
import com.culturemate.culturemate_api.domain.member.MemberStatus;
import com.culturemate.culturemate_api.domain.member.Role;
import com.culturemate.culturemate_api.dto.MemberDto;
import com.culturemate.culturemate_api.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final MemberDetailService memberDetailService;

  // 회원 가입
  @Transactional
  public Member register(MemberDto.Register registerDto) {
    log.info("회원가입 시도: {}", registerDto.getLoginId());

    if (memberRepository.existsByLoginId(registerDto.getLoginId())) {
      log.warn("중복 아이디로 회원가입 시도: {}", registerDto.getLoginId());
      throw new IllegalArgumentException("이미 사용 중인 로그인 아이디입니다.");
    }

    // 이메일이 있는 경우 중복 검증
    if (registerDto.getEmail() != null && !registerDto.getEmail().trim().isEmpty()) {
      if (memberDetailService.existsByEmail(registerDto.getEmail())) {
        log.warn("중복 이메일로 회원가입 시도: {} - {}", registerDto.getLoginId(), registerDto.getEmail());
        throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
      }
    }

    var hash = passwordEncoder.encode(registerDto.getPassword());

    Member member = Member.builder()
      .loginId(registerDto.getLoginId())
      .password(hash)
      .build();

    Member savedMember = memberRepository.save(member);
    memberDetailService.create(savedMember, MemberDto.DetailRequest.from(registerDto));

    log.info("회원가입 완료: {} (ID: {})", savedMember.getLoginId(), savedMember.getId());
    return savedMember;
  }

  // 회원 삭제
  @Transactional
  public void delete(Long memberId) {
    Member member = findById(memberId);
    log.warn("회원 삭제 실행: {} (ID: {})", member.getLoginId(), memberId);
    memberRepository.deleteById(memberId);
    log.info("회원 삭제 완료: ID={}", memberId);
  }

  // 전체 회원 조회
  public List<Member> findAll() {
    return memberRepository.findAll();
  }

  // ID로 회원 조회
  public Member findById(Long id) {
    return memberRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
  }

  // 로그인 아이디로 회원 조회
  public Member findByLoginId(String loginId) {
    return memberRepository.findByLoginId(loginId)
      .orElseThrow(() -> new IllegalArgumentException("해당 아이디의 회원이 없습니다."));
  }

  // 로그인 아이디 중복 확인
  public boolean existsByLoginId(String loginId) {
    return memberRepository.existsByLoginId(loginId);
  }

  // 상태별 회원 조회
  public List<Member> findByStatus(MemberStatus status) {
    return memberRepository.findByStatus(status);
  }

  // 닉네임으로 회원 검색 (부분 검색)
  public List<Member> findByNicknameContaining(String nickname) {
    return memberRepository.findByNicknameContaining(nickname);
  }

  // 회원 상태 변경
  @Transactional
  public Member updateStatus(Long memberId, MemberStatus newStatus) {
    Member member = findById(memberId);
    member.changeStatus(newStatus);
    return member;
  }

  // 비밀번호 변경
  @Transactional
  public Member updatePassword(Long memberId, String newPassword) {
    Member member = findById(memberId);
    var hash = passwordEncoder.encode(newPassword);
    member.changePassword(hash);
    return member;
  }

  // 권한 변경
  @Transactional
  public Member updateRole(Long memberId, Role newRole) {
    Member member = findById(memberId);
    member.changeRole(newRole);
    return member;
  }
}
