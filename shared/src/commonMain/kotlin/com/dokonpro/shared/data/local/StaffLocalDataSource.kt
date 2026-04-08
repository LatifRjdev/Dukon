package com.dokonpro.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dokonpro.shared.db.DokonProDatabase
import com.dokonpro.shared.domain.entity.Role
import com.dokonpro.shared.domain.entity.Staff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StaffLocalDataSource(private val db: DokonProDatabase) {
    fun getStaff(storeId: String): Flow<List<Staff>> =
        db.staffQueries.selectByStoreId(storeId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toStaff() } }

    fun insertStaff(staff: Staff) {
        db.staffQueries.insertOrReplace(staff.id, staff.userId, staff.name, staff.phone,
            staff.role.name, staff.storeId, if (staff.isActive) 1L else 0L)
    }

    fun insertStaffList(list: List<Staff>) { db.transaction { list.forEach { insertStaff(it) } } }

    fun updateRole(staffId: String, role: Role) { db.staffQueries.updateRole(role.name, staffId) }

    fun deactivate(staffId: String) { db.staffQueries.deactivate(staffId) }

    private fun com.dokonpro.shared.db.Staff.toStaff() = Staff(
        id, user_id, name, phone, Role.valueOf(role), store_id, is_active != 0L)
}
