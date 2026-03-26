package com.thinh.aistudybuddy.data

// Auth Models
data class User(val id: String, val email: String, val fullName: String)

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val user: User)

data class RegisterRequest(val email: String, val password: String, val fullName: String)
data class RegisterResponse(val token: String, val user: User)

// Document Models
data class Document(val id: String, val fileName: String, val summary: String?, val status: String)

// Chat Models
data class ChatMessage(val id: Int, val question: String, val answer: String, val createdAt: String)

data class ChatRequest(val question: String)
data class ChatResponse(val answer: String)

// Quiz Models
data class QuizQuestion(val question: String, val options: Map<String, String>, val correctAnswer: String, val explanation: String)

data class Quiz(val id: String, val title: String, val documentId: String, val questions: List<QuizQuestion>)
