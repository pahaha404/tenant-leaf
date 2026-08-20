package com.seipseip.app

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class TenantLeafApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BuildConfig.KAKAO_NATIVE_APP_KEY.takeIf { it.isNotBlank() }?.let { KakaoMapSdk.init(this, it) }
    }
}
