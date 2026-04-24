@file:Suppress("unused")

package com.thinh.aistudybuddy.data

import com.thinh.aistudybuddy.data.model.ProgressCompleteRequest
import com.thinh.aistudybuddy.data.model.ProgressInitRequest
import com.thinh.aistudybuddy.data.model.StudyProgressItem
import com.thinh.aistudybuddy.data.StudyPlanApiResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @Headers("No-Auth: true")
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): MessageResponse

    @Headers("No-Auth: true")
    @POST("auth/forgot-password/send-otp")
    suspend fun forgotPasswordSendOtp(@Body request: ForgotPasswordSendOtpRequest): ForgotPasswordSendOtpResponse

    @Headers("No-Auth: true")
    @POST("auth/forgot-password/verify-otp")
    suspend fun forgotPasswordVerifyOtp(@Body request: ForgotPasswordVerifyOtpRequest): MessageResponse

    @Headers("No-Auth: true")
    @POST("auth/forgot-password/reset-password")
    suspend fun forgotPasswordResetPassword(@Body request: ForgotPasswordResetPasswordRequest): MessageResponse

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): MessageResponse

    @GET("documents")
    suspend fun getDocuments(): List<Document>

    @GET("documents/{id}/status")
    suspend fun getDocumentStatus(
        @Path("id") docId: String
    ): DocumentStatusResponse

    @Multipart
    @POST("documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part
    ): Document

    @Multipart
    @POST("rag/upload")
    suspend fun uploadRagPdf(
        @Part file: MultipartBody.Part
    ): RagUploadResponse

    @Multipart
    @POST("rag/upload-image")
    suspend fun uploadRagImage(
        @Part file: MultipartBody.Part
    ): RagUploadResponse

    @DELETE("documents/{id}")
    suspend fun deleteDocument(
        @Path("id") docId: String
    )

    @POST("documents/{id}/chat")
    suspend fun sendQuestion(
        @Path("id") docId: String,
        @Body request: DocumentChatRequest
    ): ChatResponse

    @GET("documents/{id}/history")
    suspend fun getChatHistory(
        @Path("id") docId: String
    ): List<ChatMessage>

    @POST("documents/{id}/history/artifact")
    suspend fun saveDocumentArtifact(
        @Path("id") docId: String,
        @Body request: SaveDocumentArtifactRequest
    ): MessageResponse

    @POST("quizzes/generate/{documentId}")
    suspend fun generateQuiz(
        @Path("documentId") documentId: String
    ): GenerateQuizResponse

    @POST("documents/{id}/study-plan")
    suspend fun generateStudyPlan(
        @Path("id") docId: String
    ): StudyPlanApiResponse

    @GET("quizzes")
    suspend fun getQuizzes(): List<Quiz>

    @POST("chat/ask")
    suspend fun askAI(@Body request: AskAiRequest): ChatAskResponse

    @Multipart
    @POST("chat/ask-image")
    suspend fun askAIWithImage(
        @Part image: MultipartBody.Part,
        @Part("question") question: okhttp3.RequestBody,
        @Part("conversationId") conversationId: okhttp3.RequestBody? = null,
        @Part("title") title: okhttp3.RequestBody? = null
    ): ChatAskResponse

    @GET("chat/messages/{messageId}/image")
    suspend fun getMessageImage(
        @Path("messageId") messageId: String
    ): ImageMessageResponse

    @POST("ai/ask")
    suspend fun aiAsk(@Body request: AiAskRequest): ChatResponse

    @GET("progress/me")
    suspend fun getMyProgress(): List<StudyProgressItem>

    @GET("progress/timeline")
    suspend fun getProgressTimeline(): List<StudyProgressItem>

    @POST("progress/init")
    suspend fun initProgress(@Body request: ProgressInitRequest): ResponseBody

    @POST("progress/complete")
    suspend fun completeProgress(@Body request: ProgressCompleteRequest): ResponseBody

    @POST("progress/lessons")
    suspend fun createProgressLesson(@Body request: ProgressLessonRequest): ProgressLesson

    @GET("progress/lessons")
    suspend fun getProgressLessons(): List<ProgressLesson>

    @GET("progress/lessons/{lessonId}")
    suspend fun getProgressLessonDetail(
        @Path("lessonId") lessonId: String
    ): ProgressLesson

    @POST("progress/lessons/{lessonId}/quiz")
    suspend fun saveProgressLessonQuiz(
        @Path("lessonId") lessonId: String,
        @Body request: SaveLessonQuizRequest
    ): SaveLessonQuizResponse

    @POST("progress/lessons/{lessonId}/status")
    suspend fun updateProgressLessonStatus(
        @Path("lessonId") lessonId: String,
        @Body request: ProgressLessonStatusRequest
    ): MessageResponse

    @GET("conversations")
    suspend fun getConversations(): List<ConversationInfo>

    @GET("conversations/{conversationId}/messages")
    suspend fun getConversationMessages(
        @Path("conversationId") conversationId: String
    ): List<ConversationMessage>

    @DELETE("conversations/{conversationId}")
    suspend fun deleteConversation(
        @Path("conversationId") conversationId: String
    ): MessageResponse
}