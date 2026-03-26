package com.thinh.aistudybuddy.model

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val hint: String,
    val explanation: String
)