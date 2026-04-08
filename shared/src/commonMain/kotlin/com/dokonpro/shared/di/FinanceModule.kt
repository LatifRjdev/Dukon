package com.dokonpro.shared.di

import com.dokonpro.shared.data.local.TransactionLocalDataSource
import com.dokonpro.shared.data.remote.FinanceApiClient
import com.dokonpro.shared.data.repository.FinanceRepositoryImpl
import com.dokonpro.shared.domain.repository.FinanceRepository
import com.dokonpro.shared.domain.usecase.*
import org.koin.dsl.module

val financeModule = module {
    single { TransactionLocalDataSource(get()) }
    single {
        FinanceApiClient(client = get(), baseUrl = "http://10.0.2.2:3000",
            tokenProvider = { get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken })
    }
    single<FinanceRepository> { FinanceRepositoryImpl(get(), get(), get()) }
    factory { GetFinanceSummaryUseCase(get()) }
    factory { GetTransactionsUseCase(get()) }
    factory { AddExpenseUseCase(get()) }
    factory { GetReportUseCase(get()) }
}
