package com.dokonpro.shared.di

import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule: Module = module {
    includes(authModule, databaseModule, productModule, salesModule, customerModule, financeModule, staffModule, zakatModule)
}
