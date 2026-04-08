package com.dokonpro.shared.di

import com.dokonpro.shared.data.local.DatabaseDriverFactory
import com.dokonpro.shared.data.local.ProductLocalDataSource
import com.dokonpro.shared.data.sync.SyncManager
import com.dokonpro.shared.data.sync.SyncQueue
import com.dokonpro.shared.data.sync.SyncStatus
import com.dokonpro.shared.db.DokonProDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { get<DatabaseDriverFactory>().createDriver() }
    single { DokonProDatabase(get()) }
    single { ProductLocalDataSource(get()) }
    single { SyncQueue(get()) }
    single { SyncStatus() }
    single {
        SyncManager(
            queue = get(),
            client = get(),
            baseUrl = "http://10.0.2.2:3000",
            syncStatus = get(),
            tokenProvider = {
                get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken
            }
        )
    }
}
