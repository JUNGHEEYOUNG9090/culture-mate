package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.chatting.ChatMember;
import com.culturemate.culturemate_api.domain.chatting.ChatRoom;
import com.culturemate.culturemate_api.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

  Optional<ChatMember> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
  
  void deleteByChatRoomAndMember(ChatRoom chatRoom, Member member);
  
  List<ChatMember> findByMember(Member member);
}
