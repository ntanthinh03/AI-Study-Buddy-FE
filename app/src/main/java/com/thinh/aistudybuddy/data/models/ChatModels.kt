package com.thinh.aistudybuddy.data.models

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * UI Models
 */
enum class ConversationKind {
    CHAT, QUIZ, PLAN
}

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val attachmentName: String? = null,
    val showQuizButton: Boolean = false,
    val showStudyPlanButton: Boolean = false,
    val planJson: String? = null,
    val specificTitle: String? = null,
    val messageLabel: String? = null,
    val courses: List<ChatMessageCourse> = emptyList(),
    val imageMimeType: String? = null,
    val imageOriginalName: String? = null,
    val imageBase64: String? = null,
    val messageType: String? = null,
    val artifactType: String? = null,
    val artifactJson: JsonElement? = null,
    val isProcessing: Boolean = false
)

data class ChatMessageCourse(
    val id: String,
    val title: String,
    val lessonCount: Int = 0
)

data class Conversation(
    val id: String,
    val title: String,
    val isQuiz: Boolean = false,
    val kind: ConversationKind = ConversationKind.CHAT,
    val autoTitleApplied: Boolean = false,
    val documentId: String? = null,
    val chatMessages: MutableList<ChatMessage> = mutableListOf()
)

/**
 * Backend DTOs (used for API communication)
 */
data class BackendChatMessage(
    val id: String,
    val messageLabel: String? = null,
    val question: String = "",
    val answer: String = "",
    val createdAt: String = "",
    val messageType: String? = null,
    val artifactType: String? = null,
    val artifactJson: JsonElement? = null,
    val imageMimeType: String? = null,
    val imageOriginalName: String? = null
)

data class ConversationMessage(
    @SerializedName("messageId") val id: String,
    val messageLabel: String? = null,
    val messageType: String? = null,
    val question: String? = null,
    val answer: String? = null,
    val artifactType: String? = null,
    val artifactJson: JsonElement? = null,
    val createdAt: String? = null,
    val imageMimeType: String? = null,
    val imageOriginalName: String? = null
)

data class AskAiRequest(
    @SerializedName("message") val message: String,
    @SerializedName("conversationId") val conversationId: String? = null,
    @SerializedName("title") val title: String? = null
)

data class AiAskRequest(@SerializedName("question") val question: String)

data class ChatAskResponse(
    val conversationId: String,
    val messageId: String,
    val question: String,
    val answer: String,
    val createdAt: String
)

data class ChatResponse(val answer: String)

data class ConversationInfo(
    @SerializedName("conversationId") val id: String,
    val userId: String,
    val documentId: String? = null,
    @SerializedName("conversationTitle") val title: String,
    @SerializedName("conversationKind") val kind: String? = null,
    val lastMessagePreview: String? = null,
    val lastArtifactType: String? = null,
    val lastMessageAt: String? = null,
    val document: Document? = null
)

data class ImageMessageResponse(
    val messageId: String,
    val mimeType: String,
    val originalName: String,
    val base64: String
)

enum class AskState {
    IDLE,
    UPLOADING,
    PROCESSING,
    COMPLETED,
    ERROR,
    RETRY_READY
}

data class AiAskSession(
    val id: String,
    val documentId: String? = null,
    val question: String,
    val answer: String = "",
    val state: AskState = AskState.IDLE,
    val errorMessage: String? = null,
    val uploadedFileName: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Suggestion(
    val title: String,
    val subtitle: String
)

data class Banner(
    val title: String,
    val ctaText: String
)
