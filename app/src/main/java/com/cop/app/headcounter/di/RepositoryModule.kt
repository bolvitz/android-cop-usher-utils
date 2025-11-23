package com.cop.app.headcounter.di

import com.cop.app.headcounter.data.repository.AreaCountRepositoryImpl
import com.cop.app.headcounter.data.repository.AreaRepositoryImpl
import com.cop.app.headcounter.data.repository.BranchRepositoryImpl
import com.cop.app.headcounter.data.repository.ServiceRepositoryImpl
import com.cop.app.headcounter.data.repository.ServiceTypeRepositoryImpl
import com.cop.app.headcounter.domain.repository.AreaCountRepository
import com.cop.app.headcounter.domain.repository.AreaRepository
import com.cop.app.headcounter.domain.repository.BranchRepository
import com.cop.app.headcounter.domain.repository.ServiceRepository
import com.cop.app.headcounter.domain.repository.ServiceTypeRepository
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

    @Binds
    @Singleton
    abstract fun bindServiceTypeRepository(
        serviceTypeRepositoryImpl: ServiceTypeRepositoryImpl
    ): ServiceTypeRepository

    @Binds
    @Singleton
    abstract fun bindAreaCountRepository(
        areaCountRepositoryImpl: AreaCountRepositoryImpl
    ): AreaCountRepository
}
