package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.models.*
import com.thinh.aistudybuddy.services.network.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException

class UserAccountViewModel : ViewModel() {
    var user by mutableStateOf<User?>(null)
        private set

    var isLoading by mutableStateOf(false)
    var isOtpSending by mutableStateOf(false)
    var isOtpVerifying by mutableStateOf(false)
    
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    var otpSentSuccessfully by mutableStateOf(false)
    var showOtpDialog by mutableStateOf(false)
    var currentEditType by mutableStateOf("")
    var pendingNewValue by mutableStateOf("")

    fun loadProfile() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val profile = RetrofitClient.instance.getProfile()
                user = profile
            } catch (e: Exception) {
                errorMessage = "Failed to load scholar profile details."
            } finally {
                isLoading = false
            }
        }
    }

    fun selectMajor(major: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val updatedUser = RetrofitClient.instance.updateMajor(UpdateMajorRequest(major))
                user = updatedUser
                successMessage = "Study domain successfully switched to $major."
            } catch (e: Exception) {
                errorMessage = "Failed to update study major."
            } finally {
                isLoading = false
            }
        }
    }

    fun requestOtp(type: String, newValue: String) {
        if (newValue.isBlank()) {
            errorMessage = "Value cannot be empty."
            return
        }
        viewModelScope.launch {
            isOtpSending = true
            errorMessage = null
            successMessage = null
            try {
                val response = RetrofitClient.instance.sendProfileOtp(SendOtpRequest(type, newValue.trim()))
                currentEditType = type
                pendingNewValue = newValue.trim()
                otpSentSuccessfully = true
                showOtpDialog = true
                successMessage = response.message ?: "Verification code sent to your email."
            } catch (e: HttpException) {
                val backendMessage = runCatching {
                    val body = e.response()?.errorBody()?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        when (val msg = json.opt("message")) {
                            is String -> msg
                            is JSONArray -> msg.optString(0)
                            else -> null
                        }
                    } else null
                }.getOrNull()
                errorMessage = backendMessage ?: "Failed to send verification code. Please check your input."
            } catch (e: Exception) {
                errorMessage = "Failed to request OTP. Please try again."
            } finally {
                isOtpSending = false
            }
        }
    }

    fun verifyOtp(otp: String, onComplete: () -> Unit) {
        if (otp.length != 6) {
            errorMessage = "Verification code must be exactly 6 digits."
            return
        }
        viewModelScope.launch {
            isOtpVerifying = true
            errorMessage = null
            successMessage = null
            try {
                val updatedUser = RetrofitClient.instance.verifyProfileOtp(
                    VerifyOtpRequest(type = currentEditType, value = pendingNewValue, otp = otp)
                )
                user = updatedUser
                showOtpDialog = false
                otpSentSuccessfully = false
                successMessage = "Profile successfully updated."
                onComplete()
            } catch (e: HttpException) {
                val backendMessage = runCatching {
                    val body = e.response()?.errorBody()?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        when (val msg = json.opt("message")) {
                            is String -> msg
                            is JSONArray -> msg.optString(0)
                            else -> null
                        }
                    } else null
                }.getOrNull()
                errorMessage = backendMessage ?: "Incorrect verification code."
            } catch (e: Exception) {
                errorMessage = "Failed to verify. Please try again."
            } finally {
                isOtpVerifying = false
            }
        }
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }

    fun updateAvatar(base64Image: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val updatedUser = RetrofitClient.instance.updateAvatar(UpdateAvatarRequest(base64Image))
                user = updatedUser
                successMessage = "Profile picture updated successfully!"
            } catch (e: Exception) {
                errorMessage = "Failed to update profile picture."
            } finally {
                isLoading = false
            }
        }
    }
}
