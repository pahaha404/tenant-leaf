package com.tenantleaf.glass.adapter.model

/**
 * AI 글래스의 현재 종합 상태를 나타내는 단일 불변(Immutable) 상태 모델.
 * 독립적인 상태 머신(등록, 물리 링크, 스트림, 오디오 라우트)을 직교하게 분리하여
 * UI/ViewModel에서 StateFlow로 안전하게 구독할 수 있습니다.
 */
data class GlassState(
    val registration: GlassRegistrationStatus = GlassRegistrationStatus.UNAVAILABLE, // 앱과 관리 앱 사이의 페어링 상태
    val link: DeviceLinkStatus = DeviceLinkStatus.DISCONNECTED,                      // 안경과의 물리 무선 링크 상태
    val stream: GlassStreamStatus = GlassStreamStatus.STOPPED,                       // 카메라 및 센서 데이터 스트리밍 상태
    val audioRoute: GlassAudioRouteStatus = GlassAudioRouteStatus.NOT_CONNECTED,     // 안경 스피커(TTS) 오디오 출력 경로 상태
    val device: GlassDeviceInfo? = null,                                             // 연결된 기기 하드웨어 메타데이터
    val unhandledError: GlassError? = null,                                          // UI 처리 대기 중인 최근 오류
) {
    // 앱에 안경이 정상 등록되어 통신 가능한 상태인지 여부
    val isRegistered: Boolean
        get() = registration == GlassRegistrationStatus.REGISTERED

    // 안경과 물리적 무선 링크가 안정적으로 연결되어 있는지 여부
    val isConnected: Boolean
        get() = link == DeviceLinkStatus.CONNECTED

    // 현재 안경 카메라로부터 실시간 영상 스트림을 수신 중인지 여부
    val isStreaming: Boolean
        get() = stream == GlassStreamStatus.STREAMING

    // 현장점검(촬영, 스트리밍 시작)을 즉시 수행할 수 있는 완전 준비 상태인지 여부
    val isReadyForInspection: Boolean
        get() = isRegistered && isConnected && stream != GlassStreamStatus.STARTING

    // 연결 시도, 등록 중, 스트림 시작/종료 등 비동기 상태 전이가 진행 중인지 여부 (UI 로딩 표시용)
    val isBusy: Boolean
        get() = registration == GlassRegistrationStatus.REGISTERING ||
            registration == GlassRegistrationStatus.UNREGISTERING ||
            link == DeviceLinkStatus.CONNECTING ||
            link == DeviceLinkStatus.DISCONNECTING ||
            stream == GlassStreamStatus.STARTING ||
            stream == GlassStreamStatus.STOPPING

    // 안경 스피커로 TTS 음성 안내가 바로 출력 가능한 상태인지 여부
    val canPlayVoiceGuide: Boolean
        get() = audioRoute == GlassAudioRouteStatus.RAYBAN_SPEAKER_ACTIVE
}
