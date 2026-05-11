package com.culturemate.culturemate_api.domain.chatting;

/**
 * 채팅방 타입 구분
 */
public enum ChatRoomType {
  GROUP_CHAT,        // 그룹 채팅방 (동행 승인된 참여자들)
  APPLICATION_CHAT   // 신청용 1:1 채팅방 (호스트-신청자)
}