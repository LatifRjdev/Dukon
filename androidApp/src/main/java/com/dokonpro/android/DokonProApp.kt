package com.dokonpro.android

import android.app.Application
import com.dokonpro.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class DokonProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@DokonProApp)
            modules(sharedModule)
        }
    }
}
