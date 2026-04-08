package com.dokonpro.android

import android.app.Application
import com.dokonpro.shared.data.local.TokenStorage
import com.dokonpro.shared.di.sharedModule
import com.dokonpro.android.viewmodel.AuthViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class DokonProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@DokonProApp)
            modules(
                sharedModule,
                module {
                    single { TokenStorage(get()) }
                    viewModel { AuthViewModel(get(), get(), get()) }
                }
            )
        }
    }
}
