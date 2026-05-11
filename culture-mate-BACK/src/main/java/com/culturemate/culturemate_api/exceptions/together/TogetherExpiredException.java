package com.culturemate.culturemate_api.exceptions.together;

import java.time.LocalDate;

public class TogetherExpiredException extends RuntimeException {
  public TogetherExpiredException(String message) {
    super(message);
  }
  
  public TogetherExpiredException(Long togetherId, LocalDate meetingDate) {
    super("ID " + togetherId + " 모집글은 " + meetingDate + "에 종료되었습니다.");
  }
}