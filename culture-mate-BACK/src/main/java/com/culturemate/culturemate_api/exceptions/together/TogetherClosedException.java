package com.culturemate.culturemate_api.exceptions.together;

public class TogetherClosedException extends RuntimeException {
  public TogetherClosedException(String message) {
    super(message);
  }
  
  public TogetherClosedException(Long togetherId) {
    super("ID " + togetherId + " 모집글은 마감되었습니다.");
  }
}