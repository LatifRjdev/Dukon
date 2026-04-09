package com.dokonpro.shared.di

import com.dokonpro.shared.data.local.StoreSettingsLocalDataSource
import com.dokonpro.shared.data.remote.StoreSettingsApiClient
import com.dokonpro.shared.data.repository.StoreSettingsRepositoryImpl
import com.dokonpro.shared.domain.repository.StoreSettingsRepository
import com.dokonpro.shared.domain.usecase.GetStoreSettingsUseCase
import com.dokonpro.shared.domain.usecase.UpdateStoreSettingsUseCase
import org.koin.dsl.module

val settingsModule = module {
    single { StoreSettingsLocalDataSource(get()) }
    single {
        StoreSettingsApiClient(
            client = get(),
            baseUrl = "http://10.0.2.2:3000",
            tokenProvider = { get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken }
        )
    }
    single<StoreSettingsRepository> { StoreSettingsRepositoryImpl(get(), get()) }
    factory { GetStoreSettingsUseCase(get()) }
    factory { UpdateStoreSettingsUseCase(get()) }
}
