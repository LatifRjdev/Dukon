package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.data.local.PermissionManager
import com.dokonpro.shared.domain.entity.Role
import com.dokonpro.shared.domain.entity.Staff
import com.dokonpro.shared.domain.repository.StaffRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FakeStaffRepository : StaffRepository {
    val staffList = mutableListOf<Staff>()

    override fun getStaff(storeId: String): Flow<List<Staff>> =
        flowOf(staffList.filter { it.storeId == storeId && it.isActive })

    override suspend fun addStaff(storeId: String, phone: String, name: String, role: Role): Result<Staff> {
        val staff = Staff("s-${staffList.size}", "u-${staffList.size}", name, phone, role, storeId, true)
        staffList.add(staff)
        return Result.success(staff)
    }

    override suspend fun updateRole(storeId: String, staffId: String, role: Role): Result<Staff> {
        val idx = staffList.indexOfFirst { it.id == staffId }
        if (idx >= 0) staffList[idx] = staffList[idx].copy(role = role)
        return Result.success(staffList[idx])
    }

    override suspend fun deactivate(storeId: String, staffId: String): Result<Unit> {
        val idx = staffList.indexOfFirst { it.id == staffId }
        if (idx >= 0) staffList[idx] = staffList[idx].copy(isActive = false)
        return Result.success(Unit)
    }

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = Result.success(Unit)
}

class GetStaffUseCaseTest {
    private val repo = FakeStaffRepository()
    private val useCase = GetStaffUseCase(repo)

    @Test
    fun `should return active staff`() = runTest {
        repo.staffList.add(Staff("s1", "u1", "Ali", "+992901234567", Role.CASHIER, "store-1", true))
        repo.staffList.add(Staff("s2", "u2", "Bob", "+992902345678", Role.MANAGER, "store-1", false))
        val result = useCase("store-1").first()
        assertEquals(1, result.size)
        assertEquals("Ali", result[0].name)
    }
}

class AddStaffUseCaseTest {
    private val repo = FakeStaffRepository()
    private val useCase = AddStaffUseCase(repo)

    @Test
    fun `should add staff member`() = runTest {
        val result = useCase("store-1", "+992901234567", "Ali", Role.CASHIER)
        assertTrue(result.isSuccess)
        assertEquals(1, repo.staffList.size)
        assertEquals(Role.CASHIER, repo.staffList[0].role)
    }
}

class PermissionManagerTest {
    @Test
    fun `owner can manage staff`() {
        val pm = PermissionManager()
        pm.setRole(Role.OWNER)
        assertTrue(pm.canManageStaff())
        assertTrue(pm.canManageProducts())
        assertTrue(pm.canViewFinance())
        assertTrue(pm.canDeleteStore())
    }

    @Test
    fun `manager cannot manage staff or delete store`() {
        val pm = PermissionManager()
        pm.setRole(Role.MANAGER)
        assertFalse(pm.canManageStaff())
        assertTrue(pm.canManageProducts())
        assertTrue(pm.canViewFinance())
        assertFalse(pm.canDeleteStore())
    }

    @Test
    fun `cashier can only access POS`() {
        val pm = PermissionManager()
        pm.setRole(Role.CASHIER)
        assertFalse(pm.canManageStaff())
        assertFalse(pm.canManageProducts())
        assertFalse(pm.canViewFinance())
        assertTrue(pm.canAccessPOS())
    }
}
