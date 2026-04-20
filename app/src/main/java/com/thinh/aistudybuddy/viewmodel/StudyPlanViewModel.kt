@file:Suppress("unused")

package com.thinh.aistudybuddy.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.thinh.aistudybuddy.data.AiAskRequest
import com.thinh.aistudybuddy.data.ProgressLesson
import com.thinh.aistudybuddy.data.ProgressLessonRequest
import com.thinh.aistudybuddy.data.SaveLessonQuizRequest
import com.thinh.aistudybuddy.data.local.CachedLessonEnrichment
import com.thinh.aistudybuddy.data.local.LocalHistoryStore
import com.thinh.aistudybuddy.data.model.DEFAULT_STUDY_PLAN_JSON
import com.thinh.aistudybuddy.data.model.ModuleStatus
import com.thinh.aistudybuddy.data.model.ProgressCompleteRequest
import com.thinh.aistudybuddy.data.model.ProgressInitRequest
import com.thinh.aistudybuddy.data.model.QuizQuestion
import com.thinh.aistudybuddy.data.model.StudyModule
import com.thinh.aistudybuddy.data.model.StudyPlan
import com.thinh.aistudybuddy.data.model.StudyPlanJsonParser
import com.thinh.aistudybuddy.data.model.StudyPlanResponse
import com.thinh.aistudybuddy.data.model.StudyProgressItem
import com.thinh.aistudybuddy.data.model.toLegacyPlan
import com.thinh.aistudybuddy.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class StudyPlanViewModel : ViewModel() {
    companion object {
        private const val TAG = "StudyPlanViewModel"
        private val UUID_REGEX = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
        )
    }

    private data class LessonEnrichment(
        val theory: String,
        val quizQuestions: List<QuizQuestion>
    )

    private val fallbackPlanResponse = checkNotNull(StudyPlanJsonParser.parse(DEFAULT_STUDY_PLAN_JSON))
    private var currentPlanJson by mutableStateOf(DEFAULT_STUDY_PLAN_JSON)

    private var studyPlanResponse by mutableStateOf<StudyPlanResponse?>(fallbackPlanResponse)
    private var lessonEnrichment by mutableStateOf<Map<String, LessonEnrichment>>(emptyMap())
    private var lessonEnrichmentLoading by mutableStateOf<Set<String>>(emptySet())
    private var backendLessonIdByModuleId by mutableStateOf<Map<String, String>>(emptyMap())
    private var pendingEnrichmentSyncModuleIds by mutableStateOf<Set<String>>(emptySet())
    private var initializedDocuments by mutableStateOf<Set<String>>(emptySet())
    var timeline by mutableStateOf<List<StudyProgressItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val activePlan: StudyPlan
        get() {
            val base = (studyPlanResponse ?: fallbackPlanResponse).toLegacyPlan(timeline)
            val enrichedLessons = base.lessons.map { lesson ->
                val enriched = lessonEnrichment[lesson.id]
                if (enriched == null) {
                    lesson
                } else {
                    lesson.copy(
                        content = enriched.theory,
                        quizQuestions = enriched.quizQuestions
                    )
                }
            }
            return base.copy(lessons = enrichedLessons)
        }

    init {
        val cached = LocalHistoryStore.loadStudyPlanState()
        if (cached != null) {
            val cachedPlan = StudyPlanJsonParser.parse(cached.rawJson)
            if (cachedPlan != null) {
                currentPlanJson = cached.rawJson
                studyPlanResponse = cachedPlan
                timeline = cached.timeline
                lessonEnrichment = (cached.lessonEnrichment.orEmpty()).mapValues { (_, value) ->
                    LessonEnrichment(
                        theory = value.theory,
                        quizQuestions = value.quizQuestions
                    )
                }
                backendLessonIdByModuleId = cached.backendLessonIdByModuleId.orEmpty()
                pendingEnrichmentSyncModuleIds = cached.pendingEnrichmentModuleIds.orEmpty().toSet()
            }
        }

        if (studyPlanResponse == null) {
            seedLocalProgressFromPlan(fallbackPlanResponse)
            persistStudyPlanState()
        }

        if (!RetrofitClient.authToken.isNullOrBlank()) {
            initializeRemoteProgress((studyPlanResponse ?: fallbackPlanResponse).modules)
        }
    }

    @Suppress("unused")
    fun loadStudyPlanFromJson(rawJson: String, initializeRemote: Boolean = true) {
        val parsed = StudyPlanJsonParser.parse(rawJson)
        if (parsed == null || parsed.modules.isEmpty()) {
            errorMessage = "Could not parse study plan. Please regenerate plan."
            return
        }

        studyPlanResponse = parsed
        currentPlanJson = rawJson
        errorMessage = null
        seedLocalProgressFromPlan(parsed)
        persistStudyPlanState()

        if (initializeRemote) {
            initializeRemoteProgress(parsed.modules)
        }
    }

    fun refreshProgressTimeline() {
        if (RetrofitClient.authToken.isNullOrBlank()) return

        viewModelScope.launch {
            loading = true
            errorMessage = null
            try {
                timeline = RetrofitClient.instance.getProgressTimeline()
                persistStudyPlanState()
            } catch (e: HttpException) {
                handleHttpError(e)
            } catch (_: IOException) {
                errorMessage = "Network failure. Please check your connection."
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to refresh progress."
            } finally {
                loading = false
            }
        }
    }

    @Suppress("unused")
    fun updateLessonScore(lessonId: String, correctAnswers: Int): Boolean {
        val module = currentModules.firstOrNull { it.moduleId == lessonId } ?: return false
        val currentBest = timeline.firstOrNull { it.documentId == module.documentId }?.score ?: 0
        val normalizedScore = (correctAnswers.coerceIn(0, 10) * 10).coerceIn(0, 100)

        return if (normalizedScore > currentBest) {
            timeline = upsertTimeline(module.documentId, module.title, normalizedScore)
            persistStudyPlanState()
            true
        } else {
            false
        }
    }

    fun completeLesson(lessonId: String, rawScore: Int) {
        val module = currentModules.firstOrNull { it.moduleId == lessonId } ?: return
        val maxScore = (module.quiz?.recommendedQuestionCount ?: 3).coerceAtLeast(1) * 10
        val normalizedScore = (rawScore.coerceAtLeast(0) * 100 / maxScore).coerceIn(0, 100)

        timeline = upsertTimeline(module.documentId, module.title, normalizedScore)
        persistStudyPlanState()

        if (RetrofitClient.authToken.isNullOrBlank()) {
            errorMessage = "Missing token. Please log in again."
            return
        }

        viewModelScope.launch {
            loading = true
            errorMessage = null
            try {
                if (!isBackendDocumentId(module.documentId)) {
                    Log.w(TAG, "Skip /progress/complete for non-backend documentId='${module.documentId}'")
                    return@launch
                }
                RetrofitClient.instance.completeProgress(ProgressCompleteRequest(module.documentId, normalizedScore))
                refreshProgressTimeline()
            } catch (e: HttpException) {
                handleHttpError(e)
            } catch (_: IOException) {
                errorMessage = "Network failure. Please check your connection."
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to update progress."
            } finally {
                loading = false
            }
        }
    }

    fun markModuleStarted(documentId: String) {
        if (RetrofitClient.authToken.isNullOrBlank()) return
        if (!isBackendDocumentId(documentId)) {
            Log.w(TAG, "Skip /progress/init for non-backend documentId='$documentId'")
            return
        }

        viewModelScope.launch {
            try {
                RetrofitClient.instance.initProgress(ProgressInitRequest(documentId))
                initializedDocuments = initializedDocuments + documentId
            } catch (_: Exception) {
            }
        }
    }

    fun ensureLessonEnriched(lessonId: String) {
        if (lessonId.isBlank()) return
        if (lessonEnrichmentLoading.contains(lessonId)) return
        val module = currentModules.firstOrNull { it.moduleId == lessonId } ?: return

        val cachedEnrichment = lessonEnrichment[lessonId]
        if (cachedEnrichment != null) {
            if (pendingEnrichmentSyncModuleIds.contains(lessonId) && !RetrofitClient.authToken.isNullOrBlank()) {
                viewModelScope.launch {
                    persistLessonEnrichmentToBackend(module, cachedEnrichment)
                }
            }
            return
        }

        lessonEnrichmentLoading = lessonEnrichmentLoading + lessonId
        viewModelScope.launch {
            val fallback = fallbackLessonEnrichment(module)
            try {
                val enriched = generateLessonEnrichment(module) ?: fallback
                lessonEnrichment = lessonEnrichment + (lessonId to enriched)
                persistLessonEnrichmentToBackend(module, enriched)
            } catch (_: Exception) {
                lessonEnrichment = lessonEnrichment + (lessonId to fallback)
                persistLessonEnrichmentToBackend(module, fallback)
            } finally {
                lessonEnrichmentLoading = lessonEnrichmentLoading - lessonId
                persistStudyPlanState()
            }
        }
    }

    fun resolveBackendLessonId(moduleId: String): String? {
        if (moduleId.isBlank()) return null
        return backendLessonIdByModuleId[moduleId]
    }

    private val currentModules: List<StudyModule>
        get() = (studyPlanResponse ?: fallbackPlanResponse).modules

    private fun initializeRemoteProgress(modules: List<StudyModule>) {
        if (RetrofitClient.authToken.isNullOrBlank()) return

        viewModelScope.launch {
            loading = true
            errorMessage = null
            try {
                modules.forEach { module ->
                    if (!isBackendDocumentId(module.documentId)) {
                        Log.w(TAG, "Skip remote init for non-backend documentId='${module.documentId}' module='${module.moduleId}'")
                        return@forEach
                    }
                    if (module.documentId !in initializedDocuments) {
                        RetrofitClient.instance.initProgress(ProgressInitRequest(module.documentId))
                        initializedDocuments = initializedDocuments + module.documentId
                    }
                }
                timeline = RetrofitClient.instance.getProgressTimeline()
                loadStoredLessonEnrichmentFromBackend()
                persistStudyPlanState()
            } catch (e: HttpException) {
                handleHttpError(e)
            } catch (_: IOException) {
                errorMessage = "Network failure. Please check your connection."
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to initialize progress."
            } finally {
                loading = false
            }
        }
    }

    private suspend fun loadStoredLessonEnrichmentFromBackend() {
        val lessons = RetrofitClient.instance.getProgressLessons()
        if (lessons.isEmpty()) return

        val modules = currentModules
        val loadedEnrichment = mutableMapOf<String, LessonEnrichment>()
        val loadedIds = mutableMapOf<String, String>()

        lessons
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: "" }
            .forEach { backendLesson ->
                val module = matchModuleForProgressLesson(backendLesson, modules) ?: return@forEach
                if (!loadedIds.containsKey(module.moduleId)) {
                    loadedIds[module.moduleId] = backendLesson.id
                }
                if (!loadedEnrichment.containsKey(module.moduleId)) {
                    val enrichment = progressLessonToEnrichment(backendLesson, module)
                    if (enrichment != null) {
                        loadedEnrichment[module.moduleId] = enrichment
                    }
                }
            }

        if (loadedIds.isNotEmpty()) {
            backendLessonIdByModuleId = backendLessonIdByModuleId + loadedIds
        }
        if (loadedEnrichment.isNotEmpty()) {
            lessonEnrichment = lessonEnrichment + loadedEnrichment
            pendingEnrichmentSyncModuleIds = pendingEnrichmentSyncModuleIds - loadedEnrichment.keys
        }
    }

    private suspend fun persistLessonEnrichmentToBackend(module: StudyModule, enrichment: LessonEnrichment) {
        if (RetrofitClient.authToken.isNullOrBlank()) {
            pendingEnrichmentSyncModuleIds = pendingEnrichmentSyncModuleIds + module.moduleId
            persistStudyPlanState()
            return
        }
        if (!isBackendDocumentId(module.documentId)) {
            Log.w(TAG, "Skip lesson enrichment sync for non-backend documentId='${module.documentId}' module='${module.moduleId}'")
            pendingEnrichmentSyncModuleIds = pendingEnrichmentSyncModuleIds - module.moduleId
            persistStudyPlanState()
            return
        }

        try {
            val backendLessonId = ensureBackendLessonRecord(module, enrichment.theory) ?: return
            backendLessonIdByModuleId = backendLessonIdByModuleId + (module.moduleId to backendLessonId)

            val quizPayload = buildLessonQuizArray(enrichment.quizQuestions)
            if (quizPayload.size() == 0) {
                Log.w(TAG, "Skip lesson quiz save: quiz array is empty for lessonId=$backendLessonId moduleId=${module.moduleId}")
                return
            }
            val request = SaveLessonQuizRequest(quiz = quizPayload)
            Log.d(TAG, "POST /progress/lessons/$backendLessonId/quiz body=${Gson().toJson(request)}")
            RetrofitClient.instance.saveProgressLessonQuiz(
                backendLessonId,
                request
            )
            pendingEnrichmentSyncModuleIds = pendingEnrichmentSyncModuleIds - module.moduleId
        } catch (error: HttpException) {
            if (error.code() == 401) {
                RetrofitClient.authToken = null
            }
            pendingEnrichmentSyncModuleIds = pendingEnrichmentSyncModuleIds + module.moduleId
        } catch (_: IOException) {
            pendingEnrichmentSyncModuleIds = pendingEnrichmentSyncModuleIds + module.moduleId
        } catch (_: Exception) {
            pendingEnrichmentSyncModuleIds = pendingEnrichmentSyncModuleIds + module.moduleId
        } finally {
            persistStudyPlanState()
        }
    }

    private suspend fun ensureBackendLessonRecord(module: StudyModule, theory: String): String? {
        backendLessonIdByModuleId[module.moduleId]?.let { return it }

        val existing = runCatching { RetrofitClient.instance.getProgressLessons() }
            .getOrNull()
            ?.firstOrNull { candidate ->
                candidate.documentId == module.documentId || normalizeText(candidate.title) == normalizeText(module.title)
            }
        if (existing != null) return existing.id

        val created = RetrofitClient.instance.createProgressLesson(
            ProgressLessonRequest(
                documentId = module.documentId,
                title = module.title,
                contentText = theory
            )
        )
        return created.id
    }

    private fun matchModuleForProgressLesson(
        progressLesson: ProgressLesson,
        modules: List<StudyModule>
    ): StudyModule? {
        val byDocument = progressLesson.documentId
            ?.takeIf { it.isNotBlank() }
            ?.let { docId -> modules.firstOrNull { it.documentId == docId } }
        if (byDocument != null) return byDocument

        val normalizedTitle = normalizeText(progressLesson.title)
        if (normalizedTitle.isBlank()) return null
        return modules.firstOrNull { normalizeText(it.title) == normalizedTitle }
    }

    private fun progressLessonToEnrichment(progressLesson: ProgressLesson, module: StudyModule): LessonEnrichment? {
        val theory = progressLesson.contentText.trim().takeIf { it.isNotBlank() } ?: return null
        val parsedQuiz = parseProgressLessonQuiz(progressLesson.quizJson, module)
        if (parsedQuiz.size < 5) return null
        return LessonEnrichment(theory = theory, quizQuestions = parsedQuiz.take(5))
    }

    private fun parseProgressLessonQuiz(quizJson: JsonElement?, module: StudyModule): List<QuizQuestion> {
        if (quizJson == null || quizJson.isJsonNull) return emptyList()

        val questionsJson = when {
            quizJson.isJsonArray -> quizJson.asJsonArray
            quizJson.isJsonObject -> {
                val root = quizJson.asJsonObject
                root.jsonArrayOrNull("questions")
                    ?: root.jsonArrayOrNull("quiz")
            }
            else -> null
        } ?: return emptyList()

        return questionsJson.mapIndexedNotNull { index, element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapIndexedNotNull null
            val text = obj.getAsStringOrNull("question")
                ?: obj.getAsStringOrNull("text")
                ?: return@mapIndexedNotNull null

            val options = when {
                obj.jsonArrayOrNull("options") != null -> obj.jsonArrayOrNull("options")!!.toStringList().take(4)
                obj.get("options")?.isJsonObject == true -> {
                    val optionsObject = obj.getAsJsonObject("options")
                    listOf("A", "B", "C", "D").mapNotNull { key ->
                        optionsObject.get(key)?.asStringOrNull()?.trim()?.takeIf { it.isNotBlank() }
                    }.take(4)
                }
                else -> emptyList()
            }
            if (options.size != 4) return@mapIndexedNotNull null

            val correctAnswerIndex = obj.get("correctAnswerIndex")?.asIntOrNull()?.coerceIn(0, 3)
                ?: answerIndex(
                    obj.getAsStringOrNull("correctAnswer")
                        ?: obj.getAsStringOrNull("answer")
                        ?: "A",
                    options
                )

            QuizQuestion(
                id = obj.getAsStringOrNull("id") ?: "${module.moduleId}-stored-${index + 1}",
                question = text,
                options = options,
                correctAnswerIndex = correctAnswerIndex,
                hint = obj.getAsStringOrNull("hint") ?: "Choose the best answer.",
                explanation = obj.getAsStringOrNull("explanation") ?: ""
            )
        }.distinctBy { it.question.trim().lowercase() }
    }

    private fun seedLocalProgressFromPlan(plan: StudyPlanResponse) {
        timeline = plan.modules.map { module ->
            StudyProgressItem(
                documentId = module.documentId,
                fileName = module.title,
                status = module.status,
                score = if (module.status == ModuleStatus.COMPLETED) 100 else 0
            )
        }
    }

    private suspend fun generateLessonEnrichment(module: StudyModule): LessonEnrichment? {
        if (RetrofitClient.authToken.isNullOrBlank()) {
            return null
        }

        val hiddenPrompt = buildString {
            append("You are generating lesson support content for a mobile learning app. ")
            append("Return strict JSON only, no markdown, no extra text. ")
            append("Schema: {")
            append("\"theory\":\"...\",")
            append("\"quiz\":[{")
            append("\"id\":\"...\",\"text\":\"...\",\"options\":[\"...\",\"...\",\"...\",\"...\"],\"answer\":\"A\",\"explanation\":\"...\"}")
            append("]}")
            append(". Requirements: theory 180-260 words in English, practical and aligned to the objective. ")
            append("Create exactly 5 quiz questions. All 5 must be different in intent and wording. ")
            append("Question types: concept check, misconception check, scenario application, comparison, and best-practice choice. ")
            append("Each question has exactly 4 options. answer must be A/B/C/D and must match one option. ")
            append("Lesson title: ${module.title}. ")
            append("Objective: ${module.objective}. ")
            append("Difficulty: ${module.difficulty}. ")
            append("Estimated minutes: ${module.estimatedMinutes}.")
        }

        val raw = RetrofitClient.instance.aiAsk(AiAskRequest(hiddenPrompt)).answer
        return parseLessonEnrichment(raw, module)
    }

    private fun parseLessonEnrichment(raw: String, module: StudyModule): LessonEnrichment? {
        val cleaned = raw
            .replace("```json", "")
            .replace("```", "")
            .trim()
        val root = runCatching { JsonParser().parse(cleaned) }.getOrNull() as? JsonObject ?: return null
        val theory = root.getAsStringOrNull("theory")
            ?: root.getAsStringOrNull("content")
            ?: return null

        val quizArray = root.jsonArrayOrNull("quiz") ?: root.jsonArrayOrNull("questions") ?: return null
        val parsed = quizArray.mapIndexedNotNull { index, element ->
            val item = element.asJsonObjectOrNull() ?: return@mapIndexedNotNull null
            val text = item.getAsStringOrNull("text") ?: item.getAsStringOrNull("question") ?: return@mapIndexedNotNull null
            val options = item.jsonArrayOrNull("options")?.toStringList().orEmpty().take(4)
            if (options.size != 4) return@mapIndexedNotNull null
            val answerRaw = item.getAsStringOrNull("answer") ?: return@mapIndexedNotNull null
            val answerIndex = answerIndex(answerRaw, options)

            QuizQuestion(
                id = item.getAsStringOrNull("id") ?: "${module.moduleId}-ai-${index + 1}",
                question = text,
                options = options,
                correctAnswerIndex = answerIndex,
                hint = "Choose the best answer.",
                explanation = item.getAsStringOrNull("explanation") ?: ""
            )
        }

        if (parsed.size < 5) return null
        val distinctQuestions = parsed.distinctBy { it.question.trim().lowercase() }
        if (distinctQuestions.size < 5) return null

        return LessonEnrichment(
            theory = theory,
            quizQuestions = distinctQuestions.take(5)
        )
    }

    private fun fallbackLessonEnrichment(module: StudyModule): LessonEnrichment {
        val theory = buildString {
            append("${module.title} focuses on ${module.objective.lowercase()}. ")
            append("Start by defining the key terms, then connect each concept to a small example you can explain in your own words. ")
            append("When reviewing, separate what the concept is, why it matters, and when you should apply it. ")
            append("A strong approach is to practice short retrieval rounds: close your notes, write a quick explanation, and verify missing points. ")
            append("For this lesson, prioritize understanding over memorization. ")
            append("Use worked examples to test whether you can transfer the idea to a new situation, not just repeat definitions. ")
            append("Before finishing, summarize the topic in three bullet points and list one common mistake to avoid. ")
            append("This helps you build long-term recall and prepares you for the quiz questions.")
        }

        val title = module.title
        val objective = module.objective
        val questions = listOf(
            QuizQuestion(
                id = "${module.moduleId}-fallback-1",
                question = "$title: Which statement best captures the lesson objective?",
                options = listOf(
                    "Understand and apply $objective",
                    "Memorize examples without understanding the concept",
                    "Skip fundamentals and jump to advanced topics",
                    "Treat the lesson as unrelated to the module goals"
                ),
                correctAnswerIndex = 0,
                hint = "Focus on the objective.",
                explanation = "The best choice aligns directly with the stated objective."
            ),
            QuizQuestion(
                id = "${module.moduleId}-fallback-2",
                question = "$title: Which study behavior shows a misconception?",
                options = listOf(
                    "Explaining the concept in your own words",
                    "Checking why each step works",
                    "Ignoring core ideas and only memorizing answers",
                    "Reviewing mistakes after each attempt"
                ),
                correctAnswerIndex = 2,
                hint = "Look for shallow learning.",
                explanation = "Memorizing final answers without understanding leads to poor transfer."
            ),
            QuizQuestion(
                id = "${module.moduleId}-fallback-3",
                question = "$title: In a new problem, what is the best first step?",
                options = listOf(
                    "Guess an answer and move on quickly",
                    "Identify how the problem maps to $objective",
                    "Copy a random solved example",
                    "Skip analysis and wait for hints"
                ),
                correctAnswerIndex = 1,
                hint = "Connect the problem to the objective.",
                explanation = "Mapping the problem to the lesson objective helps choose a valid strategy."
            ),
            QuizQuestion(
                id = "${module.moduleId}-fallback-4",
                question = "$title: Which option best compares strong learning vs weak learning?",
                options = listOf(
                    "Strong: retrieval + explanation; Weak: passive rereading only",
                    "Strong: skip review; Weak: practice spaced repetition",
                    "Strong: avoid feedback; Weak: analyze mistakes",
                    "Strong: reduce understanding; Weak: improve transfer"
                ),
                correctAnswerIndex = 0,
                hint = "Choose the evidence-based study method.",
                explanation = "Active recall and explanation produce deeper understanding than passive rereading."
            ),
            QuizQuestion(
                id = "${module.moduleId}-fallback-5",
                question = "$title: Which practice choice is most effective before the quiz?",
                options = listOf(
                    "Do one random task without checking mistakes",
                    "Review only definitions and skip applications",
                    "Solve one applied question and explain why each option is right or wrong",
                    "Wait until the quiz to test understanding"
                ),
                correctAnswerIndex = 2,
                hint = "Use active practice with reasoning.",
                explanation = "Reasoning through options improves application skill and quiz performance."
            )
        )

        return LessonEnrichment(
            theory = theory,
            quizQuestions = questions
        )
    }

    private fun answerIndex(answerRaw: String, options: List<String>): Int {
        return when (answerRaw.trim().uppercase()) {
            "A" -> 0
            "B" -> 1
            "C" -> 2
            "D" -> 3
            else -> {
                val byText = options.indexOfFirst { it.equals(answerRaw.trim(), ignoreCase = true) }
                if (byText >= 0) byText else 0
            }
        }
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? = if (isJsonObject) asJsonObject else null
    private fun JsonElement.asStringOrNull(): String? = runCatching { asString }.getOrNull()
    private fun JsonElement.asIntOrNull(): Int? = runCatching { asInt }.getOrNull()
    private fun JsonObject.getAsStringOrNull(key: String): String? = get(key)?.asStringOrNull()
    private fun JsonObject.jsonArrayOrNull(key: String): JsonArray? = get(key)?.takeIf { it.isJsonArray }?.asJsonArray
    private fun JsonArray.toStringList(): List<String> = mapNotNull { it.asStringOrNull()?.trim()?.takeIf { value -> value.isNotBlank() } }
    private fun normalizeText(value: String): String = value.trim().lowercase()

    private fun isBackendDocumentId(documentId: String?): Boolean {
        val value = documentId?.trim().orEmpty()
        return value.isNotBlank() && UUID_REGEX.matches(value)
    }

    private fun buildLessonQuizArray(questions: List<QuizQuestion>): JsonArray {
        return JsonArray().apply {
            questions.forEach { question ->
                add(JsonObject().apply {
                    addProperty("question", question.question)
                    add("options", JsonObject().apply {
                        question.options.getOrNull(0)?.let { addProperty("A", it) }
                        question.options.getOrNull(1)?.let { addProperty("B", it) }
                        question.options.getOrNull(2)?.let { addProperty("C", it) }
                        question.options.getOrNull(3)?.let { addProperty("D", it) }
                    })
                    addProperty("correctAnswer", when (question.correctAnswerIndex) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        3 -> "D"
                        else -> "A"
                    })
                    addProperty("explanation", question.explanation)
                })
            }
        }
    }

    private fun upsertTimeline(
        documentId: String,
        fileName: String,
        score: Int
    ): List<StudyProgressItem> {
        val updated = timeline.toMutableList()
        val index = updated.indexOfFirst { it.documentId == documentId }
        val item = StudyProgressItem(documentId = documentId, fileName = fileName, status = ModuleStatus.COMPLETED, score = score)

        if (index >= 0) {
            updated[index] = item
        } else {
            updated.add(item)
        }

        return updated
    }

    private fun handleHttpError(error: HttpException) {
        if (error.code() == 401) {
            RetrofitClient.authToken = null
            initializedDocuments = emptySet()
            errorMessage = "Session expired. Please log in again."
        } else {
            errorMessage = error.message() ?: "Request failed with code ${error.code()}"
        }
    }

    private fun persistStudyPlanState() {
        LocalHistoryStore.saveStudyPlanState(
            rawJson = currentPlanJson,
            timeline = timeline,
            lessonEnrichment = lessonEnrichment.mapValues { (_, value) ->
                CachedLessonEnrichment(
                    theory = value.theory,
                    quizQuestions = value.quizQuestions
                )
            },
            backendLessonIdByModuleId = backendLessonIdByModuleId,
            pendingEnrichmentModuleIds = pendingEnrichmentSyncModuleIds.toList()
        )
    }
}