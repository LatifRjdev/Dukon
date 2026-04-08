package com.dokonpro.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dokonpro.android.ui.MainScreen
import com.dokonpro.android.ui.auth.OtpScreen
import com.dokonpro.android.ui.auth.PhoneInputScreen
import com.dokonpro.android.ui.auth.RegisterScreen
import com.dokonpro.android.viewmodel.AuthStep
import com.dokonpro.android.viewmodel.AuthViewModel
import org.koin.androidx.compose.koinViewModel

object Routes {
    const val AUTH = "auth"
    const val MAIN = "main"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(Routes.AUTH) {
            val viewModel: AuthViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            when (state.step) {
                AuthStep.PHONE_INPUT -> PhoneInputScreen(
                    phone = state.phone,
                    isLoading = state.isLoading,
                    error = state.error,
                    onPhoneChange = viewModel::updatePhone,
                    onSendOtp = viewModel::sendOtpCode
                )
                AuthStep.OTP_VERIFY -> OtpScreen(
                    phone = state.phone,
                    otp = state.otp,
                    isLoading = state.isLoading,
                    error = state.error,
                    onOtpChange = viewModel::updateOtp,
                    onVerify = viewModel::verifyOtpCode,
                    onBack = { viewModel.updateOtp("") }
                )
                AuthStep.REGISTER -> RegisterScreen(
                    name = state.name,
                    storeName = state.storeName,
                    isLoading = state.isLoading,
                    error = state.error,
                    onNameChange = viewModel::updateName,
                    onStoreNameChange = viewModel::updateStoreName,
                    onRegister = viewModel::registerUser
                )
                AuthStep.COMPLETE -> {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            }
        }
        composable(Routes.MAIN) {
            MainScreen()
        }
    }
}
