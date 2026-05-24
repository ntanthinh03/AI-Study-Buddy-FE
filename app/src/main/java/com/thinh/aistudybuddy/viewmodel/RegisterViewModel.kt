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

class RegisterViewModel : ViewModel() {
    var fullName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var phoneNumber by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val phoneRegex = Regex("^[+]?[0-9]{9,15}$")

    fun onRegisterClick(onSuccess: () -> Unit) {
        val normalizedPhone = phoneNumber.trim().replace(" ", "")

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || normalizedPhone.isEmpty()) {
            errorMessage = "Please fill in all required fields."
            return
        }

        if (!phoneRegex.matches(normalizedPhone)) {
            errorMessage = "Invalid phone number."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val request = RegisterRequest(
                    email = email,
                    password = password,
                    fullName = fullName,
                    phoneNumber = normalizedPhone
                )
                RetrofitClient.instance.register(request)
                onSuccess()
            } catch (e: HttpException) {
                val backendMessage = runCatching {
                    val body = e.response()?.errorBody()?.string().orEmpty()
                    if (body.isBlank()) {
                        null
                    } else {
                        val json = JSONObject(body)
                        when (val message = json.opt("message")) {
                            is String -> message
                            is JSONArray -> message.optString(0).takeIf { it.isNotBlank() }
                            else -> null
                        }
                    }
                }.getOrNull()

                errorMessage = backendMessage
                    ?: if (e.code() == 400) "Please check your registration details."
                    else "Registration failed. Please try again."
            } catch (_: Exception) {
                errorMessage = "Registration failed. Please check your network and try again."
            } finally {
                isLoading = false
            }
        }
    }
}