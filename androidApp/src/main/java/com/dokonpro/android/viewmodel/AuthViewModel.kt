package com.dokonpro.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.shared.domain.usecase.RegisterUseCase
import com.dokonpro.shared.domain.usecase.SendOtpUseCase
import com.dokonpro.shared.domain.usecase.VerifyOtpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val phone: String = "",
    val otp: String = "",
    val name: String = "",
    val storeName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val step: AuthStep = AuthStep.PHONE_INPUT
)

enum class AuthStep {
    PHONE_INPUT,
    OTP_VERIFY,
    REGISTER,
    COMPLETE
}

class AuthViewModel(
    private val sendOtp: SendOtpUseCase,
    private val verifyOtp: VerifyOtpUseCase,
    private val register: RegisterUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun updatePhone(phone: String) {
        _state.value = _state.value.copy(phone = phone, error = null)
    }

    fun updateOtp(otp: String) {
        _state.value = _state.value.copy(otp = otp, error = null)
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name, error = null)
    }

    fun updateStoreName(storeName: String) {
        _state.value = _state.value.copy(storeName = storeName, error = null)
    }

    fun sendOtpCode() {
        val fullPhone = "+992${_state.value.phone}"
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            sendOtp(fullPhone)
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false, step = AuthStep.OTP_VERIFY)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun verifyOtpCode() {
        val fullPhone = "+992${_state.value.phone}"
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            verifyOtp(fullPhone, _state.value.otp)
                .onSuccess { result ->
                    val nextStep = if (result.isNewUser) AuthStep.REGISTER else AuthStep.COMPLETE
                    _state.value = _state.value.copy(isLoading = false, step = nextStep)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun registerUser() {
        val fullPhone = "+992${_state.value.phone}"
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            register(fullPhone, _state.value.name, _state.value.storeName)
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false, step = AuthStep.COMPLETE)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
        }
    }
}
