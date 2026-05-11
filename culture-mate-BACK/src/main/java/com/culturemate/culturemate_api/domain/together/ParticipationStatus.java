package com.culturemate.culturemate_api.domain.together;

public enum ParticipationStatus {
    PENDING,  // 승인 대기
    APPROVED, // 승인됨
    REJECTED, // 거절됨
    HOST,     // 호스트 (동행 생성자)
    CANCELED  // 취소됨 (참여자가 직접 취소)
}
