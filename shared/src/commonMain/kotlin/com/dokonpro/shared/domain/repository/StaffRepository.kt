package com.dokonpro.shared.domain.repository

import com.dokonpro.shared.domain.entity.Role
import com.dokonpro.shared.domain.entity.Staff
import kotlinx.coroutines.flow.Flow

interface StaffRepository {
    fun getStaff(storeId: String): Flow<List<Staff>>
    suspend fun addStaff(storeId: String, phone: String, name: String, role: Role): Result<Staff>
    suspend fun updateRole(storeId: String, staffId: String, role: Role): Result<Staff>
    suspend fun deactivate(storeId: String, staffId: String): Result<Unit>
    suspend fun syncFromRemote(storeId: String): Result<Unit>
}
