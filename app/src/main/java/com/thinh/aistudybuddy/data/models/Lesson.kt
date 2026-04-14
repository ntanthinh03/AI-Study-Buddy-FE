package com.thinh.aistudybuddy.data.model

data class Lesson(
    val id: String,
    val title: String,
    val description: String = "",
    val content: String = "",
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val userScore: Int? = null,
    val isCompleted: Boolean = false,
    val documentId: String = "",
    val order: Int = 0,
    val objective: String = "",
    val estimatedMinutes: Int = 0,
    val difficulty: String = "BEGINNER",
    val status: ModuleStatus = ModuleStatus.IN_PROGRESS
)

data class StudyPlan(
    val id: String,
    val title: String,
    val lessons: List<Lesson>,
    val overview: String = "",
    val estimatedTotalMinutes: Int = 0
)