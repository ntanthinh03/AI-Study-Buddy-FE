@file:Suppress("unused")

package com.thinh.aistudybuddy.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.thinh.aistudybuddy.data.models.*
import java.io.File

private const val HISTORY_FILE_NAME = "study_buddy_history.json"
private const val HISTORY_SCHEMA_VERSION = 3

private val gson: Gson = GsonBuilder().create()

private object HistoryFileLock

private var historyFile: File? = null

private data class CachedAppHistory(
    val historyVersion: Int = HISTORY_SCHEMA_VERSION,
    val chatState: CachedChatState? = null,
    val quizSessions: List<CachedQuizSession> = emptyList(),
    val studyPlanState: CachedStudyPlanState? = null
)

data class CachedChatState(
    val activeConversationId: String = "",
    val currentChatType: String = "NEW_CHAT",
    val conversations: List<CachedConversation> = emptyList()
)

data class CachedConversation(
    val id: String,
    val title: String,
    val isQuiz: Boolean = false,
    val kind: String = ConversationKind.CHAT.name,
    val autoTitleApplied: Boolean = false,
    val documentId: String? = null,
    val chatMessages: List<CachedChatMessage> = emptyList()
)

data class CachedChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val attachmentName: String? = null,
    val showQuizButton: Boolean = false,
    val showStudyPlanButton: Boolean = false,
    val showFlashcardButton: Boolean = false,
    val showMindMapButton: Boolean = false,
    val documentId: String? = null,
    val planJson: String? = null,
    val artifactType: String? = null,
    val artifactJson: com.google.gson.JsonElement? = null,
    val createdAt: String = ""
)

data class CachedQuizSession(
    val sessionId: String,
    val title: String,
    val documentId: String? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val userAnswers: List<Int> = emptyList(),
    val submittedQuestions: List<Boolean> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val isNewRecord: Boolean = false,
    val completedAt: Long = System.currentTimeMillis()
)

data class CachedLessonEnrichment(
    val theory: String,
    val quizQuestions: List<QuizQuestion> = emptyList()
)

data class CachedStudyPlanState(
    val rawJson: String,
    val timeline: List<StudyProgressItem> = emptyList(),
    val lessonEnrichment: Map<String, CachedLessonEnrichment>? = null,
    val backendLessonIdByModuleId: Map<String, String>? = null,
    val pendingEnrichmentModuleIds: List<String>? = null,
    val lessonStatuses: Map<String, String>? = null,
    val lessonScores: Map<String, Int>? = null
)

object LocalHistoryStore {
    fun initialize(context: Context) {
        synchronized(HistoryFileLock) {
            if (historyFile == null) {
                historyFile = File(context.filesDir, HISTORY_FILE_NAME)
            }
        }
    }

    fun loadChatState(): CachedChatState? = readHistory().chatState

    fun saveChatState(state: CachedChatState) {
        writeHistory { copy(chatState = state) }
    }

    fun loadLatestQuizSession(): CachedQuizSession? = readHistory().quizSessions.lastOrNull()

    @Suppress("unused")
    fun loadQuizSessions(): List<CachedQuizSession> = readHistory().quizSessions

    fun saveQuizSession(session: CachedQuizSession) {
        writeHistory {
            val updated = quizSessions.toMutableList()
            val index = updated.indexOfFirst { it.sessionId == session.sessionId }
            if (index >= 0) updated[index] = session else updated.add(session)
            copy(quizSessions = updated)
        }
    }

    fun loadStudyPlanState(): CachedStudyPlanState? = readHistory().studyPlanState

    fun saveStudyPlanState(
        rawJson: String,
        timeline: List<StudyProgressItem>,
        lessonEnrichment: Map<String, CachedLessonEnrichment> = emptyMap(),
        backendLessonIdByModuleId: Map<String, String> = emptyMap(),
        pendingEnrichmentModuleIds: List<String> = emptyList(),
        lessonStatuses: Map<String, String> = emptyMap(),
        lessonScores: Map<String, Int> = emptyMap()
    ) {
        writeHistory {
            copy(
                studyPlanState = CachedStudyPlanState(
                    rawJson = rawJson,
                    timeline = timeline,
                    lessonEnrichment = lessonEnrichment,
                    backendLessonIdByModuleId = backendLessonIdByModuleId,
                    pendingEnrichmentModuleIds = pendingEnrichmentModuleIds,
                    lessonStatuses = lessonStatuses,
                    lessonScores = lessonScores
                )
            )
        }
    }

