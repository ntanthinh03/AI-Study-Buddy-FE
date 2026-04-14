package com.thinh.aistudybuddy.data.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("message") val message: String
)

data class ChatResponse(
    @SerializedName("answer") val answer: String
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val attachmentName: String? = null,
    val showQuizButton: Boolean = false,
    val showStudyPlanButton: Boolean = false,
    val isProcessing: Boolean = false,
    val imageBase64: String? = null,
    val imageMimeType: String? = null,
    val imageOriginalName: String? = null
)