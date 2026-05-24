package com.thinh.aistudybuddy.data.models

import com.google.gson.annotations.SerializedName

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val hint: String = "",
    val explanation: String = ""
)

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

data class GenerateMoreQuestionsResponse(
    val quizId: String,
    val questions: List<BackendQuizQuestion>,
    val completed: Boolean
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

data class CreateQuizDto(
    val documentId: String,
    val questions: List<QuizQuestion>,
    val quizName: String? = null,
    val quizTitle: String? = null
)

data class QuizSaveResponse(
    val quizId: String,
    val quizName: String,
    val quizTitle: String,
    val conversationId: String,
    val questions: List<QuizQuestion>
)

data class QuizSubmitRequest(
    val quizId: String,
    val score: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val durationSeconds: Int = 0
)
