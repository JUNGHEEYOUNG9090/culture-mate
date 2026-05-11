package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.chatting.ChatRoom;
import com.culturemate.culturemate_api.domain.chatting.ChatRoomType;
import com.culturemate.culturemate_api.domain.together.Together;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  // 기존 메서드는 여러 개 반환 가능성으로 인해 유지하되, 새로운 메서드들 추가
  Optional<ChatRoom> findByTogether(Together together);

  void deleteByTogetherId(Long togetherId);

  // 타입별 조회 메서드들
  List<ChatRoom> findByTogetherAndType(Together together, ChatRoomType type);

  Optional<ChatRoom> findByTogetherAndTypeAndApplicantIsNull(Together together, ChatRoomType type);

  // 편의 메서드들
  default Optional<ChatRoom> findGroupChatByTogether(Together together) {
    return findByTogetherAndTypeAndApplicantIsNull(together, ChatRoomType.GROUP_CHAT);
  }

  default List<ChatRoom> findApplicationChatsByTogether(Together together) {
    return findByTogetherAndType(together, ChatRoomType.APPLICATION_CHAT);
  }
}