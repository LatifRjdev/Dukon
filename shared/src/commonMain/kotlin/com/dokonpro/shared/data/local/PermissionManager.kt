package com.dokonpro.shared.data.local

import com.dokonpro.shared.domain.entity.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionManager {
    private val _currentRole = MutableStateFlow(Role.CASHIER)
    val currentRole: StateFlow<Role> = _currentRole.asStateFlow()

    fun setRole(role: Role) {
        _currentRole.value = role
    }

    fun canManageStaff(): Boolean =
        _currentRole.value == Role.OWNER

    fun canManageProducts(): Boolean =
        _currentRole.value == Role.OWNER || _currentRole.value == Role.MANAGER

    fun canViewFinance(): Boolean =
        _currentRole.value == Role.OWNER || _currentRole.value == Role.MANAGER

    fun canAccessPOS(): Boolean = true

    fun canDeleteStore(): Boolean =
        _currentRole.value == Role.OWNER
}
