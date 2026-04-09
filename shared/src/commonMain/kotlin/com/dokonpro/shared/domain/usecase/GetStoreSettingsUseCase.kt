package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.StoreSettings
import com.dokonpro.shared.domain.repository.StoreSettingsRepository

class GetStoreSettingsUseCase(private val repository: StoreSettingsRepository) {
    suspend operator fun invoke(storeId: String): Result<StoreSettings> = repository.getSettings(storeId)
}
