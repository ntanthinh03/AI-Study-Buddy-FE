package com.thinh.aistudybuddy.data.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("message") val message: String
)

data class ChatResponse(
    @SerializedName("answer") val answer: String
)

data class ChatMessage(
    val id: String, // Thêm dòng này để hết lỗi đỏ
    val text: String,
    val isUser: Boolean,
    val showQuizButton: Boolean = false,
    val showStudyPlanButton: Boolean = false
)