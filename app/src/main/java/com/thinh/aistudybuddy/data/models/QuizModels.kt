package com.thinh.aistudybuddy.data.models

import com.google.gson.annotations.SerializedName

/**
 * UI Model for Quiz Questions used across the app.
 */
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val hint: String = "",
    val explanation: String = ""
)

/**
 * Backend Model for Quiz Questions (e.g. from RAG or legacy endpoints).
 */
data class BackendQuizQuestion(
    val question: String, 
    val options: Map<String, String>, 
    val correctAnswer: String, 
    val explanation: String
)

data class GenerateQuizResponse(
    val quizId: String,
    val quizName: String? = null,
    val quizTitle: String? = null,
    val conversationId: String,
    val questions: List<BackendQuizQuestion>
)

data class Quiz(
    @SerializedName("quizId") val id: String,
    val quizName: String? = null,
    val quizTitle: String? = null,
    val documentId: String,
    val conversationId: String? = null,
    val questions: List<BackendQuizQuestion> = emptyList(),
    val createdAt: String? = null,
    val score: Int? = null
)
