package com.dokonpro.shared.di

import com.dokonpro.shared.data.local.SaleLocalDataSource
import com.dokonpro.shared.data.remote.SaleApiClient
import com.dokonpro.shared.data.repository.SaleRepositoryImpl
import com.dokonpro.shared.domain.repository.SaleRepository
import com.dokonpro.shared.domain.usecase.*
import org.koin.dsl.module

val salesModule = module {
    single { SaleLocalDataSource(get()) }
    single {
        SaleApiClient(client = get(), baseUrl = "http://10.0.2.2:3000",
            tokenProvider = { get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken })
    }
    single<SaleRepository> { SaleRepositoryImpl(get(), get(), get()) }
    factory { AddToCartUseCase() }
    factory { RemoveFromCartUseCase() }
    factory { UpdateCartItemUseCase() }
    factory { CompleteSaleUseCase(get()) }
    factory { GetSalesHistoryUseCase(get()) }
}
