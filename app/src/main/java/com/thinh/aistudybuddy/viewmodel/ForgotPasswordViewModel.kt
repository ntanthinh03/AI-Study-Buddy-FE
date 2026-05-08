package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.models.*
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

    fun sendOtp(
        email: String,
        onSuccess: (ForgotPasswordSendOtpResponse) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.instance.forgotPasswordSendOtp(
                    ForgotPasswordSendOtpRequest(email = email)
                )
                onSuccess(response)
            } catch (e: Exception) {
                errorMessage = mapSendOtpError(e)
            } finally {
                isLoading = false
            }
        }
    }

    fun verifyOtp(
        email: String,
        otp: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.instance.forgotPasswordVerifyOtp(
                    ForgotPasswordVerifyOtpRequest(
                        email = email,
                        otp = otp
                    )
                )
                onSuccess(response.message)
            } catch (e: Exception) {
                errorMessage = mapVerifyOtpError(e)
            } finally {
                isLoading = false
            }
        }
    }

    fun resetPassword(
        email: String,
        newPassword: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.instance.forgotPasswordResetPassword(
                    ForgotPasswordResetPasswordRequest(
                        email = email,
                        newPassword = newPassword
                    )
                )
                onSuccess(response.message)
            } catch (e: Exception) {
                errorMessage = when (e) {
                    is HttpException -> when (e.code()) {
                        400 -> "Password is too weak. Use 8-16 characters with uppercase, lowercase, a number, and a special character."
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

    private fun mapSendOtpError(error: Exception): String {
        if (error is HttpException) {
            return when (error.code()) {
                400 -> "Email is required or has invalid format."
                404 -> "No account found with this email."
                503 -> "Email service is unavailable. Please try again later."
                else -> "Could not send OTP. Please try again."
            }
        }
        return "Network error. Please check your connection and try again."
    }

    private fun mapVerifyOtpError(error: Exception): String {
        if (error is HttpException) {
            return when (error.code()) {
                400 -> "OTP is invalid or expired. Please try again."
                404 -> "No account found with this email."
                else -> "OTP verification failed. Please try again."
            }
        }
        return "Network error. Please check your connection and try again."
    }
}

