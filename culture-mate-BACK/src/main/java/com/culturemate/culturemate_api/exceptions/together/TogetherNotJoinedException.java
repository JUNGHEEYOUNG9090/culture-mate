package com.culturemate.culturemate_api.exceptions.together;

public class TogetherNotJoinedException extends RuntimeException {
  public TogetherNotJoinedException(String message) {
    super(message);
  }
  
  public TogetherNotJoinedException(Long togetherId, Long memberId) {
    super("회원 ID " + memberId + "는 모집글 ID " + togetherId + "에 참여하지 않았습니다.");
  }
}