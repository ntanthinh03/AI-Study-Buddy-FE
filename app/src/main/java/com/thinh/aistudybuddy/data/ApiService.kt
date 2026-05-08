@file:Suppress("unused")

package com.thinh.aistudybuddy.data

import com.thinh.aistudybuddy.data.models.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
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
    ): List<BackendChatMessage>

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

    @POST("documents/{id}/mindmap")
    suspend fun generateMindMap(
        @Path("id") id: String
    ): Response<MindMapResponse>

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

    @POST("flashcards/generate/{documentId}")
    suspend fun generateFlashcards(
        @Path("documentId") documentId: String
    ): List<Flashcard>

    @GET("flashcards")
    suspend fun getFlashcards(): List<Flashcard>

    @GET("flashcards/review")
    suspend fun getFlashcardsToReview(): List<Flashcard>

    @PATCH("flashcards/{id}/review")
    suspend fun updateFlashcardReview(
        @Path("id") id: String,
        @Body request: ReviewUpdateRequest
    ): Flashcard

    @POST("flashcards")
    suspend fun createFlashcard(
        @Body request: CreateFlashcardRequest
    ): Flashcard

    @GET("analytics/stats")
    suspend fun getStats(): UserStats

    @GET("analytics/overview")
    suspend fun getAnalyticsOverview(): Response<AnalyticsOverview>

    @GET("study-sessions/daily")
    suspend fun getDailySession(): Response<StudySession>

    @POST("study-sessions/{id}/submit")
    suspend fun submitSessionResult(
        @Path("id") id: String,
        @Body result: SessionSubmitResult
    ): Response<GamificationStats>

    @GET("study-sessions/stats")
    suspend fun getUserStats(): Response<GamificationStats>

    @POST("study-sessions/generate-batch/{documentId}")
    suspend fun generateBatch(
        @Path("documentId") documentId: String
    ): Response<BatchGenResponse>

    @POST("study-sessions/mock-exam")
    suspend fun generateMockExam(
        @Body request: MockExamRequest
    ): Response<StudySession>

    @POST("study-sessions/focus")
    suspend fun submitFocusSession(
        @Body request: FocusSubmitRequest
    ): Response<GamificationStats>

    @GET("study-sessions/leaderboard")
    suspend fun getLeaderboard(): Response<List<LeaderboardEntry>>

    @GET("analytics/chart")
    suspend fun getChartData(): List<ChartDataPoint>

    @POST("quizzes/submit")
    suspend fun submitQuizResult(
        @Body request: QuizSubmitRequest
    ): MessageResponse

    @POST("quizzes")
    suspend fun saveQuiz(
        @Body request: CreateQuizDto
    ): QuizSaveResponse
}