package com.tenantleaf.glass.adapter.model

// UI 레이어가 에러 발생 시 사용자에게 유도할 복구 액션
enum class ErrorRecoveryAction {
    OPEN_BLUETOOTH_SETTINGS, // 시스템 블루투스 설정 화면으로 이동 유도
    REQUEST_PERMISSIONS,     // 안드로이드 런타임 권한 허용 팝업 유도
    OPEN_META_VIEW,          // Meta View 전용 관리 앱으로 이동 유도 (펌웨어 업데이트 등)
    RETRY,                   // 단순 재시도 버튼 제공
    DISMISS,                 // 단순 확인 및 닫기
}

// AI 글래스 연동 및 통신 과정에서 발생하는 도메인 오류 분류
sealed class GlassError(
    val code: String,
    val userMessage: String,
    val recoveryAction: ErrorRecoveryAction = ErrorRecoveryAction.DISMISS,
) {
    // 블루투스가 꺼져 있음
    data object BluetoothDisabled : GlassError(
        code = "BLUETOOTH_DISABLED",
        userMessage = "블루투스가 꺼져 있습니다. 블루투스를 켜주세요.",
        recoveryAction = ErrorRecoveryAction.OPEN_BLUETOOTH_SETTINGS,
    )

    // 필수 권한이 거부됨
    data class PermissionDenied(val missingPermissions: List<String>) : GlassError(
        code = "PERMISSION_DENIED",
        userMessage = "안경 연결 및 카메라 사용을 위한 필수 권한이 필요합니다.",
        recoveryAction = ErrorRecoveryAction.REQUEST_PERMISSIONS,
    )

    // 페어링된 글래스를 찾을 수 없음
    data object DeviceNotFound : GlassError(
        code = "DEVICE_NOT_FOUND",
        userMessage = "등록된 Meta 안경을 찾을 수 없습니다. 안경 전원이 켜져 있는지 확인해 주세요.",
        recoveryAction = ErrorRecoveryAction.RETRY,
    )

    // 안경 기기의 펌웨어 업데이트가 필수적임
    data class FirmwareUpdateRequired(val deviceName: String) : GlassError(
        code = "FIRMWARE_UPDATE_REQUIRED",
        userMessage = "[$deviceName] 안경의 펌웨어 업데이트가 필요합니다. Meta View 앱에서 업데이트를 진행해 주세요.",
        recoveryAction = ErrorRecoveryAction.OPEN_META_VIEW,
    )

    // 안경 무선 핸드셰이크 연결 시간 초과
    data object ConnectionTimeout : GlassError(
        code = "CONNECTION_TIMEOUT",
        userMessage = "안경과의 연결 시간이 초과되었습니다. 다시 시도해 주세요.",
        recoveryAction = ErrorRecoveryAction.RETRY,
    )

    // 카메라/센서 데이터 세션 파이프라인 생성 실패
    data class SessionStartFailed(val reason: String) : GlassError(
        code = "SESSION_START_FAILED",
        userMessage = "카메라 세션을 시작하지 못했습니다: $reason",
        recoveryAction = ErrorRecoveryAction.RETRY,
    )

    // 기타 처리되지 않은 예외 및 알 수 없는 오류
    data class Unknown(val detail: String) : GlassError(
        code = "UNKNOWN_ERROR",
        userMessage = detail,
        recoveryAction = ErrorRecoveryAction.DISMISS,
    )
}
