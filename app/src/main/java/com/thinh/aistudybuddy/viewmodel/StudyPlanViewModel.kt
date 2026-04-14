@file:Suppress("unused")

package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.local.LocalHistoryStore
import com.thinh.aistudybuddy.data.model.DEFAULT_STUDY_PLAN_JSON
import com.thinh.aistudybuddy.data.model.ModuleStatus
import com.thinh.aistudybuddy.data.model.ProgressCompleteRequest
import com.thinh.aistudybuddy.data.model.ProgressInitRequest
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
    private val fallbackPlanResponse = checkNotNull(StudyPlanJsonParser.parse(DEFAULT_STUDY_PLAN_JSON))
    private var currentPlanJson by mutableStateOf(DEFAULT_STUDY_PLAN_JSON)

    private var studyPlanResponse by mutableStateOf<StudyPlanResponse?>(fallbackPlanResponse)
    private var initializedDocuments by mutableStateOf<Set<String>>(emptySet())
    var timeline by mutableStateOf<List<StudyProgressItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val activePlan: StudyPlan
        get() = (studyPlanResponse ?: fallbackPlanResponse).toLegacyPlan(timeline)

    init {
        val cached = LocalHistoryStore.loadStudyPlanState()
        if (cached != null) {
            val cachedPlan = StudyPlanJsonParser.parse(cached.rawJson)
            if (cachedPlan != null) {
                currentPlanJson = cached.rawJson
                studyPlanResponse = cachedPlan
                timeline = cached.timeline
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

        viewModelScope.launch {
            try {
                RetrofitClient.instance.initProgress(ProgressInitRequest(documentId))
                initializedDocuments = initializedDocuments + documentId
            } catch (_: Exception) {
            }
        }
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
                    if (module.documentId !in initializedDocuments) {
                        RetrofitClient.instance.initProgress(ProgressInitRequest(module.documentId))
                        initializedDocuments = initializedDocuments + module.documentId
                    }
                }
                timeline = RetrofitClient.instance.getProgressTimeline()
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
        LocalHistoryStore.saveStudyPlanState(currentPlanJson, timeline)
    }
}