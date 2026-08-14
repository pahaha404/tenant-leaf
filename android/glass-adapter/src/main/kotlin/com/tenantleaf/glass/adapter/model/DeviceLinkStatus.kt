package com.tenantleaf.glass.adapter.model

// 휴대전화와 안경 사이의 물리적 무선(BLE / Wi-Fi Direct) 링크 상태
enum class DeviceLinkStatus {
    // 안경과 무선 연결이 맺어지지 않은 상태
    DISCONNECTED,

    // 안경 검색 및 무선 핸드셰이크 진행 중
    CONNECTING,

    // 무선 링크가 정상 연결되어 대기 중인 상태
    CONNECTED,

    // 무선 연결 해제 절차 진행 중
    DISCONNECTING
}
