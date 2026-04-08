package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.repository.StaffRepository

class DeactivateStaffUseCase(private val repository: StaffRepository) {
    suspend operator fun invoke(storeId: String, staffId: String): Result<Unit> =
        repository.deactivate(storeId, staffId)
}
