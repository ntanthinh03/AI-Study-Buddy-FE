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
    val level: Int,
    val learningMode: String? = "BALANCED",
    val preferredNotificationTime: String? = "20:00",
    val streakFreezeAvailable: Int? = 1,
    val elo: Int? = 1200,
    val versusWinStreak: Int? = 0,
    val arenaName: String? = null,
    val user: UserStatsUser? = null
)

data class UserStatsUser(
    val id: String,
    val email: String,
    val fullName: String?,
    val avatar: String? = null
)

data class VersusMatchResponse(
    val id: String,
    val status: String,
    val mode: String? = "BOT",
    val difficulty: String? = "MEDIUM",
    val playerScore: Int,
    val botScore: Int,
    val playerCorrectCount: Int,
    val botCorrectCount: Int,
    val questions: List<BackendQuizQuestion>,
    val playerAnswers: Map<String, PlayerAnswerDetail>?,
    val botAnswers: Map<String, BotAnswerDetail>?,
    val roomCode: String? = null,
    val opponentName: String? = null,
    val opponentElo: Int? = null,
    val hostName: String? = null,
    val hostElo: Int? = null
)

data class VersusHistoryEntry(
    val matchId: String,
    val opponentName: String,
    val opponentElo: Int,
    val resultText: String,
    val scoreText: String,
    val dateText: String,
    val mode: String
)

data class PlayerAnswerDetail(
    val selectedAnswer: String,
    val scoreEarned: Int,
    val timeTakenSeconds: Double
)

data class BotAnswerDetail(
    val selectedAnswer: String,
    val isCorrect: Boolean
)

data class VersusAnswerSubmitRequest(
    val questionIndex: Int,
    val selectedAnswer: String,
    val timeTakenSeconds: Double
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

data class VersusLockoutStatusResponse(
    val locked: Boolean,
    val remainingSeconds: Long,
    val warningsCount: Int
)

data class VersusQuitResponse(
    val success: Boolean,
    val warningsCount: Int,
    val locked: Boolean,
    val lockoutUntil: String?
)
