package com.dokonpro.shared.di

import com.dokonpro.shared.data.local.ZakatLocalDataSource
import com.dokonpro.shared.data.remote.ZakatApiClient
import com.dokonpro.shared.data.repository.ZakatRepositoryImpl
import com.dokonpro.shared.domain.repository.ZakatRepository
import com.dokonpro.shared.domain.usecase.CalculateZakatUseCase
import com.dokonpro.shared.domain.usecase.GetZakatHistoryUseCase
import com.dokonpro.shared.domain.usecase.SaveZakatCalculationUseCase
import org.koin.dsl.module

val zakatModule = module {
    single { ZakatLocalDataSource(get()) }
    single {
        ZakatApiClient(
            client = get(),
            baseUrl = "http://10.0.2.2:3000",
            tokenProvider = { get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken }
        )
    }
    single<ZakatRepository> { ZakatRepositoryImpl(get(), get()) }
    factory { CalculateZakatUseCase(get()) }
    factory { SaveZakatCalculationUseCase(get()) }
    factory { GetZakatHistoryUseCase(get()) }
}
