package com.culturemate.culturemate_api.domain.member;

public enum MemberStatus {
  ACTIVE,       // 정상
  DORMANT,      // 휴면
  SUSPENDED,    // 일시 정지
  BANNED,       // 영구 정지
  DELETED       // 탈퇴
}
