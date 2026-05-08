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

class LoginViewModel : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun onLoginClick(onSuccess: (String) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            errorMessage = "Please enter both email and password."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val request = LoginRequest(email, password)
                val response = RetrofitClient.instance.login(request)

                if (response.token.isBlank()) {
                    RetrofitClient.updateAuthToken(null)
                    RetrofitClient.logAuthTokenDiagnostics("Login success response but token is blank")
                    errorMessage = "Login failed: missing access token in response."
                    return@launch
                }

                RetrofitClient.updateAuthToken(response.token)

                onSuccess(response.user.fullName.ifBlank { response.user.email })
            } catch (e: Exception) {
                RetrofitClient.updateAuthToken(null)
                errorMessage = when (e) {
                    is HttpException -> {
                        when (e.code()) {
                            401 -> "Incorrect account or password."
                            403 -> "Your account is not allowed to sign in."
                            else -> "Login failed. Please try again."
                        }
                    }
                    else -> "Login failed. Please check your network and try again."
                }
            } finally {
                isLoading = false
            }
        }
    }
}