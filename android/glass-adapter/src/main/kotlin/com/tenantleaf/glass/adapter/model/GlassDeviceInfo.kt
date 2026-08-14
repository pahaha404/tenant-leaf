package com.tenantleaf.glass.adapter.model

// 연결된 글래스 기기의 하드웨어 및 식별 메타데이터
data class GlassDeviceInfo(
    val deviceId: String,                       // 기기 고유 식별자
    val deviceName: String,                     // 사용자 표시용 기기 모델명
    val batteryLevel: Int? = null,              // 배터리 잔량 백분율 (0..100), 미수신 시 null
    val isCharging: Boolean = false,            // 충전 중 여부
    val isFirmwareUpdateRequired: Boolean = false, // 펌웨어 업데이트 필요 여부
) {
    init {
        if (batteryLevel != null) {
            require(batteryLevel in 0..100) {
                "배터리 잔량은 0에서 100 사이의 값이어야 합니다: $batteryLevel"
            }
        }
    }

    // 배터리 부족 상태 (20% 이하)
    val isLowBattery: Boolean
        get() = (batteryLevel ?: 100) <= 20
}
