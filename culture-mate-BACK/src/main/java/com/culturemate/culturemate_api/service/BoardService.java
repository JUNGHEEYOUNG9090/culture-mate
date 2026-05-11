package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.community.Board;
import com.culturemate.culturemate_api.domain.community.BoardLike;
import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.event.EventType;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.dto.BoardDto;
import com.culturemate.culturemate_api.dto.BoardSearchDto;
import com.culturemate.culturemate_api.repository.BoardLikeRepository;
import com.culturemate.culturemate_api.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
  private final BoardRepository boardRepository;
  private final BoardLikeRepository boardLikeRepository;
  private final MemberService memberService;
  private final EventService eventService;
  private final ValidationService validationService;

  public List<Board> findAll() {
    return boardRepository.findAll();
  }

  public Board findById(Long boardId) {
    return boardRepository.findById(boardId)
      .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
  }

  // 작성자로 조회
  public List<Board> findByAuthor(Long memberId) {
    Member author = memberService.findById(memberId);  // MemberService에서 조회
    return boardRepository.findByAuthor(author);
  }

  // 통합 검색
  public List<Board> search(BoardSearchDto searchDto) {
    EventType eventType = null;
    if (searchDto.hasEventType()) {
      eventType = EventType.valueOf(searchDto.getEventType().toUpperCase());
    }

    Event event = null;
    if (searchDto.hasEventId()) {
      event = eventService.findById(searchDto.getEventId());
    }

    return boardRepository.findBySearch(
      searchDto.hasKeyword() ? searchDto.getKeyword() : null,
      searchDto.hasAuthor() ? searchDto.getAuthorNickname() : null,
      event,
      eventType
    );
  }

  // 게시글 생성
  @Transactional
  public Board create(BoardDto.Request requestDto) {
    Member author = memberService.findById(requestDto.getAuthorId());  // MemberService에서 조회
    
    Event event = null;
    if (requestDto.getEventId() != null) {
      event = eventService.findById(requestDto.getEventId());
    }
    
    Board board = Board.builder()
      .title(requestDto.getTitle())
      .content(requestDto.getContent())
      .author(author)
      .eventType(requestDto.getEventType())
      .event(event)
      .build();
    return boardRepository.save(board);
  }

  // 게시글 수정
  @Transactional
  public Board update(Long boardId, BoardDto.Request requestDto, Long requesterId) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
    
    // 권한 검증: 본인의 게시글만 수정 가능
    validationService.validateBoardAccess(board, requesterId);

    Event event = null;
    if (requestDto.getEventId() != null) {
      event = eventService.findById(requestDto.getEventId());
    }
    
    board.setTitle(requestDto.getTitle());
    board.setContent(requestDto.getContent());
    // eventType과 event는 setter가 없으므로 업데이트 불가 (필요시 setter 추가 필요)

    return board;
  }

  // 게시글 삭제
  @Transactional
  public void delete(Long boardId, Long requesterId) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
    
    // 권한 검증: 본인의 게시글만 삭제 가능
    validationService.validateBoardAccess(board, requesterId);
    
    boardRepository.delete(board);
  }

  // 좋아요
  @Transactional
  public boolean toggleBoardLike(Long boardId, Long memberId) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
    Member member = memberService.findById(memberId);  // MemberService에서 조회

    Optional<BoardLike> existing = boardLikeRepository.findByBoardAndMember(board, member);

    if (existing.isPresent()) {
      // 이미 좋아요 눌렀으면 취소
      boardLikeRepository.delete(existing.get());
      boardRepository.updateLikeCount(boardId, -1); // 원자적 감소
      return false; // 취소됨
    } else {
      // 좋아요 추가
      BoardLike boardLike = BoardLike.builder()
        .board(board)
        .member(member)
        .build();
      boardLikeRepository.save(boardLike);
      boardRepository.updateLikeCount(boardId, 1); // 원자적 증가
      return true; // 좋아요 성공
    }
  }


}
