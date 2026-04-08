package com.dokonpro.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.shared.domain.entity.Role
import com.dokonpro.shared.domain.entity.Staff
import com.dokonpro.shared.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StaffListState(
    val staff: List<Staff> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class StaffViewModel(
    private val getStaff: GetStaffUseCase,
    private val addStaff: AddStaffUseCase,
    private val updateStaffRole: UpdateStaffRoleUseCase,
    private val deactivateStaff: DeactivateStaffUseCase,
    private val storeId: String
) : ViewModel() {
    private val _state = MutableStateFlow(StaffListState())
    val state: StateFlow<StaffListState> = _state.asStateFlow()

    init { loadStaff() }

    private fun loadStaff() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getStaff(storeId).collect { _state.value = _state.value.copy(staff = it, isLoading = false) }
        }
    }

    fun addNewStaff(phone: String, name: String, role: Role) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            addStaff(storeId, phone, name, role)
                .onFailure { _state.value = _state.value.copy(error = it.message, isLoading = false) }
                .onSuccess { _state.value = _state.value.copy(isLoading = false) }
        }
    }

    fun changeRole(staffId: String, role: Role) {
        viewModelScope.launch {
            updateStaffRole(storeId, staffId, role)
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun removeStaff(staffId: String) {
        viewModelScope.launch {
            deactivateStaff(storeId, staffId)
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
