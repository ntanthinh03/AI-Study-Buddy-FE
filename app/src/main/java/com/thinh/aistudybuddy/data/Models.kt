package com.thinh.aistudybuddy.data

import com.google.gson.JsonElement
import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName

data class User(val id: String, val email: String, val fullName: String)

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
    val phoneNumber: String
)
data class RegisterResponse(
    val message: String? = null,
    val token: String? = null,
    val user: User? = null
)
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

data class Document(
    val id: String,
    val fileName: String,
    val summary: String?,
    val status: String,
    val summaryStatus: String? = null,
    val ragStatus: String? = null
)

data class DocumentStatusResponse(
    val id: String,
    val status: String,
    val progress: Int? = null,
    val errorMessage: String? = null
)

data class ChatMessage(
    val id: String,
    val question: String = "",
    val answer: String = "",
    val createdAt: String = "",
    val messageType: String? = null,
    val artifactType: String? = null,
    val artifactJson: JsonElement? = null,
    val imageMimeType: String? = null,
    val imageOriginalName: String? = null
)

data class DocumentChatRequest(@SerializedName("question") val question: String)
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
data class StudyPlanApiResponse(
    @SerializedName("studyPlan")
    val studyPlan: JsonElement? = null
)
data class RagUploadResponse(val success: Boolean, val documentId: String? = null, val chunks: Int? = null, val status: String = "PROCESSING")

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

data class QuizQuestion(val question: String, val options: Map<String, String>, val correctAnswer: String, val explanation: String)

data class Quiz(val id: String, val title: String, val documentId: String, val questions: List<QuizQuestion>)

data class ConversationInfo(
    val id: String,
    val userId: String,
    val documentId: String? = null,
    val title: String,
    val kind: String? = null,
    val lastMessagePreview: String? = null,
    val lastArtifactType: String? = null,
    val lastMessageAt: String? = null,
    val document: Document? = null
)

data class ConversationMessage(
    val id: String,
    val messageType: String? = null,
    val question: String? = null,
    val answer: String? = null,
    val artifactType: String? = null,
    val artifactJson: JsonElement? = null,
    val createdAt: String? = null,
    val imageMimeType: String? = null,
    val imageOriginalName: String? = null
)

data class SaveDocumentArtifactRequest(
    val artifactType: String,
    val artifact: JsonElement,
    val note: String? = null
)

data class ProgressLessonRequest(
    val documentId: String,
    val title: String,
    val contentText: String
)

data class ProgressLesson(
    val id: String,
    val userId: String,
    val documentId: String? = null,
    val title: String,
    val contentText: String,
    val quizJson: JsonElement? = null,
    val lastStudiedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class SaveLessonQuizRequest(
    val quiz: JsonArray
)

data class SaveLessonQuizResponse(
    val message: String,
    val lessonId: String,
    val quizCount: Int
)

data class ImageMessageResponse(
    val messageId: String,
    val mimeType: String,
    val originalName: String,
    val base64: String
)

