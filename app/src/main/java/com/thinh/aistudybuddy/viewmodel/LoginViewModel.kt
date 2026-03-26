package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.LoginRequest
import com.thinh.aistudybuddy.data.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    // 🎨 Quản lý trạng thái UI
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // 🚀 Hàm xử lý Đăng nhập
    fun onLoginClick(onSuccess: () -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            errorMessage = "Vui lòng nhập đầy đủ thông tin Thinh ơi!"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val request = LoginRequest(email, password)
                // Gọi API thông qua RetrofitClient đã tạo
                val response = RetrofitClient.instance.login(request)

                // ✅ Lưu Token vào Client để dùng cho các API sau
                RetrofitClient.authToken = response.token

                onSuccess() // Chuyển sang màn hình Home
            } catch (e: Exception) {
                errorMessage = "Lỗi rồi: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }
}