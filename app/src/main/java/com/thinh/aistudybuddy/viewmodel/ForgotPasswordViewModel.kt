package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.ForgotPasswordRequest
import com.thinh.aistudybuddy.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ForgotPasswordViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        errorMessage = null
    }

    fun resetPassword(
        email: String,
        phoneNumber: String,
        newPassword: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.instance.forgotPassword(
                    ForgotPasswordRequest(
                        email = email,
                        phoneNumber = phoneNumber,
                        newPassword = newPassword
                    )
                )
                onSuccess(response.message)
            } catch (e: Exception) {
                errorMessage = when (e) {
                    is HttpException -> when (e.code()) {
                        400 -> "This account does not have a phone number for verification."
                        401 -> "Email or phone number is incorrect."
                        404 -> "No account found with this email."
                        else -> "Password reset failed. Please try again."
                    }
                    else -> "Network error. Please check your connection and try again."
                }
            } finally {
                isLoading = false
            }
        }
    }
}

