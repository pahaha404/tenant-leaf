package com.seipseip.core.network

import com.seipseip.core.BuildConfig
import com.seipseip.core.network.generated.api.InspectionsApi
import com.seipseip.core.network.generated.api.MediaApi
import com.seipseip.core.network.generated.api.PropertiesApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideAuthTokenProvider(): AuthTokenProvider = NoAuthTokenProvider()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(UuidJsonAdapter())
        .add(OffsetDateTimeJsonAdapter())
        .add(UriJsonAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun providePropertiesApi(retrofit: Retrofit): PropertiesApi = retrofit.create(PropertiesApi::class.java)

    @Provides
    @Singleton
    fun provideInspectionsApi(retrofit: Retrofit): InspectionsApi = retrofit.create(InspectionsApi::class.java)

    @Provides
    @Singleton
    fun provideMediaApi(retrofit: Retrofit): MediaApi = retrofit.create(MediaApi::class.java)
}

