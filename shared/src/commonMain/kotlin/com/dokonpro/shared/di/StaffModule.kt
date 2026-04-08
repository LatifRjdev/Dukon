package com.dokonpro.shared.di

import com.dokonpro.shared.data.local.PermissionManager
import com.dokonpro.shared.data.local.StaffLocalDataSource
import com.dokonpro.shared.data.remote.StaffApiClient
import com.dokonpro.shared.data.repository.StaffRepositoryImpl
import com.dokonpro.shared.domain.repository.StaffRepository
import com.dokonpro.shared.domain.usecase.*
import org.koin.dsl.module

val staffModule = module {
    single { StaffLocalDataSource(get()) }
    single {
        StaffApiClient(client = get(), baseUrl = "http://10.0.2.2:3000",
            tokenProvider = { get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken })
    }
    single<StaffRepository> { StaffRepositoryImpl(get(), get()) }
    single { PermissionManager() }
    factory { GetStaffUseCase(get()) }
    factory { AddStaffUseCase(get()) }
    factory { UpdateStaffRoleUseCase(get()) }
    factory { DeactivateStaffUseCase(get()) }
}
