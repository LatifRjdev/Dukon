package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Role
import com.dokonpro.shared.domain.entity.Staff
import com.dokonpro.shared.domain.repository.StaffRepository

class AddStaffUseCase(private val repository: StaffRepository) {
    suspend operator fun invoke(storeId: String, phone: String, name: String, role: Role): Result<Staff> =
        repository.addStaff(storeId, phone, name, role)
}
