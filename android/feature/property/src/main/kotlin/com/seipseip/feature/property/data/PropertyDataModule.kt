package com.seipseip.feature.property.data

import com.seipseip.feature.property.domain.PropertyRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PropertyBindingModule {
    @Binds
    @Singleton
    abstract fun bindPropertyRepository(implementation: PropertyRepositoryImpl): PropertyRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object PropertyNetworkModule {
    @Provides
    @Singleton
    fun providePropertyPatchApi(retrofit: Retrofit): PropertyPatchApi =
        retrofit.create(PropertyPatchApi::class.java)
}

