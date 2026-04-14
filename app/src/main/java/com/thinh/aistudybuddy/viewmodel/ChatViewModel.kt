package com.thinh.aistudybuddy.viewmodel

import android.content.Context
import android.net.Uri
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
import com.thinh.aistudybuddy.data.AiAskService
import com.thinh.aistudybuddy.data.AiAskRequest
import com.thinh.aistudybuddy.data.AskAiRequest
import com.thinh.aistudybuddy.data.AskState
import com.thinh.aistudybuddy.data.ChatMessage as BackendChatMessage
import com.thinh.aistudybuddy.data.ConversationMessage as BackendConversationMessage
import com.thinh.aistudybuddy.data.DocumentChatRequest
import com.thinh.aistudybuddy.data.Document
import com.thinh.aistudybuddy.data.QuizQuestion as ApiQuizQuestion
import com.thinh.aistudybuddy.data.SaveDocumentArtifactRequest
import com.thinh.aistudybuddy.data.local.LocalHistoryStore
import com.thinh.aistudybuddy.data.network.RetrofitClient
import com.thinh.aistudybuddy.data.model.Banner
import com.thinh.aistudybuddy.data.model.ChatMessage
import com.thinh.aistudybuddy.data.model.Conversation
import com.thinh.aistudybuddy.data.model.ConversationKind
import com.thinh.aistudybuddy.data.model.QuizQuestion
import com.thinh.aistudybuddy.data.model.Suggestion
import com.thinh.aistudybuddy.data.model.StudyPlanJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        private const val DOCUMENT_STATUS_POLL_INTERVAL_MS = 2000L
        private const val DOCUMENT_STATUS_MAX_POLLS = 180
    }

    private class DocumentProcessingTimeoutException(message: String) : IllegalStateException(message)

    private val _conversations = mutableStateListOf<Conversation>()
    val conversations: List<Conversation> get() = _conversations

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
    var pendingPdfName by mutableStateOf<String?>(null)
        private set
    private var pendingPdfUri: Uri? = null
    var uploadStatusLabel by mutableStateOf<String?>(null)
        private set
    var uploadTerminalStatus by mutableStateOf<String?>(null)
        private set

    private var currentAskSessionId by mutableStateOf<String?>(null)
    var aiAskState by mutableStateOf<AskState>(AskState.IDLE)
        private set

    private var _currentChatType = mutableStateOf(ChatScreenType.NEW_CHAT)
    val currentChatType: ChatScreenType get() = _currentChatType.value
    private val quizKeywords = listOf("quiz", "multiple choice", "mcq", "test me", "practice", "practice questions")
    private val planKeywords = listOf("study plan", "plan", "roadmap", "learning path", "course plan", "learning plan")

    private val _suggestions = mutableStateListOf<Suggestion>()
    val suggestions: List<Suggestion> get() = _suggestions

    private val _banner = mutableStateOf<Banner?>(null)
    val banner: Banner? get() = _banner.value

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
        val documentId = activeDocumentId
        if (documentId != null) {
            sendPersistedDocumentMessage(documentId, text)
            return
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
            generateAiReply(newConvId, text)
        } else {
             val index = _conversations.indexOfFirst { it.id == activeConversationId }
             if (index != -1) {
                 val updatedConv = _conversations[index]
                 updatedConv.chatMessages.add(ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = true))
                 _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                 persistChatState()
                 generateAiReply(activeConversationId, text)
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

    fun sendMessageWithPendingPdf(context: Context, text: String) {
        val pdfUri = pendingPdfUri ?: return
        val pdfName = pendingPdfName ?: "attachment.pdf"
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

        viewModelScope.launch {
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

            // Show animated processing message while reading/summarizing
            val processingMessageId = appendProcessingMessage(conversationId, "Reading document...")
            persistChatState()

            try {
                val uploaded = uploadDocument(context, pdfUri, pdfName)
                
                // Update processing message to "Summarizing..."
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

                // Remove processing message and append actual response
                val index = _conversations.indexOfFirst { it.id == conversationId }
                if (index != -1) {
                    val updatedConv = _conversations[index]
                    updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                    _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                }
                persistChatState()

                val response = RetrofitClient.instance.sendQuestion(finalizedDocument.id, DocumentChatRequest(text))
                appendAiMessage(conversationId, response.answer)
                clearPendingPdf()
                persistChatState()
            } catch (e: HttpException) {
                // Remove processing message on error
                val index = _conversations.indexOfFirst { it.id == conversationId }
                if (index != -1) {
                    val updatedConv = _conversations[index]
                    updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                    _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                }
                uploadTerminalStatus = "FAILED"
                handleHttpError(e)
            } catch (e: DocumentProcessingTimeoutException) {
                // Remove processing message on timeout
                val index = _conversations.indexOfFirst { it.id == conversationId }
                if (index != -1) {
                    val updatedConv = _conversations[index]
                    updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                    _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                }
                uploadTerminalStatus = "SKIPPED"
                errorMessage = e.message
            } catch (e: Exception) {
                // Remove processing message on error
                val index = _conversations.indexOfFirst { it.id == conversationId }
                if (index != -1) {
                    val updatedConv = _conversations[index]
                    updatedConv.chatMessages.removeIf { it.id == processingMessageId }
                    _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                }
                uploadTerminalStatus = "FAILED"
                errorMessage = e.localizedMessage ?: "Failed to send PDF with message."
            } finally {
                uploadStatusLabel = null
                isTyping = false
                isUploading = false
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
            
            // Validate file is an image BEFORE reading bytes
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
            
            val documentId = response.documentId ?: UUID.randomUUID().toString()
            
            // Create a minimal Document object from RAG response
            // The full document will be fetched and updated during waitForUploadPipeline
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
                val index = _conversations.indexOfFirst { it.documentId == documentId }
                if (index != -1) {
                    val mappedHistory = history.toUiMessages()
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
            } catch (e: Exception) {
                if (e is HttpException) handleHttpError(e)
            }
        }
    }

    private fun sendPersistedDocumentMessage(documentId: String, text: String) {
        viewModelScope.launch {
            ensureConversationForDocument(documentId)
            val index = _conversations.indexOfFirst { conversation -> conversation.documentId == documentId }
            if (index == -1) return@launch

            val convId = _conversations[index].id
            appendUserMessage(convId, text)
            persistChatState()
            isTyping = true

            try {
                if (isQuizIntent(text)) {
                    handleQuizIntent(convId, text)
                    return@launch
                }

                if (isPlanIntent(text)) {
                    handlePlanIntent(convId, text, documentId)
                    return@launch
                }

                val response = RetrofitClient.instance.sendQuestion(documentId, DocumentChatRequest(text))
                appendAiMessage(convId, response.answer)
                persistChatState()
                maybeAutoRenameConversation(convId, text, response.answer)
            } catch (e: HttpException) {
                handleHttpError(e)
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to send message."
            } finally {
                isTyping = false
            }
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

    private fun appendUserMessage(convId: String, text: String, attachmentName: String? = null) {
        val index = _conversations.indexOfFirst { it.id == convId }
        if (index == -1) return
        val updatedConv = _conversations[index]
        updatedConv.chatMessages.add(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = text,
                isUser = true,
                attachmentName = attachmentName
            )
        )
        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
    }

    private fun appendAiMessage(convId: String, text: String, messageId: String? = null) {
        val index = _conversations.indexOfFirst { it.id == convId }
        if (index == -1) return
        val updatedConv = _conversations[index]
        updatedConv.chatMessages.add(
            ChatMessage(
                id = messageId ?: UUID.randomUUID().toString(),
                text = text,
                isUser = false
            )
        )
        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
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
        _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
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
            _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
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

    private fun generateAiReply(convId: String, userMessage: String) {
        viewModelScope.launch {
            isTyping = true
            val conversationIndex = _conversations.indexOfFirst { it.id == convId }
            if (conversationIndex == -1) {
                isTyping = false
                return@launch
            }

            if (isQuizIntent(userMessage)) {
                handleQuizIntent(convId, userMessage)
                isTyping = false
                return@launch
            }

            if (isPlanIntent(userMessage)) {
                handlePlanIntent(convId, userMessage, activeDocumentId)
                isTyping = false
                return@launch
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

                val lowerMsg = userMessage.lowercase()
                val showQuiz = lowerMsg.contains("quiz")
                val showPlan = lowerMsg.contains("plan")

                val resolvedIndex = _conversations.indexOfFirst { it.id == resolvedConversationId }
                if (resolvedIndex == -1) {
                    isTyping = false
                    return@launch
                }
                val updatedConv = _conversations[resolvedIndex]
                updatedConv.chatMessages.add(
                    ChatMessage(
                        id = askResponse.messageId,
                        text = aiResponse,
                        isUser = false,
                        showQuizButton = showQuiz,
                        showStudyPlanButton = showPlan
                    )
                )
                _conversations[resolvedIndex] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                persistChatState()
                maybeAutoRenameConversation(
                    resolvedConversationId,
                    askResponse.question.ifBlank { userMessage },
                    aiResponse
                )
            } catch (e: HttpException) {
                handleHttpError(e, forceLogoutOn401 = false)
            } catch (_: SocketTimeoutException) {
                errorMessage = "The AI is taking longer than expected. Please try again."
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to get AI response."
            }
            isTyping = false
        }
    }

    private suspend fun handleQuizIntent(convId: String, userMessage: String) {
        try {
            val quizQuestions = generateQuizQuestions(userMessage)
            if (quizQuestions.isEmpty()) {
                appendAiMessage(convId, "Could not generate quiz at the moment. Please try again.")
                persistChatState()
                return
            }

            onQuizGenerated?.invoke(quizQuestions)
            persistArtifactToBackend(
                convId = convId,
                artifactType = "QUIZ",
                artifact = Gson().toJsonTree(quizQuestions),
                note = "Quiz generated from chat"
            )
            updateConversationKind(convId, ConversationKind.QUIZ)
            val index = _conversations.indexOfFirst { it.id == convId }
            if (index != -1) {
                val updatedConv = _conversations[index]
                updatedConv.chatMessages.add(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "Quiz is ready. Tap Start Quiz to begin.",
                        isUser = false,
                        showQuizButton = true
                    )
                )
                _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                persistChatState()
                maybeAutoRenameConversation(convId, userMessage, "Quiz is ready. Tap Start Quiz to begin.")
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
            persistArtifactToBackend(
                convId = convId,
                artifactType = "STUDY_PLAN",
                artifact = JsonParser().parse(planJson),
                note = "Study plan generated from chat"
            )
            updateConversationKind(convId, ConversationKind.PLAN)
            val index = _conversations.indexOfFirst { it.id == convId }
            if (index != -1) {
                val updatedConv = _conversations[index]
                updatedConv.chatMessages.add(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "Plan is ready. Tap Check Plan to view course lessons.",
                        isUser = false,
                        showStudyPlanButton = true
                    )
                )
                _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                persistChatState()
                maybeAutoRenameConversation(convId, userMessage, "Plan is ready. Tap Check Plan to view course lessons.")
            }
        } catch (e: HttpException) {
            handleHttpError(e, forceLogoutOn401 = false)
        } catch (_: SocketTimeoutException) {
            errorMessage = "Study plan generation timed out. Please try again."
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Could not generate study plan at the moment."
        }
    }

    private suspend fun generateQuizQuestions(userMessage: String): List<QuizQuestion> {
        val documentId = activeDocumentId
        if (!documentId.isNullOrBlank() && !RetrofitClient.authToken.isNullOrBlank()) {
            val generated = RetrofitClient.instance.generateQuiz(documentId)
            return generated.toUiQuizQuestions()
        }

        val hiddenPrompt = buildString {
            append("Create a quiz in valid JSON only. ")
            append("Schema: {\"questions\":[{\"id\":1,\"text\":\"Question\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":\"A\",\"explanation\":\"...\"}]}. ")
            append("Rules: Output English only, no markdown, no code fences, exactly 4 options. ")
            append("User request: ")
            append(userMessage)
        }
        val response = RetrofitClient.instance.aiAsk(AiAskRequest(hiddenPrompt)).answer
        return parseStructuredQuiz(response)
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
            append("Schema: {\"planId\":\"...\",\"title\":\"...\",\"overview\":\"...\",\"estimatedTotalMinutes\":120,\"modules\":[{\"moduleId\":\"m1\",\"order\":1,\"documentId\":\"doc-001\",\"title\":\"...\",\"objective\":\"...\",\"estimatedMinutes\":30,\"difficulty\":\"BEGINNER\",\"status\":\"IN_PROGRESS\",\"quiz\":{\"recommendedQuestionCount\":5,\"passScore\":70}}]}. ")
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

    private fun updateConversationKind(convId: String, kind: ConversationKind) {
        val index = _conversations.indexOfFirst { it.id == convId }
        if (index == -1) return
        val updated = _conversations[index]
        _conversations[index] = updated.copy(
            kind = kind,
            isQuiz = kind == ConversationKind.QUIZ
        )
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

    private fun parseStructuredQuiz(raw: String): List<QuizQuestion> {
        val cleaned = raw
            .replace("```json", "")
            .replace("```", "")
            .trim()
        val root = runCatching { JsonParser().parse(cleaned) }.getOrNull() ?: return emptyList()
        val questionsArray = when (root) {
            is JsonArray -> root
            is JsonObject -> root.getAsJsonArray("questions")
            else -> null
        } ?: return emptyList()

        return questionsArray.mapIndexedNotNull { idx, element ->
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
        if (selected.autoTitleApplied) {
            refreshConversationMessages(id)
        }
        persistChatState()
    }

    fun refreshConversationMessages(conversationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messages = RetrofitClient.instance.getConversationMessages(conversationId)
                val mappedHistory = messages.toBackendHistory().toUiMessages()
                val index = _conversations.indexOfFirst { it.id == conversationId }
                if (index != -1) {
                    val inferredKind = when {
                        mappedHistory.any { it.showStudyPlanButton } -> ConversationKind.PLAN
                        mappedHistory.any { it.showQuizButton } -> ConversationKind.QUIZ
                        else -> ConversationKind.CHAT
                    }
                    
                    // Load images for messages that have image metadata
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
                val existingMessagesByConversationId = _conversations.associate { it.id to it.chatMessages.toMutableList() }
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
                _conversations.clear()
                _conversations.addAll(conversations)

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

                // Hydrate currently active conversation from backend so reload shows messages, not only title.
                if (activeConversationId.isNotBlank()) {
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
        }
    }
    fun deleteConversation(id: String) {
        _conversations.removeIf { it.id == id }
        if (activeConversationId == id) startNewChat() else persistChatState()
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
                RetrofitClient.authToken = null
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
                val result = AiAskService.uploadThenAsk(context, fileUri, question, fileName)
                result.onSuccess { sessionId ->
                    currentAskSessionId = sessionId
                    aiAskState = AskState.COMPLETED
                    uploadTerminalStatus = "COMPLETED"

                    val answerResult = AiAskService.getSessionAnswer(sessionId)
                    answerResult.onSuccess { answer ->
                        appendAiMessage(convId, answer)
                        persistChatState()
                        maybeAutoRenameConversation(convId, question, answer)
                    }.onFailure { error ->
                        errorMessage = error.localizedMessage ?: "Failed to retrieve answer"
                        aiAskState = AskState.ERROR
                    }
                }.onFailure { error ->
                    uploadTerminalStatus = "FAILED"
                    errorMessage = error.localizedMessage ?: "Upload and ask failed"
                    aiAskState = AskState.ERROR
                }
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
            errorMessage = "No active session to retry"
            return
        }

        viewModelScope.launch {
            isTyping = true
            aiAskState = AskState.PROCESSING
            errorMessage = null
            val titleSourceQuestion = newQuestion?.takeIf { it.isNotBlank() }
                ?: activeMessages.lastOrNull { it.isUser }?.text.orEmpty()

            val convId = activeConversationId.takeIf { it.isNotBlank() }
                ?: ensureConversationForDirectAsk()

            if (newQuestion != null && newQuestion.isNotBlank()) {
                appendUserMessage(convId, newQuestion)
            }
            persistChatState()

            try {
                val result = AiAskService.retryAsk(sessionId, newQuestion)
                result.onSuccess {
                    aiAskState = AskState.COMPLETED
                    val answerResult = AiAskService.getSessionAnswer(sessionId)
                    answerResult.onSuccess { answer ->
                        appendAiMessage(convId, answer)
                        persistChatState()
                        maybeAutoRenameConversation(convId, titleSourceQuestion, answer)
                    }.onFailure { error ->
                        errorMessage = error.localizedMessage
                        aiAskState = AskState.ERROR
                    }
                }.onFailure { error ->
                    errorMessage = error.localizedMessage ?: "Retry failed"
                    aiAskState = AskState.ERROR
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Retry failed"
                aiAskState = AskState.ERROR
            } finally {
                isTyping = false
            }
        }
    }

    fun getCurrentAskSessionState(): AskState = aiAskState

    @Suppress("unused")
    fun clearAskSession() {
        currentAskSessionId?.let { AiAskService.clearSession(it) }
        currentAskSessionId = null
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

    private fun List<BackendChatMessage>.toUiMessages(): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        forEach { item ->
            val messageType = item.messageType.orEmpty().trim().uppercase()
            val artifactType = item.artifactType.orEmpty().trim().uppercase()

            if (messageType == "ARTIFACT") {
                when (artifactType) {
                    "QUIZ" -> messages.add(
                        ChatMessage(
                            id = "hist-artifact-quiz-${item.id}",
                            text = "Quiz is ready. Tap Start Quiz to begin.",
                            isUser = false,
                            showQuizButton = true
                        )
                    )
                    "STUDY_PLAN" -> messages.add(
                        ChatMessage(
                            id = "hist-artifact-plan-${item.id}",
                            text = "Plan is ready. Tap Check Plan to view course lessons.",
                            isUser = false,
                            showStudyPlanButton = true
                        )
                    )
                }
                return@forEach
            }

            if (item.question.isNotBlank()) {
                messages.add(
                    ChatMessage(
                        id = "hist-user-${item.id}",
                        text = item.question,
                        isUser = true,
                        imageMimeType = item.imageMimeType,
                        imageOriginalName = item.imageOriginalName
                    )
                )
            }
            if (item.answer.isNotBlank()) {
                messages.add(
                    ChatMessage(
                        id = "hist-ai-${item.id}",
                        text = item.answer,
                        isUser = false
                    )
                )
            }
        }
        return messages
    }

    private fun List<BackendConversationMessage>.toBackendHistory(): List<BackendChatMessage> {
        return map { item ->
            BackendChatMessage(
                id = item.id,
                question = item.question.orEmpty(),
                answer = item.answer.orEmpty(),
                createdAt = item.createdAt.orEmpty(),
                messageType = item.messageType,
                artifactType = item.artifactType,
                artifactJson = item.artifactJson,
                imageMimeType = item.imageMimeType,
                imageOriginalName = item.imageOriginalName
            )
        }
    }

    fun sendImageQuestion(context: Context, imageUri: Uri, question: String, conversationId: String? = null) {
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
            isUploading = true
            isTyping = false
            uploadStatusLabel = "Processing image..."
            uploadTerminalStatus = null
            errorMessage = null

            try {
                val conversationForImage = conversationId ?: if (_currentChatType.value == ChatScreenType.NEW_CHAT) {
                    val newConvId = UUID.randomUUID().toString()
                    _conversations.add(0, Conversation(newConvId, "", autoTitleApplied = false))
                    activeConversationId = newConvId
                    _currentChatType.value = ChatScreenType.CONTINUING_CHAT
                    newConvId
                } else {
                    activeConversationId
                }

                // Append user message with image info
                appendUserMessage(conversationForImage, question, "image")
                persistChatState()

                // Read image file
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
                val fileName = runCatching {
                    contentResolver.query(imageUri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                    }
                }.getOrNull() ?: "image.jpg"

                val bytes = contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Could not read image file.")

                // Call API with multipart
                val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", fileName, body)
                val questionBody = question.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.instance.askAIWithImage(
                    image = imagePart,
                    question = questionBody,
                    conversationId = conversationForImage.toRequestBody("text/plain".toMediaTypeOrNull()),
                    title = question.take(48).toRequestBody("text/plain".toMediaTypeOrNull())
                )

                // Update conversation if created
                val convIndex = _conversations.indexOfFirst { it.id == response.conversationId }
                if (convIndex != -1 && _conversations[convIndex].title.isBlank()) {
                    _conversations[convIndex] = _conversations[convIndex].copy(
                        title = question.take(48),
                        autoTitleApplied = true
                    )
                }

                // Append AI response
                appendAiMessage(response.conversationId, response.answer)
                persistChatState()

                uploadTerminalStatus = "COMPLETED"
            } catch (e: HttpException) {
                uploadTerminalStatus = "FAILED"
                handleHttpError(e, forceLogoutOn401 = false)
            } catch (e: Exception) {
                uploadTerminalStatus = "FAILED"
                errorMessage = e.localizedMessage ?: "Failed to process image question."
            } finally {
                uploadStatusLabel = null
                isUploading = false
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
}

