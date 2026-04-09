package com.dokonpro.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.shared.domain.entity.ZakatCalculation
import com.dokonpro.shared.domain.entity.ZakatConfig
import com.dokonpro.shared.domain.usecase.CalculateZakatUseCase
import com.dokonpro.shared.domain.usecase.GetZakatHistoryUseCase
import com.dokonpro.shared.domain.usecase.SaveZakatCalculationUseCase
import com.dokonpro.shared.domain.repository.ZakatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ZakatState(
    val calculation: ZakatCalculation? = null,
    val history: List<ZakatCalculation> = emptyList(),
    val config: ZakatConfig? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class ZakatViewModel(
    private val calculateZakat: CalculateZakatUseCase,
    private val saveZakatCalculation: SaveZakatCalculationUseCase,
    private val getZakatHistory: GetZakatHistoryUseCase,
    private val zakatRepository: ZakatRepository,
    private val storeId: String
) : ViewModel() {

    private val _state = MutableStateFlow(ZakatState())
    val state: StateFlow<ZakatState> = _state.asStateFlow()

    init {
        loadHistory()
        loadConfig()
    }

    fun calculate() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, isSaved = false)
            calculateZakat(storeId)
                .onSuccess { calculation ->
                    _state.value = _state.value.copy(
                        calculation = calculation,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
        }
    }

    fun save() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            saveZakatCalculation(storeId)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isSaved = true,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            getZakatHistory(storeId).collect { history ->
                _state.value = _state.value.copy(history = history)
            }
        }
    }

    private fun loadConfig() {
        viewModelScope.launch {
            zakatRepository.getConfig(storeId)
                .onSuccess { config ->
                    _state.value = _state.value.copy(config = config)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(error = e.message)
                }
        }
    }

    fun updateConfig(goldRate: Double?, silverRate: Double?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            zakatRepository.updateConfig(storeId, goldRate, silverRate)
                .onSuccess { config ->
                    _state.value = _state.value.copy(
                        config = config,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearSaved() {
        _state.value = _state.value.copy(isSaved = false)
    }
}
