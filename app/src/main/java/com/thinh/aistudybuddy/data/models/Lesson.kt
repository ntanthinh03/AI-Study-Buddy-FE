package com.thinh.aistudybuddy.data.model

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val quizQuestions: List<QuizQuestion>,
    val userScore: Int? = null,
    val isCompleted: Boolean = false // Thêm trường này
)

data class StudyPlan(
    val id: String,
    val title: String,
    val lessons: List<Lesson>
)