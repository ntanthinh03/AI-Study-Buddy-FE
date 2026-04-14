package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.ChangePasswordRequest
import com.thinh.aistudybuddy.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ChangePasswordViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set
    var sessionExpired by mutableStateOf(false)
        private set

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }

    fun consumeSessionExpired() {
        sessionExpired = false
    }

    fun changePassword(oldPassword: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            successMessage = null

            if (!RetrofitClient.hasUsableAuthToken()) {
                RetrofitClient.logAuthTokenDiagnostics("Blocked change-password call: token missing or expired")
                errorMessage = "Session expired. Please log in again."
                sessionExpired = true
                isLoading = false
                return@launch
            }

            try {
                val response = RetrofitClient.instance.changePassword(
                    ChangePasswordRequest(oldPassword = oldPassword, newPassword = newPassword)
                )
                successMessage = response.message.ifBlank { "Password changed successfully." }
                onSuccess()
            } catch (e: Exception) {
                errorMessage = when (e) {
                    is HttpException -> when (e.code()) {
                        400 -> "This account does not support local password change."
                        401 -> {
                            val tokenExpired = RetrofitClient.isAuthTokenExpired()
                            if (tokenExpired) {
                                RetrofitClient.authToken = null
                                sessionExpired = true
                                "Session expired. Please log in again."
                            } else {
                                "Old password is incorrect."
                            }
                        }
                        404 -> "User not found."
                        else -> "Password change failed. Please try again."
                    }
                    else -> "Network error. Please check your connection and try again."
                }
            } finally {
                isLoading = false
            }
        }
    }
}

