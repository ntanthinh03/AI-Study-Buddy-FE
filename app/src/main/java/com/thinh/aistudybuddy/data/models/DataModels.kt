package com.thinh.aistudybuddy.data.model

import com.google.gson.annotations.SerializedName


data class Document(
    val id: String,
    val name: String,
    val size: Long,
    val type: String,
    val url: String
)



data class LoginResponse(
    val token: String,
    val user: UserProfile
)

data class RegisterResponse(
    val message: String
)

data class UserProfile(
    val id: String,
    val email: String,
    val nickname: String
)

data class Quiz(
    val id: String,
    val title: String,
    val score: Int? = null
)

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val hint: String,
    val explanation: String
)
