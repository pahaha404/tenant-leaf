package com.tenantleaf.glass.adapter

import com.tenantleaf.glass.adapter.model.GlassError

/**
 * UI 계층에 일회성으로 전달되는 글래스 제어/상태 이벤트.
 * 상태(GlassState)로 관리하기 부적합한 토스트 알림, 화면 전환 인텐트 실행,
 * 권한 요청 팝업 등을 트리거할 때 사용합니다.
 */
sealed interface GlassConnectionEvent {
    // 필수 안드로이드 런타임 권한 요청이 필요한 경우
    data class RequirePermissions(val permissions: List<String>) : GlassConnectionEvent

    // 제조사 관리 앱(Meta View)의 기기 등록 화면 실행 요청
    data object LaunchRegistrationFlow : GlassConnectionEvent

    // 기기 등록 해제 화면 실행 요청
    data object LaunchUnregistrationFlow : GlassConnectionEvent

    // 펌웨어 업데이트 화면 실행 요청
    data class LaunchFirmwareUpdate(val deviceName: String) : GlassConnectionEvent

    // 글래스 기기 연결 완료 알림
    data class Connected(val deviceName: String) : GlassConnectionEvent

    // 글래스 기기 연결 해제 알림
    data object Disconnected : GlassConnectionEvent

    // 사용자에게 다이얼로그나 스낵바로 알려야 하는 오류 발생
    data class ErrorOccurred(val error: GlassError) : GlassConnectionEvent
}
