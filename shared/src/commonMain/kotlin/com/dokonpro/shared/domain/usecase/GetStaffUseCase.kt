package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Staff
import com.dokonpro.shared.domain.repository.StaffRepository
import kotlinx.coroutines.flow.Flow

class GetStaffUseCase(private val repository: StaffRepository) {
    operator fun invoke(storeId: String): Flow<List<Staff>> = repository.getStaff(storeId)
}
