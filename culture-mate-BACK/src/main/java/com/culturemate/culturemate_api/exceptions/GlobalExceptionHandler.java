package com.culturemate.culturemate_api.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  // JSON 파싱 오류 - 400 Bad Request
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<String> handleJsonParseError(HttpMessageNotReadableException e) {
    log.warn("JSON 파싱 오류: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("요청 형식이 올바르지 않습니다.");
  }

  // 유효성 검사 오류 - 400 Bad Request
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<String> handleValidationError(MethodArgumentNotValidException e) {
    log.warn("유효성 검사 실패: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("입력값이 올바르지 않습니다.");
  }

  // 파라미터 타입 오류 - 400 Bad Request
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    log.warn("파라미터 타입 오류: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("요청 파라미터가 올바르지 않습니다.");
  }

  // IllegalArgumentException - 상황에 따라 400 또는 404
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
    String message = e.getMessage();
    log.warn("IllegalArgumentException: {}", message);

    if (message != null && (message.contains("찾을 수 없") || message.contains("존재하지 않"))) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
  }

  // 일반적인 서버 오류 - 500 Internal Server Error
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
    log.error("서버 내부 오류", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("서버에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
  }
}