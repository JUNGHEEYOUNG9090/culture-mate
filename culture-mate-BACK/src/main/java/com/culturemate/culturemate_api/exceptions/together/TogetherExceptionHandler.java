package com.culturemate.culturemate_api.exceptions.together;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class TogetherExceptionHandler {

  @ExceptionHandler(TogetherNotFoundException.class)
  public ResponseEntity<String> handleTogetherNotFound(TogetherNotFoundException e) {
    return ResponseEntity.status(404).body(e.getMessage());
  }

  @ExceptionHandler(TogetherExpiredException.class)
  public ResponseEntity<String> handleTogetherExpired(TogetherExpiredException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }

  @ExceptionHandler(TogetherFullException.class)
  public ResponseEntity<String> handleTogetherFull(TogetherFullException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }

  @ExceptionHandler(TogetherAlreadyJoinedException.class)
  public ResponseEntity<String> handleAlreadyJoined(TogetherAlreadyJoinedException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }

  @ExceptionHandler(TogetherNotJoinedException.class)
  public ResponseEntity<String> handleNotJoined(TogetherNotJoinedException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }

  @ExceptionHandler(TogetherClosedException.class)
  public ResponseEntity<String> handleTogetherClosed(TogetherClosedException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }
}