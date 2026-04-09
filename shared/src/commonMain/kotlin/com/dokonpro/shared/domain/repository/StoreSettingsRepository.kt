package com.dokonpro.shared.domain.repository

import com.dokonpro.shared.domain.entity.StoreSettings

interface StoreSettingsRepository {
    suspend fun getSettings(storeId: String): Result<StoreSettings>
    suspend fun updateSettings(storeId: String, settings: StoreSettings): Result<StoreSettings>
}
