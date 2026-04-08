package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Role
import com.dokonpro.shared.domain.entity.Staff
import com.dokonpro.shared.domain.repository.StaffRepository

class UpdateStaffRoleUseCase(private val repository: StaffRepository) {
    suspend operator fun invoke(storeId: String, staffId: String, role: Role): Result<Staff> =
        repository.updateRole(storeId, staffId, role)
}
