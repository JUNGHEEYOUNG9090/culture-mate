package com.culturemate.culturemate_api.exceptions.together;

public class TogetherFullException extends RuntimeException {
  public TogetherFullException(String message) {
    super(message);
  }
  
  public TogetherFullException(Long togetherId, int maxParticipants) {
    super("ID " + togetherId + " 모집글은 정원 " + maxParticipants + "명이 모두 찼습니다.");
  }
}