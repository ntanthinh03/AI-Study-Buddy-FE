package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.RegisterRequest
import com.thinh.aistudybuddy.data.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    var fullName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun onRegisterClick(onSuccess: () -> Unit) {
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorMessage = "Please fill in all required fields."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val request = RegisterRequest(email = email, password = password, fullName = fullName)
                RetrofitClient.instance.register(request)
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Registration failed: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }
}