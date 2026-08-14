package com.tenantleaf.glass.adapter

import com.tenantleaf.glass.adapter.model.GlassState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * AI 글래스 연결 및 생명주기 관리를 위한 클라이언트 공통 인터페이스.
 * Android Framework(Activity, Context)에 직접 의존하지 않아
 * UI/ViewModel 계층과 글래스 하드웨어 어댑터를 깔끔하게 분리합니다.
 */
interface GlassConnectionApi {
    // 실시간 글래스 종합 상태 스트림 (UI에서 collectAsStateWithLifecycle로 관찰)
    val state: StateFlow<GlassState>

    // 화면 이동, 권한 요청, 에러 알림 등 일회성 이벤트 스트림 (버퍼 채널 기반)
    val events: Flow<GlassConnectionEvent>

    // 글래스 기기 등록(Pairing) 플로우 시작 요청 (LaunchRegistrationFlow 이벤트 방출)
    suspend fun requestRegistration()

    // 글래스 기기 등록 해제(Unregistration) 플로우 시작 요청
    suspend fun requestUnregistration()

    // 등록된 글래스와 무선(BLE/Wi-Fi) 링크 연결 및 데이터 세션 시작
    suspend fun connect(): Result<Unit>

    // 글래스 연결 해제 및 모든 활성 세션/스트림 안전 종료
    suspend fun disconnect()

    // UI에서 확인 완료된 최근 오류(unhandledError) 초기화
    fun clearError()
}
