package com.culturemate.culturemate_api.domain;

public enum ImageTarget {
  TOGETHER("together"),
  EVENT("event"),
  EVENT_CONTENT("event/content"),
  BOARD_CONTENT("board"),
  CHAT_MESSAGE("chat/message"),
  MEMBER_PROFILE("member/profile"),
  MEMBER_BACKGROUND("member/background"),
  MEMBER_GALLERY("member/gallery"),
  INQUIRY("inquiry");

  private final String path;

  ImageTarget(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }
}
