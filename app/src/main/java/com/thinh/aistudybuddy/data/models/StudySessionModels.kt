package com.thinh.aistudybuddy.data.models

import com.google.gson.annotations.SerializedName

data class StudySession(
    val id: String,
    val status: String,
    val content: SessionContent,
    val xpEarned: Int,
    val correctAnswers: Int,
    val createdAt: String,
    val completedAt: String?
)

data class SessionContent(
    @SerializedName("quizQuestions") val quizQuestions: List<BackendQuizQuestion>,
    @SerializedName("flashcards") val flashcards: List<Flashcard>
)

data class GamificationStats(
    val totalXP: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastStudyDate: String?,
    val level: Int
)

data class SessionSubmitResult(
    val correctAnswers: Int,
    val totalQuestions: Int
)

data class BatchGenResponse(
    val quizCount: Int,
    val flashcardCount: Int
)

data class FocusSubmitRequest(
    val minutes: Int
)

data class LeaderboardEntry(
    val id: String,
    val name: String,
    val avatar: String?,
    val xp: Int,
    val level: Int,
    val rank: Int
)

data class MockExamRequest(
    @SerializedName("questionCount") val questionCount: Int = 20,
    @SerializedName("documentId") val documentId: String? = null
)
