package com.culturemate.culturemate_api.exceptions.together;

public class TogetherNotFoundException extends RuntimeException {
  public TogetherNotFoundException(String message) {
    super(message);
  }
  
  public TogetherNotFoundException(Long togetherId) {
    super("ID " + togetherId + "에 해당하는 모집글을 찾을 수 없습니다.");
  }
}