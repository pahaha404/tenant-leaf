package com.tenantleaf.glass.adapter.model

// 안경 스피커(TTS 음성 가이드)의 블루투스 오디오 출력 경로 상태
enum class GlassAudioRouteStatus {
    // 안경 오디오 미연결 (휴대전화 내장 스피커 사용)
    NOT_CONNECTED,

    // 블루투스 오디오 페어링됨 (기본 출력 라우트 미확정)
    AVAILABLE_NOT_CONFIRMED,

    // 안경 스피커로 음성 안내 출력 준비 완료
    RAYBAN_SPEAKER_ACTIVE
}
