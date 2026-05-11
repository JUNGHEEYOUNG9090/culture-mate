package com.culturemate.culturemate_api.exceptions.together;

public class TogetherAlreadyJoinedException extends RuntimeException {
  public TogetherAlreadyJoinedException(String message) {
    super(message);
  }
  
  public TogetherAlreadyJoinedException(Long togetherId, Long memberId) {
    super("회원 ID " + memberId + "는 이미 모집글 ID " + togetherId + "에 참여 중입니다.");
  }
}