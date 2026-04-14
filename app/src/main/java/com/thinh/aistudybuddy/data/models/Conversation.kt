package com.thinh.aistudybuddy.data.model

import androidx.compose.runtime.mutableStateListOf

enum class ConversationKind {
    CHAT,
    QUIZ,
    PLAN
}

data class Conversation(
    val id: String,
    val title: String,
    val isQuiz: Boolean = false,
    val kind: ConversationKind = ConversationKind.CHAT,
    val autoTitleApplied: Boolean = false,
    val documentId: String? = null,
    val chatMessages: MutableList<ChatMessage> = mutableStateListOf()
)