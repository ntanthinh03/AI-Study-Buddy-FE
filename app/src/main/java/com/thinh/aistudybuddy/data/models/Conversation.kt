package com.thinh.aistudybuddy.data.model

import androidx.compose.runtime.mutableStateListOf

data class Conversation(
    val id: String,
    val title: String,
    val isQuiz: Boolean = false,
    val chatMessages: MutableList<ChatMessage> = mutableStateListOf()
)