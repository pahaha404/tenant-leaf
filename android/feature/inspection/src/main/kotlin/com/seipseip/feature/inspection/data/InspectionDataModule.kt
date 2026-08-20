package com.seipseip.feature.inspection.data

import com.seipseip.feature.inspection.domain.InspectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class InspectionDataModule {
    @Binds
    @Singleton
    abstract fun bindInspectionRepository(implementation: InspectionRepositoryImpl): InspectionRepository
}

