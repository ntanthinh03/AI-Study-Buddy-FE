package com.thinh.aistudybuddy.data.models

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val major: String? = null,
    val avatar: String? = null
)

data class UserProfile(
    val id: String,
    val email: String,
    val nickname: String
)

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    @SerializedName(value = "access_token", alternate = ["token"])
    val token: String,
    val user: User
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val phoneNumber: String,
    val major: String? = null
)

data class RegisterResponse(
    val message: String? = null,
    val token: String? = null,
    val user: User? = null
)

data class SendOtpRequest(val type: String, val value: String)
data class VerifyOtpRequest(val type: String, val value: String, val otp: String)
data class UpdateMajorRequest(val major: String)
data class UpdateAvatarRequest(val avatar: String)

data class ForgotPasswordRequest(
    val email: String,
    val phoneNumber: String,
    val newPassword: String
)

data class ForgotPasswordSendOtpRequest(
    val email: String
)

data class ForgotPasswordSendOtpResponse(
    val message: String,
    val expiresInMinutes: Int? = null
)

data class ForgotPasswordVerifyOtpRequest(
    val email: String,
    val otp: String
)

data class ForgotPasswordResetPasswordRequest(
    val email: String,
    val newPassword: String
)

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

data class MessageResponse(val message: String)
