package com.dokonpro.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.shared.domain.entity.StoreSettings
import com.dokonpro.shared.domain.usecase.GetStoreSettingsUseCase
import com.dokonpro.shared.domain.usecase.LogoutUseCase
import com.dokonpro.shared.domain.usecase.UpdateStoreSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val settings: StoreSettings? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(
    private val getSettings: GetStoreSettingsUseCase,
    private val updateSettings: UpdateStoreSettingsUseCase,
    private val logout: LogoutUseCase,
    private val storeId: String
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            getSettings(storeId)
                .onSuccess { settings ->
                    _state.value = _state.value.copy(settings = settings, isLoading = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun saveSettings(settings: StoreSettings) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, isSaved = false, error = null)
            updateSettings(storeId, settings)
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        settings = updated,
                        isLoading = false,
                        isSaved = true
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun performLogout(onComplete: () -> Unit) {
        viewModelScope.launch {
            logout()
            onComplete()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearSaved() {
        _state.value = _state.value.copy(isSaved = false)
    }
}
