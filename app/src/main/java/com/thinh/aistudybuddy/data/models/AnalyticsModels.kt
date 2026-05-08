package com.thinh.aistudybuddy.data.models

import com.google.gson.annotations.SerializedName

data class UserStats(
    val totalQuizzes: Int,
    val totalFlashcards: Int,
    val accuracy: Int,
    val recentActivities: List<StudyActivity>
)

data class AnalyticsOverview(
    val totalXP: Int,
    val rank: Int,
    val nextLevelXP: Int,
    val progressToNextLevel: Float
)

data class StudyActivity(
    val id: String,
    val type: String,
    val score: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val durationSeconds: Int,
    @SerializedName("created_at")
    val createdAt: String
)

data class ChartDataPoint(
    val date: String,
    val count: Int
)

