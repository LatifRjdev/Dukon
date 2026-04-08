package com.dokonpro.shared.di

import com.dokonpro.shared.data.remote.ProductApiClient
import com.dokonpro.shared.data.repository.ProductRepositoryImpl
import com.dokonpro.shared.domain.repository.ProductRepository
import com.dokonpro.shared.domain.usecase.*
import org.koin.dsl.module

val productModule = module {
    single {
        ProductApiClient(
            client = get(),
            baseUrl = "http://10.0.2.2:3000",
            tokenProvider = {
                get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken
            }
        )
    }
    single<ProductRepository> { ProductRepositoryImpl(get(), get(), get()) }
    factory { GetProductsUseCase(get()) }
    factory { CreateProductUseCase(get()) }
    factory { UpdateProductUseCase(get()) }
    factory { DeleteProductUseCase(get()) }
    factory { SearchProductUseCase(get()) }
}
