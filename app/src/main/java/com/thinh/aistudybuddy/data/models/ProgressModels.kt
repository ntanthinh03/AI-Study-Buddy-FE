package com.thinh.aistudybuddy.data.models

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val userScore: Int? = null,
    val isCompleted: Boolean = false,
    val documentId: String = "",
    val order: Int = 0,
    val objective: String = "",
    val estimatedMinutes: Int = 0,
    val difficulty: String = "BEGINNER",
    val status: ModuleStatus = ModuleStatus.LOCKED
)

data class StudyPlan(
    val id: String,
    val title: String,
    val lessons: List<Lesson>,
    val overview: String = "",
    val estimatedTotalMinutes: Int = 0,
    val intensity: StudyIntensity = StudyIntensity.SMART
)

enum class StudyIntensity(val label: String, val icon: String, val lessonsPerDay: Int) {
    CHILL("Chill", "🐢", 1),
    SMART("Smart", "🦊", 3),
    HARDCORE("Hardcore", "🐲", 5)
}

enum class ModuleStatus {
    LOCKED,
    IN_PROGRESS,
    COMPLETED
}

data class ProgressLessonRequest(
    val conversationId: String,
    val courseName: String? = null,
    val title: String,
    val contentText: String,
    val documentId: String? = null,
    val status: String? = null
)

data class ProgressLesson(
    val id: String,
    val userId: String,
    val conversationId: String,
    val documentId: String? = null,
    val courseName: String? = null,
    val lessonTitle: String? = null,
    val title: String,
    val contentText: String,
    val status: String? = null,
    val quizJson: JsonElement? = null,
    val lastStudiedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class SaveLessonQuizRequest(
    val quiz: JsonArray
)

data class SaveLessonQuizResponse(
    val message: String,
    val lessonId: String,
    val quizCount: Int
)

data class ProgressLessonStatusRequest(
    val status: String
)

data class StudyPlanApiResponse(
    @SerializedName("studyPlan")
    val studyPlan: JsonElement? = null
)

data class StudyProgressItem(
    @SerializedName("documentId")
    val documentId: String,
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("status")
    val status: ModuleStatus,
    @SerializedName("score")
    val score: Int
)

data class ProgressInitRequest(
    @SerializedName("documentId")
    val documentId: String
)

data class ProgressCompleteRequest(
    @SerializedName("documentId")
    val documentId: String,
    @SerializedName("score")
    val score: Int
)
