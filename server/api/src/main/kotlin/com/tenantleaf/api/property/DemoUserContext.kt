package com.tenantleaf.api.property

import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

/**
 * 발표용으로 요청별 소유권 범위를 제공한다.
 *
 * 실제 로그인 전에는 Android Debug APK가 [DEMO_USER_HEADER]에 허용된 데모 사용자 키를 보낸다.
 * 헤더가 없거나 허용되지 않은 값이면 기존 단일 로컬 사용자를 사용하므로, 기존 USB/에뮬레이터 개발 흐름은 유지된다.
 * 실제 배포에서는 반드시 인증 principal/JWT로 교체해야 한다.
 */
@Component
class DemoUserContext {
    fun requireUserId(): UUID {
        val demoUser = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)
            ?.request
            ?.getHeader(DEMO_USER_HEADER)
            ?.trim()
            ?.lowercase()

        return DEMO_USER_IDS[demoUser] ?: DEMO_USER_ID
    }

    companion object {
        const val DEMO_USER_HEADER = "X-Demo-User"
        val DEMO_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val DEMO_USER_IDS: Map<String, UUID> = mapOf(
            "local" to DEMO_USER_ID,
            "judge-a" to UUID.fromString("00000000-0000-0000-0000-000000000011"),
            "judge-b" to UUID.fromString("00000000-0000-0000-0000-000000000012"),
            "judge-c" to UUID.fromString("00000000-0000-0000-0000-000000000013"),
            "judge-d" to UUID.fromString("00000000-0000-0000-0000-000000000014"),
        )
    }
}
