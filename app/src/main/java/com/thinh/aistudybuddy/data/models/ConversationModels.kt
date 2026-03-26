package com.thinh.aistudybuddy.data.model

data class Conversation(
    val id: String,
    val title: String,
    val chatMessages: MutableList<ChatMessage> = mutableListOf(),
    val isQuiz: Boolean = false
)