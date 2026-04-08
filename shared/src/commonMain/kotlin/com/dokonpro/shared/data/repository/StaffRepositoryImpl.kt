package com.dokonpro.shared.data.repository

import com.dokonpro.shared.data.local.StaffLocalDataSource
import com.dokonpro.shared.data.remote.StaffApiClient
import com.dokonpro.shared.data.remote.dto.AddStaffRequest
import com.dokonpro.shared.data.remote.dto.UpdateStaffRequest
import com.dokonpro.shared.domain.entity.Role
import com.dokonpro.shared.domain.entity.Staff
import com.dokonpro.shared.domain.repository.StaffRepository
import kotlinx.coroutines.flow.Flow

class StaffRepositoryImpl(
    private val local: StaffLocalDataSource,
    private val api: StaffApiClient
) : StaffRepository {

    override fun getStaff(storeId: String): Flow<List<Staff>> = local.getStaff(storeId)

    override suspend fun addStaff(storeId: String, phone: String, name: String, role: Role): Result<Staff> = runCatching {
        val dto = api.addStaff(storeId, AddStaffRequest(phone, name, role.name))
        val staff = Staff(dto.id, dto.userId, dto.name ?: name, dto.phone, Role.valueOf(dto.role), dto.storeId, true)
        local.insertStaff(staff)
        staff
    }

    override suspend fun updateRole(storeId: String, staffId: String, role: Role): Result<Staff> = runCatching {
        val dto = api.updateStaff(storeId, staffId, UpdateStaffRequest(role = role.name))
        local.updateRole(staffId, role)
        Staff(dto.id, dto.userId, dto.name ?: "", dto.phone, Role.valueOf(dto.role), dto.storeId, true)
    }

    override suspend fun deactivate(storeId: String, staffId: String): Result<Unit> = runCatching {
        api.deactivateStaff(storeId, staffId)
        local.deactivate(staffId)
    }

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = runCatching {
        val dtos = api.getStaff(storeId)
        local.insertStaffList(dtos.map { Staff(it.id, it.userId, it.name ?: "", it.phone, Role.valueOf(it.role), it.storeId, true) })
    }
}
