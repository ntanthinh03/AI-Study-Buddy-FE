package com.thinh.aistudybuddy.data

import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @GET("documents")
    suspend fun getDocuments(@Header("Authorization") token: String): List<Document>

    @Multipart
    @POST("documents/upload")
    suspend fun uploadDocument(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): Document

    @DELETE("documents/{id}")
    suspend fun deleteDocument(
        @Header("Authorization") token: String,
        @Path("id") docId: String
    )

    @POST("documents/{id}/chat")
    suspend fun sendQuestion(
        @Header("Authorization") token: String,
        @Path("id") docId: String,
        @Body request: ChatRequest
    ): ChatResponse

    @GET("documents/{id}/history")
    suspend fun getChatHistory(
        @Header("Authorization") token: String,
        @Path("id") docId: String
    ): List<ChatMessage>

    @POST("quizzes/generate/{documentId}")
    suspend fun generateQuiz(
        @Header("Authorization") token: String,
        @Path("documentId") documentId: String
    ): List<QuizQuestion>

    @GET("quizzes")
    suspend fun getQuizzes(@Header("Authorization") token: String): List<Quiz>

    @POST("chat/ask")
    suspend fun askAI(@Body request: ChatRequest): ChatResponse
}