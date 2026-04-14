package com.thinh.aistudybuddy.data.model

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

val DEFAULT_STUDY_PLAN_JSON = """
{
  "planId": "plan_ds_101",
  "title": "Data Structures Mastery",
  "overview": "A structured roadmap to master arrays, linked lists, and trees with guided practice and progress tracking.",
  "estimatedTotalMinutes": 180,
  "modules": [
    {
      "moduleId": "m1",
      "order": 1,
      "documentId": "doc_arrays_001",
      "title": "Arrays Core Concepts",
      "objective": "Understand contiguous memory, indexing, and traversal basics.",
      "estimatedMinutes": 45,
      "difficulty": "BEGINNER",
      "status": "IN_PROGRESS",
      "quiz": {
        "recommendedQuestionCount": 5,
        "passScore": 70
      }
    },
    {
      "moduleId": "m2",
      "order": 2,
      "documentId": "doc_linked_lists_001",
      "title": "Linked Lists Deep Dive",
      "objective": "Learn node-based structures, pointers, and dynamic memory behavior.",
      "estimatedMinutes": 60,
      "difficulty": "INTERMEDIATE",
      "status": "LOCKED",
      "quiz": {
        "recommendedQuestionCount": 5,
        "passScore": 70
      }
    },
    {
      "moduleId": "m3",
      "order": 3,
      "documentId": "doc_trees_001",
      "title": "Trees and Traversals",
      "objective": "Practice hierarchical data, recursion, and traversal strategies.",
      "estimatedMinutes": 75,
      "difficulty": "INTERMEDIATE",
      "status": "LOCKED",
      "quiz": {
        "recommendedQuestionCount": 5,
        "passScore": 75
      }
    }
  ]
}
"""

enum class ModuleStatus {
    LOCKED,
    IN_PROGRESS,
    COMPLETED
}

data class ModuleQuizConfig(
    @SerializedName("recommendedQuestionCount")
    val recommendedQuestionCount: Int,
    @SerializedName("passScore")
    val passScore: Int
)

data class StudyModule(
    @SerializedName("moduleId")
    val moduleId: String,
    @SerializedName("order")
    val order: Int,
    @SerializedName("documentId")
    val documentId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("objective")
    val objective: String,
    @SerializedName("estimatedMinutes")
    val estimatedMinutes: Int,
    @SerializedName("difficulty")
    val difficulty: String,
    @SerializedName("status")
    val status: ModuleStatus,
    @SerializedName("quiz")
    val quiz: ModuleQuizConfig?
)

data class StudyPlanResponse(
    @SerializedName("planId")
    val planId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("overview")
    val overview: String,
    @SerializedName("estimatedTotalMinutes")
    val estimatedTotalMinutes: Int,
    @SerializedName("modules")
    val modules: List<StudyModule>
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

object StudyPlanJsonParser {
    private val gson = GsonBuilder().create()

    fun parse(rawJson: String): StudyPlanResponse? {
        val root = runCatching { JsonParser().parse(rawJson) }.getOrNull() as? JsonObject ?: return null

        val directPlan = when {
            root.has("plan") && root.get("plan").isJsonObject -> {
                gson.fromJson(root.get("plan").asJsonObject, StudyPlanResponse::class.java)
            }
            root.has("planId") -> gson.fromJson(root, StudyPlanResponse::class.java)
            else -> null
        }

        return directPlan?.let(::normalize)
    }

    fun normalize(plan: StudyPlanResponse): StudyPlanResponse {
        val sortedModules = plan.modules
            .sortedBy { it.order }
            .mapIndexed { index, module ->
                val normalizedStatus = when {
                    index == 0 && module.status == ModuleStatus.LOCKED -> ModuleStatus.IN_PROGRESS
                    else -> module.status
                }
                module.copy(status = normalizedStatus)
            }

        return plan.copy(modules = sortedModules)
    }
}

fun StudyPlanResponse.toLegacyPlan(progressTimeline: List<StudyProgressItem> = emptyList()): StudyPlan {
    val timelineByDocumentId = progressTimeline.associateBy { it.documentId }

    return StudyPlan(
        id = planId,
        title = title,
        lessons = modules.map { module ->
            val progress = timelineByDocumentId[module.documentId]
            module.toLesson(progress)
        },
        overview = overview,
        estimatedTotalMinutes = estimatedTotalMinutes
    )
}

fun StudyModule.toLesson(progress: StudyProgressItem? = null): Lesson {
    val displayScore = progress?.score?.div(10)

    return Lesson(
        id = moduleId,
        title = title,
        description = objective,
        content = buildString {
            append(objective)
            append("\n\n")
            append("Difficulty: ")
            append(difficulty)
            append("\nEstimated time: ")
            append(estimatedMinutes)
            append(" minutes")
        },
        quizQuestions = generateModuleQuizQuestions(this),
        userScore = displayScore,
        isCompleted = progress?.status == ModuleStatus.COMPLETED || status == ModuleStatus.COMPLETED,
        documentId = documentId,
        order = order,
        objective = objective,
        estimatedMinutes = estimatedMinutes,
        difficulty = difficulty,
        status = progress?.status ?: status
    )
}

private fun generateModuleQuizQuestions(module: StudyModule): List<QuizQuestion> {
    val questionCount = module.quiz?.recommendedQuestionCount?.coerceAtLeast(3) ?: 3
    return List(questionCount) { index ->
        QuizQuestion(
            id = "${module.moduleId}-q${index + 1}",
            question = "${module.title}: Which statement best matches ${module.objective.lowercase()}?",
            options = listOf(
                "Focus on ${module.title.lowercase()} core ideas.",
                "Skip the main concept and memorize examples only.",
                "Ignore the structure and move on immediately.",
                "Treat the topic as unrelated to the lesson."
            ),
            correctAnswerIndex = 0,
            hint = "Think about the main learning objective.",
            explanation = "The correct answer reinforces the lesson objective: ${module.objective}"
        )
    }
}

