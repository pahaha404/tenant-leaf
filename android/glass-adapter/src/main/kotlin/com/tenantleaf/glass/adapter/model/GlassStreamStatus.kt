package com.tenantleaf.glass.adapter.model

// 안경 카메라 및 마이크 센서의 실시간 미디어 스트리밍 상태
enum class GlassStreamStatus {
    // 카메라 스트림이 중지된 상태
    STOPPED,

    // 카메라 세션 파이프라인 생성 및 스트림 시작 중
    STARTING,

    // 실시간 비디오 및 오디오 프레임 정상 수신 중
    STREAMING,

    // 안경 탭 터치 또는 앱 요청으로 일시 정지된 상태
    PAUSED,

    // 스트림 종료 진행 중
    STOPPING
}
