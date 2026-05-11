package com.culturemate.culturemate_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 검색 결과와 전체 개수를 함께 담는 래퍼 클래스
 * @param <T> 검색 결과 데이터 타입 (Event, Together 등)
 */
@Getter
@AllArgsConstructor
public class SearchResult<T> {

  /**
   * 검색 결과 데이터 목록 (페이지네이션 적용된 실제 결과)
   */
  private final List<T> content;

  /**
   * 검색 조건에 해당하는 전체 개수 (페이지네이션 적용 전)
   */
  private final long totalCount;

  /**
   * 현재 페이지 결과 개수
   */
  public int getCurrentCount() {
    return content != null ? content.size() : 0;
  }

  /**
   * 빈 결과 생성
   */
  public static <T> SearchResult<T> empty() {
    return new SearchResult<>(List.of(), 0L);
  }
}