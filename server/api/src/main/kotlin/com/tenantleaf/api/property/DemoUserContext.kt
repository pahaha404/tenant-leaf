package com.tenantleaf.api.property

import org.springframework.stereotype.Component
import java.util.UUID

/** MVP 데모 로그인 사용자의 소유권 범위를 제공한다. 실제 인증 도입 시 인증 principal로 교체한다. */
@Component
class DemoUserContext {
    fun requireUserId(): UUID = DEMO_USER_ID

    companion object {
        val DEMO_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }
}