    fun clearAll() {
        synchronized(HistoryFileLock) {
            historyFile?.delete()
        }
    }

    fun cachedConversationsToRuntime(state: CachedChatState): Pair<List<Conversation>, String> {
        val conversations = state.conversations.map { cached ->
            Conversation(
                id = cached.id,
                title = cached.title,
                isQuiz = cached.isQuiz,
                kind = runCatching { ConversationKind.valueOf(cached.kind) }.getOrDefault(
                    if (cached.isQuiz) ConversationKind.QUIZ else ConversationKind.CHAT
                ),
                autoTitleApplied = cached.autoTitleApplied,
                documentId = cached.documentId,
                chatMessages = cached.chatMessages.map { it.toRuntime() }.toMutableList()
            )
        }
        return conversations to state.activeConversationId
    }

    fun runtimeChatState(
        conversations: List<Conversation>,
        activeConversationId: String,
        currentChatType: String
    ): CachedChatState {
        return CachedChatState(
            activeConversationId = activeConversationId,
            currentChatType = currentChatType,
            conversations = conversations.map { it.toCached() }
        )
    }

    fun runtimeQuizSession(
        sessionId: String,
        title: String,
        documentId: String?,
        questions: List<QuizQuestion>,
        userAnswers: List<Int>,
        submittedQuestions: List<Boolean>,
        currentQuestionIndex: Int,
        score: Int,
        isNewRecord: Boolean,
        completedAt: Long = System.currentTimeMillis()
    ): CachedQuizSession {
        return CachedQuizSession(
            sessionId = sessionId,
            title = title,
            documentId = documentId,
            questions = questions,
            userAnswers = userAnswers,
            submittedQuestions = submittedQuestions,
            currentQuestionIndex = currentQuestionIndex,
            score = score,
            isNewRecord = isNewRecord,
            completedAt = completedAt
        )
    }

    private fun readHistory(): CachedAppHistory {
        val file = synchronized(HistoryFileLock) { historyFile } ?: return CachedAppHistory()
        if (!file.exists()) return CachedAppHistory()

        val history = runCatching {
            file.readText().takeIf { it.isNotBlank() }?.let { gson.fromJson(it, CachedAppHistory::class.java) }
        }.getOrNull() ?: return CachedAppHistory()

        return if (history.historyVersion != HISTORY_SCHEMA_VERSION) {
            synchronized(HistoryFileLock) {
                file.delete()
            }
            CachedAppHistory()
        } else {
            history
        }
    }

    private fun writeHistory(update: CachedAppHistory.() -> CachedAppHistory) {
        synchronized(HistoryFileLock) {
            val file = historyFile ?: return
            val current = readHistory()
            val updated = current.update().copy(historyVersion = HISTORY_SCHEMA_VERSION)
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(updated))
        }
    }

    private fun Conversation.toCached(): CachedConversation = CachedConversation(
        id = id,
        title = title,
        isQuiz = isQuiz,
        kind = kind.name,
        autoTitleApplied = autoTitleApplied,
        documentId = documentId,
        chatMessages = chatMessages.map { it.toCached() }
    )

    private fun ChatMessage.toCached(): CachedChatMessage = CachedChatMessage(
        id = id,
        text = text,
        isUser = isUser,
        attachmentName = attachmentName,
        showQuizButton = showQuizButton,
        showStudyPlanButton = showStudyPlanButton,
        showFlashcardButton = showFlashcardButton,
        showMindMapButton = showMindMapButton,
        documentId = documentId,
        planJson = planJson,
        artifactType = artifactType,
        artifactJson = artifactJson,
        createdAt = createdAt
    )

    private fun CachedChatMessage.toRuntime(): ChatMessage = ChatMessage(
        id = id,
        text = text,
        isUser = isUser,
        attachmentName = attachmentName,
        showQuizButton = showQuizButton,
        showStudyPlanButton = showStudyPlanButton,
        showFlashcardButton = showFlashcardButton,
        showMindMapButton = showMindMapButton,
        documentId = documentId,
        planJson = planJson,
        artifactType = artifactType,
        artifactJson = artifactJson,
        createdAt = createdAt
    )
}

