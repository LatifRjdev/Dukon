package com.dokonpro.shared.di

import com.dokonpro.shared.data.local.SessionProvider
import com.dokonpro.shared.data.remote.AuthApiClient
import com.dokonpro.shared.data.remote.createHttpClient
import com.dokonpro.shared.data.repository.AuthRepositoryImpl
import com.dokonpro.shared.domain.repository.AuthRepository
import com.dokonpro.shared.domain.usecase.*
import org.koin.dsl.module

val authModule = module {
    single { createHttpClient() }
    single { AuthApiClient(get(), "http://10.0.2.2:3000") }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single { SessionProvider(get()) }
    factory { SendOtpUseCase(get()) }
    factory { VerifyOtpUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { RefreshTokenUseCase(get()) }
    factory { LogoutUseCase(get()) }
}
