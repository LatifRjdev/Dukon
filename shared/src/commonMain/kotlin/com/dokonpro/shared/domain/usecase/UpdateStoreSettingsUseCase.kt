package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.StoreSettings
import com.dokonpro.shared.domain.repository.StoreSettingsRepository

class UpdateStoreSettingsUseCase(private val repository: StoreSettingsRepository) {
    suspend operator fun invoke(storeId: String, settings: StoreSettings): Result<StoreSettings> =
        repository.updateSettings(storeId, settings)
}
