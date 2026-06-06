package com.thinh.aistudybuddy.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.thinh.aistudybuddy.services.AiAskService
import com.thinh.aistudybuddy.data.models.*
import com.thinh.aistudybuddy.data.models.BackendQuizQuestion as ApiQuizQuestion

import com.thinh.aistudybuddy.data.local.*
import com.thinh.aistudybuddy.services.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.util.UUID

enum class ChatScreenType {
    NEW_CHAT, CONTINUING_CHAT
}

@Suppress("unused")
class ChatViewModel : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val TURN_LOG_TAG = "ChatTurn"
        private const val DOCUMENT_STATUS_POLL_INTERVAL_MS = 2000L
        private const val DOCUMENT_STATUS_MAX_POLLS = 180
    }

    private class DocumentProcessingTimeoutException(message: String) : IllegalStateException(message)

    private val _conversations = mutableStateListOf<Conversation>()
    val conversations: List<Conversation> get() = _conversations
    private val turnMutex = Mutex()

    var activeConversationId by mutableStateOf("")
    var activeDocumentId by mutableStateOf<String?>(null)
    var searchQuery by mutableStateOf("")
    var isTyping by mutableStateOf(false)
    var isUploading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var sessionExpired by mutableStateOf(false)
        private set
    var onQuizGenerated: ((List<QuizQuestion>) -> Unit)? = null
    var onPlanGenerated: ((String) -> Unit)? = null
    var onMindMapGenerated: ((String) -> Unit)? = null
    var pendingPdfName by mutableStateOf<String?>(null)
        private set
    private var pendingPdfUri: Uri? = null

    var pendingImageName by mutableStateOf<String?>(null)
        private set
    private var pendingImageUri: Uri? = null

    var uploadStatusLabel by mutableStateOf<String?>(null)
        private set
    var uploadTerminalStatus by mutableStateOf<String?>(null)
        private set

    private var currentAskSessionId by mutableStateOf<String?>(null)
    private var currentAskDocumentId by mutableStateOf<String?>(null)
    var aiAskState by mutableStateOf<AskState>(AskState.IDLE)
        private set

    private var _currentChatType = mutableStateOf(ChatScreenType.NEW_CHAT)
    val currentChatType: ChatScreenType get() = _currentChatType.value
    private val quizKeywords = listOf("quiz", "multiple choice", "mcq", "test me", "practice", "practice questions")
    private val planKeywords = listOf("study plan", "plan", "roadmap", "learning path", "course plan", "learning plan", "progress")

    private val _suggestions = mutableStateListOf<Suggestion>()
    val suggestions: List<Suggestion> get() = _suggestions

     private val _banner = mutableStateOf<Banner?>(null)
     val banner: Banner? get() = _banner.value

     var successMessage by mutableStateOf<String?>(null)
         private set
     var openedFromFreshLogin by mutableStateOf(false)
         private set
     var debugAccountKeyHashForLogs by mutableStateOf<String?>(null)
         private set
     
     private val conversationStudyPlansById = mutableMapOf<String, List<ConversationStudyPlanItem>>()

     init {
        seedSuggestions()
        restoreChatHistory()
    }

    private fun seedSuggestions() {
        if (_suggestions.isNotEmpty()) return
        _suggestions.addAll(listOf(
            Suggestion("Create a Quiz", "on Python Programming"),
            Suggestion("Summarize PDF", "Research paper summary"),
            Suggestion("Explain Concept", "How React Native works"),
            Suggestion("Study Plan", "for Data Structures")
        ))
    }

    private fun newTurnId(prefix: String = "turn"): String {
        return "$prefix-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
    }

    private fun logTurn(turnId: String, phase: String, conversationId: String? = null, detail: String = "") {
        val suffix = if (detail.isBlank()) "" else " detail=$detail"
        Log.d(TURN_LOG_TAG, "turnId=$turnId phase=$phase conversationId=${conversationId.orEmpty()}$suffix")
    }

    private fun setupMockData() {
        val quizId = UUID.randomUUID().toString()
        val quizConv = Conversation(quizId, "Java Basics Quiz", isQuiz = true, kind = ConversationKind.QUIZ, autoTitleApplied = true).apply {
            chatMessages.addAll(listOf(
                ChatMessage(id = UUID.randomUUID().toString(), text = "Can you create a quiz about Java fundamentals?", isUser = true),
                ChatMessage(id = UUID.randomUUID().toString(), text = "I've generated a 10-question quiz on Java concepts. You can start testing your knowledge now!", isUser = false, showQuizButton = true)
            ))
        }
        val planId = UUID.randomUUID().toString()
        val planConv = Conversation(planId, "Study Plan: Data Structures", kind = ConversationKind.PLAN, autoTitleApplied = true).apply {
            chatMessages.addAll(listOf(
                ChatMessage(id = UUID.randomUUID().toString(), text = "Create a study plan for Data Structures", isUser = true),
                ChatMessage(id = UUID.randomUUID().toString(), text = "I've designed a personalized roadmap for Data Structures. Tap 'Check Plan' to start learning!", isUser = false, showStudyPlanButton = true)
            ))
        }
        _conversations.addAll(listOf(quizConv, planConv))
    }

    private fun restoreChatHistory() {
        val cachedState = LocalHistoryStore.loadChatState()
        if (cachedState == null || cachedState.conversations.isEmpty()) {
            _conversations.clear()
            activeConversationId = ""
            activeDocumentId = null
            _currentChatType.value = ChatScreenType.NEW_CHAT
            return
        }

        val (conversations, activeId) = LocalHistoryStore.cachedConversationsToRuntime(cachedState)
        _conversations.clear()
        _conversations.addAll(conversations)
        activeConversationId = activeId.ifBlank { conversations.firstOrNull()?.id.orEmpty() }
        activeDocumentId = _conversations.find { it.id == activeConversationId }?.documentId
        _currentChatType.value = runCatching { ChatScreenType.valueOf(cachedState.currentChatType) }
            .getOrDefault(if (activeConversationId.isBlank()) ChatScreenType.NEW_CHAT else ChatScreenType.CONTINUING_CHAT)
    }

    fun startNewChat() {
        clearAskSession()
        _currentChatType.value = ChatScreenType.NEW_CHAT
        activeConversationId = ""
        activeDocumentId = null
        isTyping = false
        persistChatState()
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val turnId = newTurnId()
        viewModelScope.launch {
            turnMutex.withLock {
                val documentId = activeDocumentId
                    ?: _conversations.find { it.id == activeConversationId }?.documentId
                        ?.also { activeDocumentId = it }
                if (documentId != null) {
                    logTurn(turnId, "user_sent_document", activeConversationId, "documentId=$documentId")
                    sendPersistedDocumentMessage(documentId, text, turnId)
                    return@withLock
                }

                if (_currentChatType.value == ChatScreenType.NEW_CHAT) {
                    val newConvId = UUID.randomUUID().toString()
                    val newConv = Conversation(newConvId, "", autoTitleApplied = false).apply {
                        chatMessages.add(ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = true))
                    }
                    _conversations.add(0, newConv)
                    activeConversationId = newConvId
                    _currentChatType.value = ChatScreenType.CONTINUING_CHAT
                    persistChatState()
                    logTurn(turnId, "user_sent_general", newConvId)
                    generateAiReply(newConvId, text, turnId)
                } else {
                    val index = _conversations.indexOfFirst { it.id == activeConversationId }
                    if (index != -1) {
                        val updatedConv = _conversations[index]
                        updatedConv.chatMessages.add(ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = true))
                        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
                        persistChatState()
                        logTurn(turnId, "user_sent_general", activeConversationId)
                        generateAiReply(activeConversationId, text, turnId)
                    }
                }
            }
        }
    }

    fun setPendingPdf(uri: Uri, fileName: String) {
        pendingPdfUri = uri
        pendingPdfName = fileName
    }

    fun clearPendingPdf() {
        pendingPdfUri = null
        pendingPdfName = null
    }

    fun setPendingImage(uri: Uri, fileName: String) {
        pendingImageUri = uri
        pendingImageName = fileName
    }

    fun clearPendingImage() {
        pendingImageUri = null
        pendingImageName = null
    }

    fun sendMessageWithPendingImage(context: Context, text: String) {
        val imageUri = pendingImageUri ?: return
        val questionText = text.ifBlank { "Explain this image" }
        if (!RetrofitClient.hasUsableAuthToken()) {
            RetrofitClient.logAuthTokenDiagnostics("Blocked protected ask-image call: token missing or expired")
            errorMessage = "Missing or expired token. Please log in again."
            sessionExpired = true
            return  // Don't clear the attachment — user keeps it and can retry after login
        }
        clearPendingImage()
        sendImageQuestion(context, imageUri, questionText)
    }


    fun sendMessageWithPendingPdf(context: Context, text: String) {
        val pdfUri = pendingPdfUri ?: return
        val pdfName = pendingPdfName ?: "attachment.pdf"
        val turnId = newTurnId("pdf")
        if (text.isBlank()) {
            errorMessage = "Please add a message with your PDF."
            return
        }
        if (!RetrofitClient.hasUsableAuthToken()) {
            RetrofitClient.logAuthTokenDiagnostics("Blocked protected upload call: token missing or expired")
            errorMessage = "Missing or expired token. Please log in again."
            sessionExpired = true
            return
        }

        clearPendingPdf()

        viewModelScope.launch {
            turnMutex.withLock {
                isUploading = true
                isTyping = false
                uploadStatusLabel = "Uploading document"
                uploadTerminalStatus = null
                errorMessage = null

                val conversationId = if (_currentChatType.value == ChatScreenType.NEW_CHAT) {
                    val newConvId = UUID.randomUUID().toString()
                    _conversations.add(0, Conversation(newConvId, "", autoTitleApplied = false))
                    activeConversationId = newConvId
                    _currentChatType.value = ChatScreenType.CONTINUING_CHAT
                    newConvId
                } else {
                    activeConversationId
                }

                appendUserMessage(
                    convId = conversationId,
                    text = text,
                    attachmentName = pdfName
                )
                persistChatState()
                logTurn(turnId, "user_sent_pdf", conversationId, "file=$pdfName")

                val processingMessageId = appendProcessingMessage(conversationId, "Reading document...")
                persistChatState()

                var currentId = conversationId
                try {
                    val uploaded = uploadDocument(context, pdfUri, pdfName)

                    replaceMessage(conversationId, processingMessageId, "Summarizing...")
                    persistChatState()

                    val finalizedDocument = waitForUploadPipeline(uploaded)
                    activeDocumentId = finalizedDocument.id

                    val conversationIndex = _conversations.indexOfFirst { it.id == conversationId }
                    if (conversationIndex != -1) {
                        val updatedConv = _conversations[conversationIndex].copy(
                            documentId = finalizedDocument.id,
                            title = finalizedDocument.fileName.ifBlank { _conversations[conversationIndex].title }
                        )
                        _conversations[conversationIndex] = updatedConv
                    }

                    val index = _conversations.indexOfFirst { it.id == conversationId }
                    if (index != -1) {
                        val updatedConv = _conversations[index]
                        updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
                    }
                    persistChatState()

                    val response = RetrofitClient.instance.sendQuestion(finalizedDocument.id, DocumentChatRequest(text, conversationId))
                    val resolvedConversationId = if (!response.conversationId.isNullOrBlank()) {
                        rebindConversationId(conversationId, response.conversationId)
                    } else {
                        conversationId
                    }
                    currentId = resolvedConversationId

                    appendAiMessage(
                        convId = resolvedConversationId,
                        text = response.answer,
                        messageId = response.messageId,
                        documentId = finalizedDocument.id,
                        artifactType = response.artifactType,
                        artifactJson = response.artifactData
                    )
                    
                    if (response.artifactType == "QUIZ" && response.artifactData != null && !response.artifactData.isJsonNull) {
                        val questions = parseQuestionsFromJson(response.artifactData)
                        onQuizGenerated?.invoke(questions)
                    } else if (response.artifactType == "STUDY_PLAN" && response.artifactData != null && !response.artifactData.isJsonNull) {
                        onPlanGenerated?.invoke(response.artifactData.toString())
                    }

                    clearPendingPdf()
                    persistChatState()
                    logTurn(turnId, "ai_reply_appended", resolvedConversationId, "source=documents_chat")
                } catch (e: HttpException) {
                    val index = _conversations.indexOfFirst { it.id == currentId }
                    if (index != -1) {
                        val updatedConv = _conversations[index]
                        updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
                    }
                    uploadTerminalStatus = "FAILED"
                    handleHttpError(e)
                    logTurn(turnId, "turn_http_error", currentId, "code=${e.code()}")
                } catch (e: DocumentProcessingTimeoutException) {
                    val index = _conversations.indexOfFirst { it.id == currentId }
                    if (index != -1) {
                        val updatedConv = _conversations[index]
                        updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
                    }
                    uploadTerminalStatus = "SKIPPED"
                    errorMessage = e.message
                    logTurn(turnId, "turn_timeout", currentId, "stage=document_pipeline")
                } catch (e: Exception) {
                    val index = _conversations.indexOfFirst { it.id == currentId }
                    if (index != -1) {
                        val updatedConv = _conversations[index]
                        updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
                    }
                    uploadTerminalStatus = "FAILED"
                    errorMessage = e.localizedMessage ?: "Failed to send PDF with message."
                    logTurn(turnId, "turn_error", currentId, e.javaClass.simpleName)
                } finally {
                    uploadStatusLabel = null
                    isTyping = false
                    isUploading = false
                }
            }
        }
    }

    fun attachDocument(context: Context, uri: Uri, displayName: String? = null) {
        viewModelScope.launch {
            isUploading = true
            uploadStatusLabel = "Uploading document"
            uploadTerminalStatus = null
            errorMessage = null
            try {
                if (RetrofitClient.authToken.isNullOrBlank()) {
                    RetrofitClient.logAuthTokenDiagnostics("Blocked protected documents/upload call in attachDocument")
                    errorMessage = "Missing token. Please log in again."
                    sessionExpired = true
                    return@launch
                } else if (!RetrofitClient.hasUsableAuthToken()) {
                    RetrofitClient.logAuthTokenDiagnostics("Blocked protected documents/upload call in attachDocument")
                    errorMessage = "Token expired. Please log in again."
                    sessionExpired = true
                    return@launch
                } else {
                    val contentResolver = context.contentResolver
                    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                    val isImage = mimeType.startsWith("image/")
                    
                    val uploaded = if (isImage) {
                        uploadDocumentAsRag(context, uri, displayName)
                    } else {
                        uploadDocument(context, uri, displayName)
                    }
                    val finalizedDocument = waitForUploadPipeline(uploaded)
                    val conversation = ensureConversationForDocument(finalizedDocument)
                    activeConversationId = conversation.id
                    activeDocumentId = finalizedDocument.id
                    _currentChatType.value = ChatScreenType.CONTINUING_CHAT
                    loadDocumentHistory(finalizedDocument.id)
                    persistChatState()
                }
            } catch (e: HttpException) {
                uploadTerminalStatus = "FAILED"
                handleHttpError(e)
            } catch (e: DocumentProcessingTimeoutException) {
                uploadTerminalStatus = "SKIPPED"
                errorMessage = e.message
            } catch (e: Exception) {
                uploadTerminalStatus = "FAILED"
                errorMessage = e.localizedMessage ?: "Failed to upload document."
            } finally {
                uploadStatusLabel = null
                isUploading = false
            }
        }
    }

    private suspend fun uploadDocument(context: Context, uri: Uri, displayName: String?): Document =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = displayName ?: queryDisplayName(contentResolver, uri) ?: "upload.${guessExtension(mimeType)}"

            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Could not read selected file.")

            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", fileName, body)
            RetrofitClient.instance.uploadDocument(part)
        }

    private suspend fun uploadDocumentAsRag(context: Context, uri: Uri, displayName: String?): Document =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = displayName ?: queryDisplayName(contentResolver, uri) ?: "upload.${guessExtension(mimeType)}"
            
            val isImageFile = mimeType.startsWith("image/") || 
                fileName.endsWith(".png", ignoreCase = true) ||
                fileName.endsWith(".jpg", ignoreCase = true) ||
                fileName.endsWith(".jpeg", ignoreCase = true) ||
                fileName.endsWith(".webp", ignoreCase = true) ||
                fileName.endsWith(".gif", ignoreCase = true)
            
            if (!isImageFile) {
                throw IllegalArgumentException("Only image files (PNG, JPG, JPEG, WebP, GIF) are supported for image upload.")
            }
            
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Could not read selected file.")

            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", fileName, body)
            
            val response = RetrofitClient.instance.uploadRagImage(part)
            if (!response.success) {
                throw Exception("Server rejected image upload: ${response.status}")
            }
            
            val documentId = response.documentId
                ?: throw IllegalStateException("Upload response missing documentId.")
            
            Document(
                id = documentId,
                fileName = fileName,
                summary = null,
                status = "PROCESSING",
                summaryStatus = "PROCESSING",
                ragStatus = "PROCESSING"
            )
        }

    private suspend fun uploadToRag(context: Context, uri: Uri, displayName: String?) =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = displayName ?: queryDisplayName(contentResolver, uri) ?: "upload.${guessExtension(mimeType)}"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Could not read selected file.")

            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", fileName, body)
            val response = when {
                mimeType.startsWith("image/") -> RetrofitClient.instance.uploadRagImage(part)
                mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true) -> RetrofitClient.instance.uploadRagPdf(part)
                else -> throw IllegalArgumentException("Unsupported file type for RAG upload. Use a PDF or image.")
            }
            errorMessage = "RAG upload complete: ${response.chunks ?: 0} chunks."
        }

    private suspend fun waitForUploadPipeline(uploadedDocument: Document): Document =
        withContext(Dispatchers.IO) {
            val documentId = uploadedDocument.id
            var latest: Document = uploadedDocument
            var latestStatus: String? = null
            repeat(DOCUMENT_STATUS_MAX_POLLS) {
                val statusResponse = runCatching {
                    RetrofitClient.instance.getDocumentStatus(documentId)
                }.getOrNull()
                val doc = runCatching {
                    RetrofitClient.instance.getDocuments().firstOrNull { it.id == documentId }
                }.getOrNull()

                if (doc != null) {
                    latest = doc
                }

                val summaryState = latest.summaryStateNormalized()
                val ragState = latest.ragStateNormalized()
                val statusState = statusResponse?.status?.trim()?.uppercase().orEmpty()
                latestStatus = statusState.ifBlank { latestStatus }

                uploadStatusLabel = when {
                    summaryState == "COMPLETED" && (ragState == "PENDING" || ragState == "PROCESSING") ->
                        "Summary ready. Knowledge indexing continues in background..."
                    statusState == "PROCESSING" || summaryState == "PROCESSING" -> "Generating summary..."
                    ragState == "PROCESSING" -> "Ingesting knowledge..."
                    else -> null
                }

                if (summaryState == "COMPLETED" || statusState == "COMPLETED") {
                    uploadTerminalStatus = if (ragState == "FAILED") "SKIPPED" else "COMPLETED"
                    if (ragState == "FAILED") {
                        errorMessage = "Summary is ready. Knowledge indexing failed, but you can still chat with this file."
                    }
                    
                    launch {
                        try {
                            RetrofitClient.instance.generateBatch(documentId)
                            Log.d(TAG, "Batch generation triggered for $documentId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to trigger batch generation", e)
                        }
                    }
                    
                    return@withContext latest
                }

                if (statusState == "FAILED" || summaryState == "FAILED") {
                    uploadTerminalStatus = "FAILED"
                    throw IllegalStateException("Document processing failed. Please try again.")
                }

                delay(DOCUMENT_STATUS_POLL_INTERVAL_MS)
            }

            throw DocumentProcessingTimeoutException(
                "Summary is still processing. Please wait a bit and send again. summaryStatus=${latest.summaryStatus ?: latest.status}, ragStatus=${latest.ragStatus ?: "PENDING"}, status=${latestStatus ?: "UNKNOWN"}."
            )
        }

    private fun Document.summaryStateNormalized(): String {
        return (summaryStatus ?: status)
            .trim()
            .uppercase()
            .let { raw ->
                when (raw) {
                    "SUMMARIZING" -> "PROCESSING"
                    else -> raw
                }
            }
    }

    private fun Document.ragStateNormalized(): String {
        return ragStatus
            ?.trim()
            ?.uppercase()
            ?: when (status.trim().uppercase()) {
                "COMPLETED" -> "COMPLETED"
                "FAILED" -> "FAILED"
                else -> "PENDING"
            }
    }

    private fun String.isSummaryTerminal(): Boolean = this == "COMPLETED" || this == "FAILED"
    private fun String.isRagTerminal(): Boolean = this == "COMPLETED" || this == "FAILED" || this == "SKIPPED"

    private fun loadDocumentHistory(documentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = RetrofitClient.instance.getChatHistory(documentId)
                withContext(Dispatchers.Main) {
                    val index = _conversations.indexOfFirst { it.documentId == documentId }
                    if (index != -1) {
                        val mappedHistory = history.toUiMessages(_conversations[index].id)
                        val inferredKind = when {
                            mappedHistory.any { it.showStudyPlanButton } -> ConversationKind.PLAN
                            mappedHistory.any { it.showQuizButton } -> ConversationKind.QUIZ
                            else -> ConversationKind.CHAT
                        }
                        val updated = _conversations[index].copy(
                            kind = inferredKind,
                            isQuiz = inferredKind == ConversationKind.QUIZ,
                            chatMessages = mappedHistory.toMutableList()
                        )
                        _conversations[index] = updated
                        persistChatState()
                    }
                }
            } catch (e: Exception) {
                if (e is HttpException) handleHttpError(e)
            }
        }
    }

    private suspend fun sendPersistedDocumentMessage(documentId: String, text: String, turnId: String) {
        ensureConversationForDocument(documentId)
        val index = _conversations.indexOfFirst { conversation -> conversation.documentId == documentId }
        if (index == -1) return

        val convId = _conversations[index].id
        appendUserMessage(convId, text)
        persistChatState()
        isTyping = true

        var currentId = convId
        try {
            logTurn(turnId, "persist_document_request", convId)
            val response = RetrofitClient.instance.sendQuestion(documentId, DocumentChatRequest(text, convId))
            val resolvedConversationId = if (!response.conversationId.isNullOrBlank()) {
                rebindConversationId(convId, response.conversationId)
            } else {
                convId
            }
            currentId = resolvedConversationId

            appendAiMessage(
                convId = resolvedConversationId,
                text = response.answer,
                messageId = response.messageId,
                documentId = documentId,
                artifactType = response.artifactType,
                artifactJson = response.artifactData
            )

            if (response.artifactType == "QUIZ" && response.artifactData != null && !response.artifactData.isJsonNull) {
                val questions = parseQuestionsFromJson(response.artifactData)
                onQuizGenerated?.invoke(questions)
            } else if (response.artifactType == "STUDY_PLAN" && response.artifactData != null && !response.artifactData.isJsonNull) {
                onPlanGenerated?.invoke(response.artifactData.toString())
            }

            persistChatState()
            logTurn(turnId, "ai_reply_appended", resolvedConversationId, "source=documents_chat")
            maybeAutoRenameConversation(resolvedConversationId, text, response.answer)
        } catch (e: HttpException) {
            handleHttpError(e)
            logTurn(turnId, "turn_http_error", currentId, "code=${e.code()}")
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to send message."
            logTurn(turnId, "turn_error", currentId, e.javaClass.simpleName)
        } finally {
            isTyping = false
        }
    }

    private suspend fun persistIntentTurnForDocument(documentId: String, userMessage: String): ChatResponse? {
        return try {
            val conv = _conversations.firstOrNull { it.documentId == documentId }
            RetrofitClient.instance.sendQuestion(documentId, DocumentChatRequest(userMessage, conv?.id))
        } catch (e: HttpException) {
            handleHttpError(e)
            null
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to persist message."
            null
        }
    }

    private suspend fun persistIntentTurnForGeneralChat(convId: String, userMessage: String): ChatAskResponse? {
        return try {
            val currentConversation = _conversations.firstOrNull { it.id == convId }
            val requestTitle = currentConversation
                ?.title
                ?.takeIf { it.isNotBlank() }
                ?: userMessage.trim().take(48).takeIf { it.isNotBlank() }
            RetrofitClient.instance.askAI(
                AskAiRequest(
                    message = userMessage,
                    conversationId = convId,
                    title = requestTitle
                )
            )
        } catch (e: HttpException) {
            handleHttpError(e, forceLogoutOn401 = false)
            null
        } catch (_: SocketTimeoutException) {
            errorMessage = "The AI is taking longer than expected. Please try again."
            null
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to persist message."
            null
        }
    }

    private fun ensureConversationForDocument(document: Document): Conversation {
        val existingIndex = _conversations.indexOfFirst { it.documentId == document.id }
        if (existingIndex != -1) return _conversations[existingIndex]

        val created = Conversation(
            id = UUID.randomUUID().toString(),
            title = document.fileName.ifBlank { "Document Chat" },
            autoTitleApplied = false,
            documentId = document.id
        )
        _conversations.add(0, created)
        return created
    }

    private fun ensureConversationForDocument(documentId: String): Conversation {
        val existingIndex = _conversations.indexOfFirst { it.documentId == documentId }
        if (existingIndex != -1) return _conversations[existingIndex]

        val created = Conversation(
            id = UUID.randomUUID().toString(),
            title = "Document Chat",
            autoTitleApplied = false,
            documentId = documentId
        )
        _conversations.add(0, created)
        return created
    }

    private fun appendUserMessage(
        convId: String,
        text: String,
        attachmentName: String? = null,
        imageBase64: String? = null,
        imageMimeType: String? = null,
        imageOriginalName: String? = null
    ) {
        val index = _conversations.indexOfFirst { it.id == convId }
        if (index == -1) return
        val updatedConv = _conversations[index]
        updatedConv.chatMessages.add(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = text,
                isUser = true,
                attachmentName = attachmentName,
                imageBase64 = imageBase64,
                imageMimeType = imageMimeType,
                imageOriginalName = imageOriginalName,
                createdAt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            )
        )
        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
    }

    private fun appendAiMessage(
        convId: String,
        text: String,
        messageId: String? = null,
        showFlashcardButton: Boolean = false,
        showMindMapButton: Boolean = false,
        documentId: String? = null,
        artifactType: String? = null,
        artifactJson: JsonElement? = null
    ) {
        val index = _conversations.indexOfFirst { it.id == convId }
        if (index == -1) return
        val updatedConv = _conversations[index]

        val isQuiz = artifactType == "QUIZ"
        val isPlan = artifactType == "STUDY_PLAN"
        val isFlash = showFlashcardButton || artifactType == "FLASHCARDS"
        val isMindMap = showMindMapButton || artifactType == "MINDMAP"

        updatedConv.chatMessages.add(
            ChatMessage(
                id = messageId ?: UUID.randomUUID().toString(),
                text = text,
                isUser = false,
                showQuizButton = isQuiz,
                showStudyPlanButton = isPlan,
                showFlashcardButton = isFlash,
                showMindMapButton = isMindMap,
                documentId = documentId ?: updatedConv.documentId,
                artifactType = artifactType,
                artifactJson = artifactJson,
                createdAt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            )
        )
        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
    }

    private fun appendProcessingMessage(convId: String, text: String): String {
        val index = _conversations.indexOfFirst { it.id == convId }
        if (index == -1) return ""
        val messageId = UUID.randomUUID().toString()
        val updatedConv = _conversations[index]
        updatedConv.chatMessages.add(
            ChatMessage(
                id = messageId,
                text = text,
                isUser = false,
                isProcessing = true
            )
        )
        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
        return messageId
    }

    private fun replaceMessage(convId: String, messageId: String, newText: String) {
        val index = _conversations.indexOfFirst { it.id == convId }
        if (index == -1) return
        val updatedConv = _conversations[index]
        val msgIndex = updatedConv.chatMessages.indexOfFirst { it.id == messageId }
        if (msgIndex != -1) {
            updatedConv.chatMessages[msgIndex] = updatedConv.chatMessages[msgIndex].copy(
                text = newText,
                isProcessing = false
            )
            _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
        }
    }

    private fun rebindConversationId(localConversationId: String, backendConversationId: String): String {
        if (localConversationId.isBlank() || localConversationId == backendConversationId) {
            return backendConversationId
        }

        val localIndex = _conversations.indexOfFirst { it.id == localConversationId }
        if (localIndex == -1) {
            activeConversationId = backendConversationId
            return backendConversationId
        }

        val backendIndex = _conversations.indexOfFirst { it.id == backendConversationId }
        if (backendIndex != -1) {
            val localConversation = _conversations[localIndex]
            val backendConversation = _conversations[backendIndex]
            val mergedMessages = (backendConversation.chatMessages + localConversation.chatMessages)
                .distinctBy { it.id }
                .toMutableList()
            _conversations[backendIndex] = backendConversation.copy(chatMessages = mergedMessages)
            _conversations.removeAt(localIndex)
        } else {
            val localConversation = _conversations[localIndex]
            _conversations[localIndex] = localConversation.copy(id = backendConversationId)
        }

        if (activeConversationId == localConversationId) {
            activeConversationId = backendConversationId
        }
        return backendConversationId
    }

    private fun maybeAutoRenameConversation(convId: String, userMessage: String, assistantMessage: String) {
        val index = _conversations.indexOfFirst { it.id == convId }
        if (index == -1) return

        val conversation = _conversations[index]
        if (conversation.autoTitleApplied) return

        viewModelScope.launch {
            try {
                val prompt = buildString {
                    append("Create a short conversation title in English, maximum 5 words. ")
                    append("Return only the title, with no quotes, no punctuation, and no extra text. ")
                    append("Conversation user message: ")
                    append(userMessage)
                    append(". Assistant message: ")
                    append(assistantMessage)
                }
                val generated = AiAskService.directAsk(prompt).getOrNull()?.trim().orEmpty()
                val title = generated
                    .trim('"', '\'', '`')
                    .replace(Regex("\\s+"), " ")
                    .take(48)
                    .ifBlank { userMessage.trim().take(48) }

                val finalTitle = title.ifBlank { conversation.title }
                if (finalTitle.isNotBlank()) {
                    _conversations[index] = conversation.copy(title = finalTitle, autoTitleApplied = true)
                    persistChatState()
                    runCatching {
                        RetrofitClient.instance.renameConversation(convId, RenameConversationRequest(finalTitle))
                    }
                }
            } catch (_: Exception) {
                _conversations[index] = conversation.copy(autoTitleApplied = true)
                persistChatState()
            }
        }
    }

    private fun queryDisplayName(resolver: android.content.ContentResolver, uri: Uri): String? {
        return runCatching {
            resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
    }

    private fun guessExtension(mimeType: String): String = when {
        mimeType.contains("pdf", ignoreCase = true) -> "pdf"
        mimeType.startsWith("image/") -> mimeType.substringAfter("image/", "jpg")
        else -> "bin"
    }

    private suspend fun generateAiReply(convId: String, userMessage: String, turnId: String) {
        isTyping = true
        val conversationIndex = _conversations.indexOfFirst { it.id == convId }
        if (conversationIndex == -1) {
            isTyping = false
            return
        }

        if (isQuizIntent(userMessage)) {
            logTurn(turnId, "persist_general_intent_quiz", convId)
            val persisted = persistIntentTurnForGeneralChat(convId, userMessage)
            if (persisted == null) {
                isTyping = false
                return
            }
            val persistedConvId = rebindConversationId(convId, persisted.conversationId)
            logTurn(turnId, "ai_reply_hidden_for_quiz", persistedConvId, "source=chat_ask")
            handleQuizIntent(persistedConvId, userMessage)
            isTyping = false
            return
        }

        if (isPlanIntent(userMessage)) {
            logTurn(turnId, "persist_general_intent_plan", convId)
            val persisted = persistIntentTurnForGeneralChat(convId, userMessage)
            if (persisted == null) {
                isTyping = false
                return
            }
            val persistedConvId = rebindConversationId(convId, persisted.conversationId)
            if (persisted.answer.isNotBlank()) {
                appendAiMessage(persistedConvId, persisted.answer, persisted.messageId)
                persistChatState()
                logTurn(turnId, "ai_reply_appended", persistedConvId, "source=chat_ask")
            }
            handlePlanIntent(persistedConvId, userMessage, activeDocumentId)
            isTyping = false
            return
        }

        try {
            val currentConversation = _conversations.getOrNull(conversationIndex)
            val requestTitle = currentConversation
                ?.title
                ?.takeIf { it.isNotBlank() }
                ?: userMessage.trim().take(48).takeIf { it.isNotBlank() }
            val askResponse = RetrofitClient.instance.askAI(
                AskAiRequest(
                    message = userMessage,
                    conversationId = convId,
                    title = requestTitle
                )
            )
            val resolvedConversationId = rebindConversationId(convId, askResponse.conversationId)
            val aiResponse = askResponse.answer.ifBlank {
                "AI returned an empty response. Please try again."
            }

            val showQuiz = askResponse.artifactType == "QUIZ"
            val showPlan = askResponse.artifactType == "STUDY_PLAN"
            val showFlashcards = askResponse.artifactType == "FLASHCARDS"
            val showMindMap = askResponse.artifactType == "MINDMAP"

            val resolvedIndex = _conversations.indexOfFirst { it.id == resolvedConversationId }
            if (resolvedIndex == -1) {
                isTyping = false
                return
            }
            val updatedConv = _conversations[resolvedIndex]
            updatedConv.chatMessages.add(
                ChatMessage(
                    id = askResponse.messageId,
                    text = aiResponse,
                    isUser = false,
                    showQuizButton = showQuiz,
                    showStudyPlanButton = showPlan,
                    showFlashcardButton = showFlashcards,
                    showMindMapButton = showMindMap,
                    artifactType = askResponse.artifactType,
                    artifactJson = askResponse.artifactData,
                    planJson = if (showPlan) askResponse.artifactData?.toString() else null,
                    documentId = updatedConv.documentId
                )
            )

            if (askResponse.artifactType == "QUIZ" && askResponse.artifactData != null && !askResponse.artifactData.isJsonNull) {
                val questions = parseQuestionsFromJson(askResponse.artifactData)
                onQuizGenerated?.invoke(questions)
            } else if (askResponse.artifactType == "STUDY_PLAN" && askResponse.artifactData != null && !askResponse.artifactData.isJsonNull) {
                onPlanGenerated?.invoke(askResponse.artifactData.toString())
            }

            _conversations[resolvedIndex] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
            persistChatState()
            logTurn(turnId, "ai_reply_appended", resolvedConversationId, "source=chat_ask")
            maybeAutoRenameConversation(
                resolvedConversationId,
                askResponse.question.ifBlank { userMessage },
                aiResponse
            )
        } catch (e: HttpException) {
            handleHttpError(e, forceLogoutOn401 = false)
            logTurn(turnId, "turn_http_error", convId, "code=${e.code()}")
        } catch (_: SocketTimeoutException) {
            errorMessage = "The AI is taking longer than expected. Please try again."
            logTurn(turnId, "turn_timeout", convId)
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to get AI response."
            logTurn(turnId, "turn_error", convId, e.javaClass.simpleName)
        }
        isTyping = false
    }

    private suspend fun handleQuizIntent(convId: String, userMessage: String) {
        try {
            val generatedQuiz = generateQuizQuestions(convId, userMessage)
            val resolvedConversationId = generatedQuiz.conversationId ?: convId
            val quizQuestions = generatedQuiz.questions
            if (quizQuestions.isEmpty()) {
                appendAiMessage(resolvedConversationId, "Could not generate quiz at the moment. Please try again.")
                persistChatState()
                return
            }

            onQuizGenerated?.invoke(quizQuestions)
            val quizTitle = generatedQuiz.title ?: "Quiz generated from chat"
            
            
            val storageJson = JsonObject().apply {
                add("questions", Gson().toJsonTree(quizQuestions))
                addProperty("quizTitle", quizTitle)
            }

            persistArtifactToBackend(
                convId = resolvedConversationId,
                artifactType = "QUIZ",
                artifact = storageJson,
                note = quizTitle
            )
            refreshConversationMessages(resolvedConversationId)
            
            viewModelScope.launch {
                delay(700L)
                refreshConversationMessages(resolvedConversationId)
            }
            
            updateConversationKind(resolvedConversationId, ConversationKind.QUIZ)
            val index = _conversations.indexOfFirst { it.id == resolvedConversationId }
            if (index != -1) {
                maybeAutoRenameConversation(resolvedConversationId, userMessage, "Quiz is ready: $quizTitle")
            }
        } catch (e: HttpException) {
            handleHttpError(e, forceLogoutOn401 = false)
        } catch (_: SocketTimeoutException) {
            errorMessage = "Quiz generation timed out. Please try again."
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Could not generate quiz at the moment."
        }
    }

    private suspend fun handlePlanIntent(convId: String, userMessage: String, documentId: String?) {
        try {
            val planJson = generateStudyPlanJson(userMessage, documentId)
            val parsed = StudyPlanJsonParser.parse(planJson)
            if (parsed == null || parsed.modules.isEmpty()) {
                appendAiMessage(convId, "Could not generate study plan at the moment. Please try again.")
                persistChatState()
                return
            }

            onPlanGenerated?.invoke(planJson)
            val planTitle = parsed.title.ifBlank { "Study plan generated from chat" }
            
            persistArtifactToBackend(
                convId = convId,
                artifactType = "STUDY_PLAN",
                artifact = JsonParser().parse(planJson),
                note = planTitle
            )
            updateConversationKind(convId, ConversationKind.PLAN)
            val courses = extractCoursesFromPlanJson(planJson, parsed)
            val index = _conversations.indexOfFirst { it.id == convId }
            if (index != -1) {
                val updatedConv = _conversations[index]
                updatedConv.chatMessages.add(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "Study plan is ready.",
                        isUser = false,
                        showStudyPlanButton = true,
                        planJson = planJson,
                        courses = courses,
                        specificTitle = planTitle,
                        documentId = updatedConv.documentId
                    )
                )
                _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
                persistChatState()
                maybeAutoRenameConversation(convId, userMessage, "Study plan is ready.")
            }
        } catch (e: HttpException) {
            handleHttpError(e, forceLogoutOn401 = false)
        } catch (_: SocketTimeoutException) {
            errorMessage = "Study plan generation timed out. Please try again."
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Could not generate study plan at the moment."
        }
    }

    private data class QuizGenerationResult(
        val questions: List<QuizQuestion>,
        val title: String? = null,
        val conversationId: String? = null
    )

    private suspend fun generateQuizQuestions(convId: String, userMessage: String): QuizGenerationResult {
        val documentId = activeDocumentId
        if (!documentId.isNullOrBlank() && !RetrofitClient.authToken.isNullOrBlank()) {
            val generated = RetrofitClient.instance.generateQuiz(documentId)
            val resolvedConversationId = rebindConversationId(convId, generated.conversationId)
            return QuizGenerationResult(
                questions = generated.questions.toUiQuizQuestions(),
                conversationId = resolvedConversationId
            )
        }

        val hiddenPrompt = buildString {
            append("Create a quiz in valid JSON only. ")
            append("Schema: {\"title\":\"A smart title for this quiz based on the topic\",\"questions\":[{\"id\":1,\"text\":\"Question\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":\"A\",\"explanation\":\"...\"}]}. ")
            append("Rules: Output English only, no markdown, no code fences, exactly 4 options. ")
            append("User request: ")
            append(userMessage)
        }
        val response = RetrofitClient.instance.aiAsk(AiAskRequest(hiddenPrompt)).answer
        val (questions, title) = parseStructuredQuiz(response)
        return QuizGenerationResult(questions = questions, title = title, conversationId = convId)
    }

    private fun isQuizIntent(message: String): Boolean {
        val lower = message.lowercase()
        return quizKeywords.any { lower.contains(it) }
    }

    private fun isPlanIntent(message: String): Boolean {
        val lower = message.lowercase()
        return planKeywords.any { lower.contains(it) }
    }

    private suspend fun generateStudyPlanJson(userMessage: String, documentId: String?): String {
        if (!documentId.isNullOrBlank() && !RetrofitClient.authToken.isNullOrBlank()) {
            val response = RetrofitClient.instance.generateStudyPlan(documentId)
            val raw = response.studyPlan?.toString().orEmpty()
            if (raw.isNotBlank()) return raw
            throw IllegalStateException("Study plan API returned empty payload.")
        }

        val hiddenPrompt = buildString {
            append("Create a study plan in valid JSON only. ")
            append("Schema: {\"planId\":\"...\",\"title\":\"...\",\"overview\":\"...\",\"estimatedTotalMinutes\":120,\"modules\":[{\"moduleId\":\"m1\",\"order\":1,\"documentId\":\"8f7f4d7a-9f9e-4c97-b1e2-c6f9f5e5b0e1\",\"title\":\"...\",\"objective\":\"...\",\"estimatedMinutes\":30,\"difficulty\":\"BEGINNER\",\"status\":\"IN_PROGRESS\",\"quiz\":{\"recommendedQuestionCount\":7,\"passScore\":30}}]}. ")
            append("If a real backend document id is unknown, set documentId to an empty string. ")
            append("Rules: Output English only, no markdown, no code fences, status must be LOCKED|IN_PROGRESS|COMPLETED. ")
            append("User request: ")
            append(userMessage)
        }
         val response = RetrofitClient.instance.aiAsk(AiAskRequest(hiddenPrompt)).answer
         return response
             .replace("```json", "")
             .replace("```", "")
             .trim()
     }

    private fun extractCoursesFromPlanJson(_planJson: String, parsed: StudyPlanResponse?): List<ChatMessageCourse> {
         if (parsed == null) return emptyList()
         
         val courseGroups = mutableMapOf<String, Int>()
         
         parsed.modules.forEach { module ->
             val courseTitle = module.title
             courseGroups[courseTitle] = (courseGroups[courseTitle] ?: 0) + 1
         }
         
         return courseGroups.map { (title, count) ->
             ChatMessageCourse(
                 id = UUID.randomUUID().toString(),
                 title = title,
                 lessonCount = count
             )
         }
     }

     private fun updateConversationKind(convId: String, kind: ConversationKind) {
        val index = _conversations.indexOfFirst { it.id == convId }
        if (index == -1) return
        val updated = _conversations[index]
        _conversations[index] = updated.copy(
            kind = kind,
            isQuiz = kind == ConversationKind.QUIZ
        )
    }

    fun generateQuizForDocument(documentId: String, onComplete: (String, List<QuizQuestion>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.generateQuiz(documentId)
                val uiQuestions = response.questions.toUiQuizQuestions()
                withContext(Dispatchers.Main) {
                    onComplete(response.quizId, uiQuestions)
                }
            } catch (e: HttpException) {
                handleHttpError(e)
                withContext(Dispatchers.Main) {
                    onComplete("", emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate quiz for document $documentId", e)
                withContext(Dispatchers.Main) {
                    errorMessage = e.localizedMessage ?: "Failed to generate quiz."
                    onComplete("", emptyList())
                }
            }
        }
    }

    private fun parseQuestionsFromJson(json: JsonElement): List<QuizQuestion> {
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<ApiQuizQuestion>>() {}.type
            val backendQuestions = Gson().fromJson<List<ApiQuizQuestion>>(json, type)
            backendQuestions.toUiQuizQuestions()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun List<ApiQuizQuestion>.toUiQuizQuestions(): List<QuizQuestion> {
        return mapIndexed { idx, item ->
            val ordered = listOf("A", "B", "C", "D")
                .mapNotNull { key -> item.options[key] }
                .ifEmpty { item.options.values.toList() }
                .take(4)
            val safeOptions = if (ordered.size == 4) ordered else listOf("Option A", "Option B", "Option C", "Option D")
            val answerIndex = orderedAnswerIndex(item.correctAnswer, safeOptions)
            QuizQuestion(
                id = "api-${idx + 1}",
                question = item.question,
                options = safeOptions,
                correctAnswerIndex = answerIndex,
                hint = "Choose the best answer.",
                explanation = item.explanation
            )
        }
    }

    private fun parseStructuredQuiz(raw: String): Pair<List<QuizQuestion>, String?> {
        val cleaned = raw
            .replace("```json", "")
            .replace("```", "")
            .trim()
        val root = runCatching { JsonParser().parse(cleaned) }.getOrNull() ?: return Pair(emptyList(), null)
        val questionsArray = when (root) {
            is JsonArray -> root
            is JsonObject -> root.getAsJsonArray("questions")
            else -> null
        } ?: return Pair(emptyList(), null)
        
        val title = (root as? JsonObject)?.getAsStringOrNull("title")

        val questions = questionsArray.mapIndexedNotNull { idx, element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapIndexedNotNull null
            val text = obj.getAsStringOrNull("text") ?: obj.getAsStringOrNull("question") ?: return@mapIndexedNotNull null
            val options = obj.getAsJsonArray("options")?.mapNotNull { it.asStringOrNull() }?.take(4).orEmpty()
            if (options.size != 4) return@mapIndexedNotNull null
            val answerRaw = obj.getAsStringOrNull("answer") ?: return@mapIndexedNotNull null
            val explanation = obj.getAsStringOrNull("explanation") ?: ""
            QuizQuestion(
                id = obj.getAsStringOrNull("id") ?: "json-${idx + 1}",
                question = text,
                options = options,
                correctAnswerIndex = orderedAnswerIndex(answerRaw, options),
                hint = "Choose the best answer.",
                explanation = explanation
            )
        }
        return Pair(questions, title)
    }

    private fun orderedAnswerIndex(answerRaw: String, options: List<String>): Int {
        val normalized = answerRaw.trim()
        val byLetter = when (normalized.uppercase()) {
            "A" -> 0
            "B" -> 1
            "C" -> 2
            "D" -> 3
            else -> -1
        }
        if (byLetter in options.indices) return byLetter
        val byText = options.indexOfFirst { it.equals(normalized, ignoreCase = true) }
        return if (byText >= 0) byText else 0
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? = if (isJsonObject) asJsonObject else null
    private fun JsonElement.asStringOrNull(): String? = runCatching { asString }.getOrNull()
    private fun JsonObject.getAsStringOrNull(key: String): String? = get(key)?.asStringOrNull()

    private fun persistArtifactToBackend(convId: String, artifactType: String, artifact: JsonElement, note: String) {
        val documentId = _conversations.firstOrNull { it.id == convId }?.documentId ?: return
        if (documentId.isBlank() || RetrofitClient.authToken.isNullOrBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                RetrofitClient.instance.saveDocumentArtifact(
                    documentId,
                    SaveDocumentArtifactRequest(
                        artifactType = artifactType,
                        artifact = artifact,
                        note = note
                    )
                )
            }.onFailure { error ->
                if (error is HttpException) {
                    handleHttpError(error, forceLogoutOn401 = false)
                }
            }
        }
    }

    fun selectConversation(id: String) {
        val selected = _conversations.find { it.id == id } ?: return
        activeConversationId = id
        _currentChatType.value = ChatScreenType.CONTINUING_CHAT
        isTyping = false
        activeDocumentId = selected.documentId
        refreshConversationMessages(id)
        persistChatState()
    }

    fun refreshConversationMessages(conversationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messages = RetrofitClient.instance.getConversationMessages(conversationId)
                withContext(Dispatchers.Main) {
                    val mappedHistory = messages.toBackendHistory().toUiMessages(conversationId)
                    val index = _conversations.indexOfFirst { it.id == conversationId }
                    if (index != -1) {
                        val inferredKind = when {
                            mappedHistory.any { it.showStudyPlanButton } -> ConversationKind.PLAN
                            mappedHistory.any { it.showQuizButton } -> ConversationKind.QUIZ
                            else -> ConversationKind.CHAT
                        }

                        val messagesWithImages = mappedHistory.map { msg ->
                            if (!msg.imageMimeType.isNullOrBlank() && msg.isUser) {
                                val messageIdFromId = msg.id.removePrefix("hist-user-")
                                try {
                                    val imageResponse = RetrofitClient.instance.getMessageImage(messageIdFromId)
                                    msg.copy(
                                        imageBase64 = imageResponse.base64,
                                        imageMimeType = imageResponse.mimeType,
                                        imageOriginalName = imageResponse.originalName
                                    )
                                } catch (e: Exception) {
                                    msg
                                }
                            } else {
                                msg
                            }
                        }

                        _conversations[index] = _conversations[index].copy(
                            kind = inferredKind,
                            isQuiz = inferredKind == ConversationKind.QUIZ,
                            chatMessages = messagesWithImages.toMutableList()
                        )
                        persistChatState()
                    }
                }
            } catch (e: Exception) {
                if (e is HttpException) {
                    if (e.code() == 404) {
                        _conversations.removeIf { it.id == conversationId }
                        if (activeConversationId == conversationId) {
                            activeConversationId = ""
                            activeDocumentId = null
                            _currentChatType.value = ChatScreenType.NEW_CHAT
                        }
                        errorMessage = "This conversation is no longer available."
                        persistChatState()
                    } else {
                        handleHttpError(e)
                    }
                }
            }
        }
    }

    fun loadConversationsFromBackend() {
        viewModelScope.launch {
            try {
                if (RetrofitClient.authToken.isNullOrBlank()) return@launch
                val convInfos = RetrofitClient.instance.getConversations()
                val backendConversationIds = convInfos.map { it.id }.toSet()
                val localConversations = _conversations.toList()
                val existingMessagesByConversationId = localConversations.associate { it.id to it.chatMessages.toMutableList() }
                val conversations = convInfos.map { info ->
                    Conversation(
                        id = info.id,
                        title = info.title,
                        kind = runCatching { ConversationKind.valueOf(info.kind.orEmpty()) }
                            .getOrDefault(ConversationKind.CHAT),
                        autoTitleApplied = true,
                        documentId = info.documentId,
                        chatMessages = (existingMessagesByConversationId[info.id] ?: emptyList()).toMutableList()
                    )
                }
                val pendingLocalConversations = localConversations.filter { conversation ->
                    conversation.id !in backendConversationIds &&
                        (conversation.documentId != null || conversation.title.isBlank() || !conversation.autoTitleApplied)
                }
                _conversations.clear()
                _conversations.addAll(conversations + pendingLocalConversations)

                if (openedFromFreshLogin) {
                    activeConversationId = ""
                    activeDocumentId = null
                    _currentChatType.value = ChatScreenType.NEW_CHAT
                    openedFromFreshLogin = false
                    persistChatState()
                    return@launch
                }

                val activeStillExists = _conversations.any { it.id == activeConversationId }
                if (!activeStillExists) {
                    val firstConversation = _conversations.firstOrNull()
                    if (firstConversation == null) {
                        activeConversationId = ""
                        activeDocumentId = null
                        _currentChatType.value = ChatScreenType.NEW_CHAT
                    } else {
                        activeConversationId = firstConversation.id
                        activeDocumentId = firstConversation.documentId
                        _currentChatType.value = ChatScreenType.CONTINUING_CHAT
                    }
                } else {
                    activeDocumentId = _conversations.find { it.id == activeConversationId }?.documentId
                    _currentChatType.value = ChatScreenType.CONTINUING_CHAT
                }

                if (activeConversationId.isNotBlank() && activeConversationId in backendConversationIds) {
                    refreshConversationMessages(activeConversationId)
                }

                persistChatState()
            } catch (e: Exception) {
            }
        }
    }

    fun resetForAccountSwitch() {
        clearAskSession()
        _conversations.clear()
        activeConversationId = ""
        activeDocumentId = null
        searchQuery = ""
        errorMessage = null
        uploadStatusLabel = null
        uploadTerminalStatus = null
        _currentChatType.value = ChatScreenType.NEW_CHAT
        persistChatState()
    }

    fun updateSearchQuery(query: String) { searchQuery = query }
    fun renameConversation(id: String, newTitle: String) {
        val index = _conversations.indexOfFirst { it.id == id }
        if (index != -1 && newTitle.isNotBlank()) {
            _conversations[index] = _conversations[index].copy(title = newTitle, autoTitleApplied = true)
            persistChatState()
            
            viewModelScope.launch {
                try {
                    RetrofitClient.instance.renameConversation(id, RenameConversationRequest(newTitle))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync conversation rename to backend", e)
                }
            }
        }
    }
    fun deleteConversation(id: String) {
        if (id.isBlank()) return

        viewModelScope.launch {
            if (!RetrofitClient.authToken.isNullOrBlank()) {
                try {
                    RetrofitClient.instance.deleteConversation(id)
                } catch (e: HttpException) {
                    if (e.code() != 404) {
                        handleHttpError(e, forceLogoutOn401 = false)
                        return@launch
                    }
                } catch (e: Exception) {
                    errorMessage = e.localizedMessage ?: "Failed to delete conversation."
                    return@launch
                }
            }

            _conversations.removeIf { it.id == id }
            if (activeConversationId == id) {
                startNewChat()
            } else {
                persistChatState()
            }

            if (!RetrofitClient.authToken.isNullOrBlank()) {
                loadConversationsFromBackend()
            }
        }
    }
    val activeMessages: List<ChatMessage> get() = _conversations.find { it.id == activeConversationId }?.chatMessages ?: emptyList()
    val filteredConversations: List<Conversation>
        get() {
            val visibleConversations = _conversations.filter { it.title.isNotBlank() }
            return if (searchQuery.isEmpty()) {
                visibleConversations
            } else {
                visibleConversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }
        }
    val pendingConversations: List<Conversation>
        get() = if (searchQuery.isEmpty()) {
            _conversations.filter { it.title.isBlank() }
        } else {
            _conversations
                .find { it.id == activeConversationId && it.title.isBlank() }
                ?.let { listOf(it) }
                ?: emptyList()
        }

    private fun persistChatState() {
        viewModelScope.launch(Dispatchers.IO) {
            LocalHistoryStore.saveChatState(
                LocalHistoryStore.runtimeChatState(
                    conversations = _conversations.toList(),
                    activeConversationId = activeConversationId,
                    currentChatType = _currentChatType.value.name
                )
            )
        }
    }

    private fun handleHttpError(error: HttpException, forceLogoutOn401: Boolean = true) {
        if (error.code() == 401) {
            RetrofitClient.logAuthTokenDiagnostics("Received 401 in ChatViewModel")
            if (forceLogoutOn401) {
                RetrofitClient.updateAuthToken(null)
                errorMessage = "Session expired. Please log in again."
                sessionExpired = true
            } else {
                errorMessage = "Unauthorized for this action. Please check account/session state."
            }
        } else {
            errorMessage = error.message() ?: "Request failed with code ${error.code()}"
        }
    }

    fun consumeSessionExpired() {
        sessionExpired = false
    }

    fun directAiAsk(question: String) {
        if (question.isBlank()) {
            errorMessage = "Please enter a question"
            return
        }

        viewModelScope.launch {
            isTyping = true
            aiAskState = AskState.PROCESSING
            errorMessage = null

            val convId = ensureConversationForDirectAsk()
            appendUserMessage(convId, question)
            persistChatState()

            try {
                val result = AiAskService.directAsk(question)
                result.onSuccess { answer ->
                    appendAiMessage(convId, answer)
                    aiAskState = AskState.COMPLETED
                    persistChatState()
                    maybeAutoRenameConversation(convId, question, answer)
                }.onFailure { error ->
                    errorMessage = error.localizedMessage ?: "Failed to get AI response"
                    aiAskState = AskState.ERROR
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to ask AI"
                aiAskState = AskState.ERROR
            } finally {
                isTyping = false
            }
        }
    }

    fun uploadThenAsk(context: Context, fileUri: Uri, question: String, fileName: String? = null) {
        if (question.isBlank()) {
            errorMessage = "Please enter a question"
            return
        }
        if (!RetrofitClient.hasUsableAuthToken()) {
            RetrofitClient.logAuthTokenDiagnostics("Blocked uploadThenAsk: token missing or expired")
            errorMessage = "Missing or expired token. Please log in again."
            sessionExpired = true
            return
        }

        viewModelScope.launch {
            isUploading = true
            isTyping = false
            aiAskState = AskState.UPLOADING
            uploadStatusLabel = "Uploading document"
            uploadTerminalStatus = null
            errorMessage = null

            val convId = ensureConversationForDirectAsk()
            appendUserMessage(convId, question, fileName)
            persistChatState()

            try {
                aiAskState = AskState.PROCESSING
                uploadStatusLabel = "Uploading and asking"

                val result = AiAskService.uploadThenAsk(context, fileUri, question, fileName)
                result.onSuccess { sessionId ->
                    currentAskSessionId = sessionId
                    val session = AiAskService.getSession(sessionId)
                    val documentId = session?.documentId
                        ?: throw IllegalStateException("Upload succeeded but missing document id.")
                    val answer = session.answer.trim()
                    if (answer.isBlank()) {
                        throw IllegalStateException("AI returned an empty response")
                    }

                    currentAskDocumentId = documentId
                    activeDocumentId = documentId
                    val convIndex = _conversations.indexOfFirst { it.id == convId }
                    if (convIndex != -1) {
                        _conversations[convIndex] = _conversations[convIndex].copy(documentId = documentId)
                    }

                    appendAiMessage(convId, answer)
                    maybeAutoRenameConversation(convId, question, answer)

                    val backendConvId = runCatching {
                        RetrofitClient.instance.getConversations()
                            .firstOrNull { it.documentId == documentId }
                            ?.id
                    }.getOrNull()
                    val resolvedConvId = when {
                        backendConvId.isNullOrBlank() -> convId
                        backendConvId == convId -> convId
                        else -> rebindConversationId(convId, backendConvId)
                    }
                    refreshConversationMessages(resolvedConvId)
                }.onFailure { error ->
                    throw error
                }

                aiAskState = AskState.COMPLETED
                uploadTerminalStatus = "COMPLETED"
                persistChatState()
            } catch (e: HttpException) {
                uploadTerminalStatus = "FAILED"
                handleHttpError(e)
                aiAskState = AskState.ERROR
            } catch (e: Exception) {
                uploadTerminalStatus = "FAILED"
                errorMessage = e.localizedMessage ?: "Failed to process request"
                aiAskState = AskState.ERROR
            } finally {
                uploadStatusLabel = null
                isUploading = false
                isTyping = false
            }
        }
    }

    fun retryAsk(newQuestion: String? = null) {
        val sessionId = currentAskSessionId
        if (sessionId.isNullOrBlank()) {
            errorMessage = "No upload session available to retry"
            return
        }
        if (!RetrofitClient.hasUsableAuthToken()) {
            RetrofitClient.logAuthTokenDiagnostics("Blocked retryAsk: token missing or expired")
            errorMessage = "Missing or expired token. Please log in again."
            sessionExpired = true
            return
        }

        viewModelScope.launch {
            isTyping = true
            aiAskState = AskState.PROCESSING
            errorMessage = null
            val retryQuestion = newQuestion?.takeIf { it.isNotBlank() }
                ?: activeMessages.lastOrNull { it.isUser }?.text
            if (retryQuestion.isNullOrBlank()) {
                errorMessage = "Please enter a question"
                aiAskState = AskState.ERROR
                isTyping = false
                return@launch
            }

            val convId = activeConversationId.takeIf { it.isNotBlank() }
                ?: ensureConversationForDirectAsk()

            if (!newQuestion.isNullOrBlank()) {
                appendUserMessage(convId, newQuestion)
            }
            persistChatState()

            try {
                val result = AiAskService.retryAsk(sessionId, retryQuestion)
                result.onSuccess {
                    val session = AiAskService.getSession(sessionId)
                        ?: throw IllegalStateException("Session not found")
                    val documentId = session.documentId
                        ?: throw IllegalStateException("Retry session missing document id")
                    val answer = session.answer.trim()
                    if (answer.isBlank()) {
                        throw IllegalStateException("AI returned an empty response")
                    }

                    currentAskDocumentId = documentId
                    activeDocumentId = documentId
                    appendAiMessage(convId, answer)
                    maybeAutoRenameConversation(convId, retryQuestion, answer)

                    val backendConvId = runCatching {
                        RetrofitClient.instance.getConversations()
                            .firstOrNull { it.documentId == documentId }
                            ?.id
                    }.getOrNull()
                    val resolvedConvId = when {
                        backendConvId.isNullOrBlank() -> convId
                        backendConvId == convId -> convId
                        else -> rebindConversationId(convId, backendConvId)
                    }
                    refreshConversationMessages(resolvedConvId)
                }.onFailure { error ->
                    throw error
                }

                aiAskState = AskState.COMPLETED
                persistChatState()
            } catch (e: HttpException) {
                handleHttpError(e)
                aiAskState = AskState.ERROR
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Retry failed"
                aiAskState = AskState.ERROR
            } finally {
                isTyping = false
            }
        }
    }

    fun getCurrentAskSessionState(): AskState = aiAskState

    suspend fun ensureActiveConversationForStudyPlan(): String? {
        return turnMutex.withLock {
            if (activeConversationId.isNotBlank()) {
                return@withLock activeConversationId
            }

            if (!RetrofitClient.hasUsableAuthToken()) {
                Log.w(TAG, "Cannot bootstrap study-plan conversation: missing/expired token")
                return@withLock null
            }

            val turnId = newTurnId("plan-bootstrap")
            logTurn(turnId, "bootstrap_request")

            try {
                val response = RetrofitClient.instance.askAI(
                    AskAiRequest(
                        message = "Initialize a conversation for study plan tracking. Reply briefly with 'Study plan context ready.'",
                        title = "Study Plan"
                    )
                )
                val convId = response.conversationId
                if (_conversations.none { it.id == convId }) {
                    _conversations.add(
                        0,
                        Conversation(
                            id = convId,
                            title = "Study Plan",
                            kind = ConversationKind.PLAN,
                            autoTitleApplied = true
                        )
                    )
                }
                activeConversationId = convId
                _currentChatType.value = ChatScreenType.CONTINUING_CHAT
                persistChatState()
                logTurn(turnId, "bootstrap_success", convId, "messageId=${response.messageId}")
                convId
            } catch (e: HttpException) {
                handleHttpError(e, forceLogoutOn401 = false)
                logTurn(turnId, "bootstrap_http_error", detail = "code=${e.code()}")
                null
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to initialize study plan conversation."
                logTurn(turnId, "bootstrap_error", detail = e.javaClass.simpleName)
                null
            }
        }
    }

    @Suppress("unused")
    fun clearAskSession() {
        currentAskSessionId?.let { AiAskService.clearSession(it) }
        currentAskSessionId = null
        currentAskDocumentId = null
        aiAskState = AskState.IDLE
    }

    private fun ensureConversationForDirectAsk(): String {
        val convId = if (activeConversationId.isNotBlank()) {
            activeConversationId
        } else {
            val newConvId = UUID.randomUUID().toString()
            _conversations.add(0, Conversation(newConvId, "", autoTitleApplied = false))
            activeConversationId = newConvId
            _currentChatType.value = ChatScreenType.CONTINUING_CHAT
            newConvId
        }
        return convId
    }

    private fun List<BackendChatMessage>.toUiMessages(targetConversationId: String? = null): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        val convIdToUse = targetConversationId ?: activeConversationId
        val conversation = _conversations.find { it.id == convIdToUse }
        val docIdToUse = conversation?.documentId
        val localMessages = conversation?.chatMessages.orEmpty()





        val lastArtifactIdByType = mutableMapOf<String, String>()
        for (item in this) {
            val mt = item.messageType.orEmpty().trim().uppercase()
            val at = item.artifactType.orEmpty().trim().uppercase()
            if (mt == "ARTIFACT" && at.isNotBlank()) {
                lastArtifactIdByType[at] = item.id
            }
        }
        val suppressedArtifactIds = mutableSetOf<String>()
        for (item in this) {
            val mt = item.messageType.orEmpty().trim().uppercase()
            val at = item.artifactType.orEmpty().trim().uppercase()
            if (mt == "ARTIFACT" && at.isNotBlank() && lastArtifactIdByType[at] != item.id) {
                suppressedArtifactIds.add(item.id)
            }
        }

        forEach { item ->
            val messageType = item.messageType.orEmpty().trim().uppercase()
            val artifactType = item.artifactType.orEmpty().trim().uppercase()

            if (messageType == "ARTIFACT" && suppressedArtifactIds.contains(item.id)) {
                return@forEach
            }

            if (messageType == "ARTIFACT") {
                val label = item.messageLabel
                val specificTitle = runCatching {
                    val json = item.artifactJson?.asJsonObjectOrNull()
                    when (artifactType) {
                        "QUIZ" -> json?.getAsStringOrNull("quizTitle") ?: json?.getAsStringOrNull("quizName") ?: json?.getAsStringOrNull("title")
                        "STUDY_PLAN" -> json?.getAsStringOrNull("courseName") ?: json?.getAsStringOrNull("title")
                        else -> null
                    }
                }.getOrNull()

                when (artifactType) {
                    "STUDY_PLAN" -> messages.add(
                        ChatMessage(
                            id = "hist-artifact-plan-${item.id}",
                            text = label ?: "Plan is ready. Tap Check Plan to view course lessons.",
                            isUser = false,
                            showStudyPlanButton = true,
                            messageLabel = label,
                            specificTitle = specificTitle,
                            messageType = messageType,
                            artifactType = artifactType,
                            artifactJson = item.artifactJson,
                            planJson = item.artifactJson?.toString(),
                            documentId = docIdToUse,
                            createdAt = item.createdAt
                        )
                    )
                    "MIND_MAP" -> messages.add(
                        ChatMessage(
                            id = "hist-artifact-mindmap-${item.id}",
                            text = label ?: "Mind Map is ready. Tap View Mind Map to explore.",
                            isUser = false,
                            showMindMapButton = true,
                            documentId = docIdToUse,
                            messageLabel = label,
                            specificTitle = specificTitle,
                            messageType = messageType,
                            artifactType = artifactType,
                            artifactJson = item.artifactJson,
                            createdAt = item.createdAt
                        )
                    )
                }
                return@forEach
            }
            if (item.question.isNotBlank()) {
                val localAttachment = localMessages.firstOrNull {
                    it.isUser && it.text == item.question && !it.attachmentName.isNullOrBlank()
                }
                val localImage = localMessages.firstOrNull {
                    it.isUser && it.text == item.question && !it.imageMimeType.isNullOrBlank()
                }
                messages.add(
                    ChatMessage(
                        id = "hist-user-${item.id}",
                        text = item.question,
                        isUser = true,
                        attachmentName = item.attachmentName ?: localAttachment?.attachmentName,
                        imageBase64 = null,
                        imageMimeType = item.imageMimeType ?: localImage?.imageMimeType,
                        imageOriginalName = item.imageOriginalName ?: localImage?.imageOriginalName,
                        documentId = docIdToUse,
                        createdAt = item.createdAt
                    )
                )
            }
            if (item.answer.isNotBlank()) {
                val answerText = item.answer.trim()
                val isStudyPlan = artifactType == "STUDY_PLAN" || looksLikeStudyPlanAnswer(answerText)
                
                val hasAlreadyAddedQuiz = messages.any { it.showQuizButton && it.id.contains(item.id) }
                
                messages.add(
                    ChatMessage(
                        id = "hist-ai-${item.id}",
                        text = if (isStudyPlan) "Study plan is ready." else answerText,
                        isUser = false,
                        showStudyPlanButton = isStudyPlan,
                        showFlashcardButton = artifactType == "FLASHCARDS",
                        artifactType = if (isStudyPlan) "STUDY_PLAN" else item.artifactType,
                        planJson = if (isStudyPlan) item.artifactJson?.toString() else null,
                        documentId = docIdToUse,
                        createdAt = item.createdAt
                    )
                )
            }
        }
        return messages
    }

    private fun looksLikeGeneratedQuizAnswer(answer: String): Boolean {
        val normalized = answer.lowercase()
        if (normalized.contains("answer key")) return true
        val numberedQuestions = Regex("""(?m)^\s*\d+\.\s*\*\*.*\?\*\*""")
        if (numberedQuestions.findAll(answer).count() >= 2) return true
        val optionLines = Regex("""(?m)^\s*[a-dA-D]\)\s+.+""")
        return optionLines.findAll(answer).count() >= 4
    }

    private fun looksLikeStudyPlanAnswer(answer: String): Boolean {
        val normalized = answer.lowercase()
        if (normalized.contains("study plan")) return true
        if (normalized.contains("week 1") || normalized.contains("week 2") || normalized.contains("week 3") || normalized.contains("week 4")) return true
        if (normalized.contains("day 1") || normalized.contains("day 2") || normalized.contains("day 3")) return true
        return normalized.count { it == '\n' } >= 6 && (normalized.contains("monday") || normalized.contains("tuesday") || normalized.contains("wednesday"))
    }

    private fun List<ConversationMessage>.toBackendHistory(): List<BackendChatMessage> {
        return map { item ->
            BackendChatMessage(
                id = item.id,
                messageLabel = item.messageLabel,
                question = item.question.orEmpty(),
                answer = item.answer.orEmpty(),
                createdAt = item.createdAt.orEmpty(),
                messageType = item.messageType,
                artifactType = item.artifactType,
                artifactJson = item.artifactJson,
                imageMimeType = item.imageMimeType,
                imageOriginalName = item.imageOriginalName,
                attachmentName = item.attachmentName
            )
        }
    }

    fun sendImageQuestion(context: Context, imageUri: Uri, question: String, conversationId: String? = null) {
        val turnId = newTurnId("image")
        if (question.isBlank()) {
            errorMessage = "Please enter a question"
            return
        }

        if (!RetrofitClient.hasUsableAuthToken()) {
            RetrofitClient.logAuthTokenDiagnostics("Blocked protected ask-image call: token missing or expired")
            errorMessage = "Missing or expired token. Please log in again."
            sessionExpired = true
            return
        }

        viewModelScope.launch {
            turnMutex.withLock {
                isUploading = true
                isTyping = false
                uploadStatusLabel = "Processing image..."
                uploadTerminalStatus = null
                errorMessage = null

                var conversationForImage = conversationId ?: if (_currentChatType.value == ChatScreenType.NEW_CHAT) {
                    val newConvId = UUID.randomUUID().toString()
                    _conversations.add(0, Conversation(newConvId, "", autoTitleApplied = false))
                    activeConversationId = newConvId
                    _currentChatType.value = ChatScreenType.CONTINUING_CHAT
                    newConvId
                } else {
                    activeConversationId
                }

                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
                val fileName = runCatching {
                    contentResolver.query(imageUri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                    }
                }.getOrNull() ?: "image.jpg"

                var processingMessageId = ""

                try {
                    val bytes = contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("Could not read image file.")

                    val localBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)

                    appendUserMessage(
                        convId = conversationForImage,
                        text = question,
                        attachmentName = fileName,
                        imageBase64 = localBase64,
                        imageMimeType = mimeType,
                        imageOriginalName = fileName
                    )
                    persistChatState()
                    logTurn(turnId, "user_sent_image", conversationForImage)

                    processingMessageId = appendProcessingMessage(conversationForImage, "Analyzing image...")
                    persistChatState()

                    val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("image", fileName, body)
                    val questionBody = question.toRequestBody("text/plain".toMediaTypeOrNull())

                    val response = RetrofitClient.instance.askAIWithImage(
                        image = imagePart,
                        question = questionBody,
                        conversationId = conversationForImage.toRequestBody("text/plain".toMediaTypeOrNull()),
                        title = question.take(48).toRequestBody("text/plain".toMediaTypeOrNull())
                    )

                    // Remove the processing message
                    if (processingMessageId.isNotBlank()) {
                        val index = _conversations.indexOfFirst { it.id == conversationForImage }
                        if (index != -1) {
                            val updatedConv = _conversations[index]
                            updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                            _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
                        }
                    }

                    val resolvedConversationId = rebindConversationId(conversationForImage, response.conversationId)
                    conversationForImage = resolvedConversationId

                    val convIndex = _conversations.indexOfFirst { it.id == resolvedConversationId }
                    if (convIndex != -1 && _conversations[convIndex].title.isBlank()) {
                        _conversations[convIndex] = _conversations[convIndex].copy(
                            title = question.take(48),
                            autoTitleApplied = true
                        )
                    }

                    appendAiMessage(resolvedConversationId, response.answer, response.messageId)
                    persistChatState()
                    logTurn(turnId, "ai_reply_appended", resolvedConversationId, "source=chat_ask_image")

                    uploadTerminalStatus = "COMPLETED"
                } catch (e: HttpException) {
                    if (processingMessageId.isNotBlank()) {
                        val index = _conversations.indexOfFirst { it.id == conversationForImage }
                        if (index != -1) {
                            val updatedConv = _conversations[index]
                            updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                            _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
                        }
                    }
                    uploadTerminalStatus = "FAILED"
                    handleHttpError(e, forceLogoutOn401 = false)
                    logTurn(turnId, "turn_http_error", conversationForImage, "code=${e.code()}")
                } catch (e: Exception) {
                    if (processingMessageId.isNotBlank()) {
                        val index = _conversations.indexOfFirst { it.id == conversationForImage }
                        if (index != -1) {
                            val updatedConv = _conversations[index]
                            updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                            _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages.toMutableList())
                        }
                    }
                    uploadTerminalStatus = "FAILED"
                    errorMessage = e.localizedMessage ?: "Failed to process image question."
                    logTurn(turnId, "turn_error", conversationForImage, e.javaClass.simpleName)
                } finally {
                    uploadStatusLabel = null
                    isUploading = false
                }
            }
        }
    }

    fun loadImageForMessage(messageId: String, callback: (imageBase64: String?, mimeType: String?, originalName: String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getMessageImage(messageId)
                callback(response.base64, response.mimeType, response.originalName)
            } catch (e: Exception) {
                callback(null, null, null)
            }
        }
    }

    fun setDebugAccountIdentity(accountIdentifier: String) {
        openedFromFreshLogin = true
        debugAccountKeyHashForLogs = hashAccountIdentity(accountIdentifier)
    }

    fun markNewChatLandingAfterLogin() {
        openedFromFreshLogin = true
        activeConversationId = ""
        activeDocumentId = null
        _currentChatType.value = ChatScreenType.NEW_CHAT
    }

    fun getConversationStudyPlans(conversationId: String): List<ConversationStudyPlanItem> {
        return conversationStudyPlansById[conversationId] ?: emptyList()
    }

    fun consumeSuccessMessage() {
        successMessage = null
    }

    fun consumeErrorMessage() {
        errorMessage = null
    }


    private fun hashAccountIdentity(accountIdentifier: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(accountIdentifier.toByteArray())
            java.util.Formatter().use { formatter ->
                hash.forEach { formatter.format("%02x", it) }
                formatter.toString().take(12)
            }
        } catch (e: Exception) {
            "unknown_account"
        }
    }
}

data class ConversationStudyPlanLessonItem(
    val lessonId: String,
    val title: String,
    val documentId: String,
    val order: Int
)

data class ConversationStudyPlanItem(
    val id: String,
    val title: String,
    val lessons: List<ConversationStudyPlanLessonItem>,
    val lessonCount: Int,
    val rawJson: String,
    val createdAt: String?
)

 