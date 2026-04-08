package com.dokonpro.shared.di

import com.dokonpro.shared.data.local.CustomerLocalDataSource
import com.dokonpro.shared.data.remote.CustomerApiClient
import com.dokonpro.shared.data.repository.CustomerRepositoryImpl
import com.dokonpro.shared.domain.repository.CustomerRepository
import com.dokonpro.shared.domain.usecase.*
import org.koin.dsl.module

val customerModule = module {
    single { CustomerLocalDataSource(get()) }
    single { CustomerApiClient(client = get(), baseUrl = "http://10.0.2.2:3000",
        tokenProvider = { get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken }) }
    single<CustomerRepository> { CustomerRepositoryImpl(get(), get(), get()) }
    factory { GetCustomersUseCase(get()) }
    factory { CreateCustomerUseCase(get()) }
    factory { UpdateCustomerUseCase(get()) }
    factory { SearchCustomerUseCase(get()) }
    factory { GetCustomerPurchasesUseCase(get()) }
}
