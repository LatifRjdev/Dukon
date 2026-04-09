package com.dokonpro.shared.data.repository

import com.dokonpro.shared.data.local.StoreSettingsLocalDataSource
import com.dokonpro.shared.data.remote.StoreSettingsApiClient
import com.dokonpro.shared.data.remote.dto.UpdateStoreSettingsRequest
import com.dokonpro.shared.domain.entity.StoreSettings
import com.dokonpro.shared.domain.repository.StoreSettingsRepository

class StoreSettingsRepositoryImpl(
    private val local: StoreSettingsLocalDataSource,
    private val api: StoreSettingsApiClient
) : StoreSettingsRepository {

    override suspend fun getSettings(storeId: String): Result<StoreSettings> = runCatching {
        val dto = api.getSettings(storeId)
        val settings = StoreSettings(
            id = dto.id,
            name = dto.name,
            address = dto.address,
            phone = dto.phone,
            currency = dto.currency,
            logoUrl = dto.logoUrl,
            receiptHeader = dto.receiptHeader,
            receiptFooter = dto.receiptFooter,
            storeId = storeId
        )
        local.saveSettings(settings)
        settings
    }.recoverCatching {
        local.getSettings(storeId) ?: throw it
    }

    override suspend fun updateSettings(storeId: String, settings: StoreSettings): Result<StoreSettings> = runCatching {
        val request = UpdateStoreSettingsRequest(
            name = settings.name,
            address = settings.address,
            phone = settings.phone,
            currency = settings.currency,
            logoUrl = settings.logoUrl,
            receiptHeader = settings.receiptHeader,
            receiptFooter = settings.receiptFooter
        )
        val dto = api.updateSettings(storeId, request)
        val updated = StoreSettings(
            id = dto.id,
            name = dto.name,
            address = dto.address,
            phone = dto.phone,
            currency = dto.currency,
            logoUrl = dto.logoUrl,
            receiptHeader = dto.receiptHeader,
            receiptFooter = dto.receiptFooter,
            storeId = storeId
        )
        local.saveSettings(updated)
        updated
    }
}
