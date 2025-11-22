package com.church.attendancecounter.di

import com.church.attendancecounter.data.repository.AreaRepositoryImpl
import com.church.attendancecounter.data.repository.BranchRepositoryImpl
import com.church.attendancecounter.data.repository.ServiceRepositoryImpl
import com.church.attendancecounter.domain.repository.AreaRepository
import com.church.attendancecounter.domain.repository.BranchRepository
import com.church.attendancecounter.domain.repository.ServiceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBranchRepository(
        branchRepositoryImpl: BranchRepositoryImpl
    ): BranchRepository

    @Binds
    @Singleton
    abstract fun bindAreaRepository(
        areaRepositoryImpl: AreaRepositoryImpl
    ): AreaRepository

    @Binds
    @Singleton
    abstract fun bindServiceRepository(
        serviceRepositoryImpl: ServiceRepositoryImpl
    ): ServiceRepository
}
