package com.culturemate.culturemate_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 검색 결과를 위한 페이징 정보 포함 응답 DTO
 * @param <T> 실제 데이터 타입 (EventDto.Response, TogetherDto.Response 등)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto<T> {

  /**
   * 실제 결과 데이터 목록
   */
  private List<T> content;

  /**
   * 전체 검색 결과 개수
   */
  private long totalCount;

  /**
   * 현재 페이지에서 반환된 개수
   */
  private int currentCount;

  /**
   * 요청한 limit 값
   */
  private Integer limit;

  /**
   * 요청한 offset 값
   */
  private Integer offset;

  /**
   * 다음 페이지 존재 여부
   */
  private boolean hasNext;

  /**
   * 이전 페이지 존재 여부
   */
  private boolean hasPrevious;

  /**
   * 검색 키워드 (선택사항)
   */
  private String keyword;

  /**
   * 정적 팩토리 메서드 - 전체 개수와 함께 SearchResultDto 생성
   * @param content 결과 데이터
   * @param totalCount 전체 개수
   * @param limit 요청 limit
   * @param offset 요청 offset
   * @param keyword 검색 키워드
   * @return SearchResultDto 인스턴스
   */
  public static <T> SearchResultDto<T> of(List<T> content, long totalCount, Integer limit, Integer offset, String keyword) {
    int currentCount = content != null ? content.size() : 0;
    boolean hasNext = false;
    boolean hasPrevious = false;

    if (limit != null && offset != null) {
      hasNext = (offset + limit) < totalCount;
      hasPrevious = offset > 0;
    }

    return SearchResultDto.<T>builder()
        .content(content)
        .totalCount(totalCount)
        .currentCount(currentCount)
        .limit(limit)
        .offset(offset)
        .hasNext(hasNext)
        .hasPrevious(hasPrevious)
        .keyword(keyword)
        .build();
  }

  /**
   * 키워드 없이 SearchResultDto 생성
   */
  public static <T> SearchResultDto<T> of(List<T> content, long totalCount, Integer limit, Integer offset) {
    return of(content, totalCount, limit, offset, null);
  }
}