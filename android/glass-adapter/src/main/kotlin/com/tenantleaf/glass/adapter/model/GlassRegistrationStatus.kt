package com.tenantleaf.glass.adapter.model

// 앱과 Meta View(또는 글래스 관리 앱) 사이의 계정/기기 등록 상태
enum class GlassRegistrationStatus {
    // 블루투스 꺼짐 또는 필수 관리 앱 미설치로 등록 불가
    UNAVAILABLE,

    // 앱에 안경이 아직 등록되지 않은 상태
    UNREGISTERED,

    // 등록(페어링) 진행 중
    REGISTERING,

    // 등록 완료되어 기기 통신이 가능한 상태
    REGISTERED,

    // 등록 해제 진행 중
    UNREGISTERING
}
