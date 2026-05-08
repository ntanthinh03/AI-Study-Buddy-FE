package com.thinh.aistudybuddy.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.thinh.aistudybuddy.data.models.*
import com.thinh.aistudybuddy.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.util.UUID

@Suppress("unused")
object AiAskService {
    private const val TAG = "AiAskService"
    private const val UPLOAD_RETRY_ATTEMPTS = 3
    private const val UPLOAD_RETRY_DELAY_MS = 500L
    private const val CHAT_READY_RETRY_ATTEMPTS = 4
    private const val CHAT_READY_RETRY_DELAY_MS = 1200L

    private val sessions = mutableMapOf<String, AiAskSession>()

    fun getSession(sessionId: String): AiAskSession? = sessions[sessionId]
    fun getAllSessions(): List<AiAskSession> = sessions.values.toList()

    suspend fun directAsk(question: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Asking AI directly: $question")
            val response = RetrofitClient.instance.aiAsk(AiAskRequest(question))
            response.answer
        }.onFailure {
            Log.e(TAG, "Direct ask failed", it)
        }
    }

    suspend fun uploadThenAsk(
        context: Context,
        fileUri: Uri,
        question: String,
        fileDisplayName: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val sessionId = UUID.randomUUID().toString()
        val session = AiAskSession(
            id = sessionId,
            question = question,
            uploadedFileName = fileDisplayName
        )
        sessions[sessionId] = session
        Log.d(TAG, "Starting uploadThenAsk session=$sessionId, question=$question")

        try {
            updateSession(sessionId) { it.copy(state = AskState.UPLOADING) }
            val documentId = uploadDocumentForAsk(context, fileUri, fileDisplayName)
            Log.d(TAG, "Document uploaded: documentId=$documentId")

            updateSession(sessionId) { it.copy(documentId = documentId, state = AskState.PROCESSING) }

            val answer = askAgainstDocument(sessionId, documentId, question)
            if (answer.isBlank()) {
                return@withContext Result.failure(Exception("AI returned an empty response"))
            }

            Result.success(sessionId)
        } catch (e: Exception) {
            Log.e(TAG, "uploadThenAsk failed for session=$sessionId", e)
            updateSession(sessionId) {
                it.copy(
                    state = AskState.ERROR,
                    errorMessage = e.localizedMessage ?: "Upload failed"
                )
            }
            Result.failure(e)
        }
    }

    suspend fun retryAsk(sessionId: String, newQuestion: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val session = getSession(sessionId) ?: return@withContext Result.failure(Exception("Session not found"))
        val question = newQuestion ?: session.question
        val documentId = session.documentId

        Log.d(TAG, "Retrying ask for session=$sessionId, documentId=$documentId, newQuestion=$newQuestion")

        if (documentId.isNullOrBlank()) {
            return@withContext Result.failure(Exception("No document in session"))
        }

        try {
            updateSession(sessionId) {
                it.copy(
                    state = AskState.PROCESSING,
                    question = question,
                    retryCount = it.retryCount + 1,
                    errorMessage = null
                )
            }

            val answer = askAgainstDocument(sessionId, documentId, question)
            if (answer.isBlank()) {
                return@withContext Result.failure(Exception("AI returned an empty response"))
            }

            val currentSession = getSession(sessionId)
            if (currentSession?.state == AskState.COMPLETED) {
                return@withContext Result.success(sessionId)
            }

            Result.failure(Exception("Document not ready: ${currentSession?.state}"))
        } catch (e: Exception) {
            Log.e(TAG, "retryAsk failed", e)
            updateSession(sessionId) {
                it.copy(
                    state = AskState.ERROR,
                    errorMessage = e.localizedMessage
                )
            }
            Result.failure(e)
        }
    }

    suspend fun getSessionAnswer(sessionId: String): Result<String> = withContext(Dispatchers.IO) {
        val session = getSession(sessionId)
            ?: return@withContext Result.failure(Exception("Session not found"))

        if (session.state != AskState.COMPLETED) {
            return@withContext Result.failure(Exception("Session not completed. State: ${session.state}"))
        }

        Result.success(session.answer)
    }

    private suspend fun uploadDocumentForAsk(
        context: Context,
        uri: Uri,
        displayName: String?
    ): String = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = displayName ?: queryDisplayName(contentResolver, uri) ?: "upload.${guessExtension(mimeType)}"

        val supportedFile = mimeType.startsWith("image/") ||
            mimeType == "application/pdf" ||
            fileName.endsWith(".pdf", ignoreCase = true)
        if (!supportedFile) {
            throw IllegalArgumentException("Unsupported file type. Please upload a PDF or image.")
        }

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not read selected file")

        Log.d(TAG, "Uploading document for ask: filename=$fileName, mimeType=$mimeType, size=${bytes.size}")

        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, body)

        var lastError: Throwable? = null
        repeat(UPLOAD_RETRY_ATTEMPTS) { attempt ->
            try {
                val response = RetrofitClient.instance.uploadDocument(part)
                val documentId = response.id.trim()
                if (documentId.isBlank()) {
                    throw IllegalStateException("Upload succeeded but response document id is empty")
                }
                return@withContext documentId
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Upload attempt ${attempt + 1} failed", e)
                if (attempt < UPLOAD_RETRY_ATTEMPTS - 1) {
                    delay(UPLOAD_RETRY_DELAY_MS)
                }
            }
        }

        throw lastError ?: Exception("Upload failed")
    }

    private suspend fun askAgainstDocument(
        sessionId: String,
        documentId: String,
        question: String
    ): String = withContext(Dispatchers.IO) {
        val response = askWithReadyRetry(documentId, question)
        val answer = response.answer.trim()
        if (answer.isBlank()) {
            throw Exception("AI returned an empty response")
        }

        updateSession(sessionId) {
            it.copy(
                question = question,
                answer = answer,
                state = AskState.COMPLETED,
                errorMessage = null
            )
        }
        answer
    }

    private suspend fun askWithReadyRetry(documentId: String, question: String): ChatResponse {
        var lastError: Throwable? = null
        repeat(CHAT_READY_RETRY_ATTEMPTS) { attempt ->
            try {
                return RetrofitClient.instance.sendQuestion(documentId, DocumentChatRequest(question))
            } catch (e: Exception) {
                val shouldRetry = isTransientNotReadyError(e)
                if (!shouldRetry || attempt == CHAT_READY_RETRY_ATTEMPTS - 1) {
                    throw e
                }
                lastError = e
                delay(CHAT_READY_RETRY_DELAY_MS * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Failed to ask against document")
    }

    private fun isTransientNotReadyError(error: Exception): Boolean {
        val message = error.localizedMessage.orEmpty().lowercase()
        if (error is HttpException) {
            if (error.code() in listOf(409, 423, 425, 429, 503)) return true
        }
        return message.contains("processing") ||
            message.contains("not ready") ||
            message.contains("try again")
    }

    private fun updateSession(sessionId: String, update: (AiAskSession) -> AiAskSession) {
        val current = sessions[sessionId] ?: return
        sessions[sessionId] = update(current)
        Log.d(TAG, "Session updated: sessionId=$sessionId, state=${sessions[sessionId]?.state}")
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
    }

    private fun guessExtension(mimeType: String): String = when {
        mimeType.contains("pdf", ignoreCase = true) -> "pdf"
        mimeType.startsWith("image/") -> mimeType.substringAfter("image/", "jpg")
        else -> "bin"
    }

    fun clearSession(sessionId: String) {
        sessions.remove(sessionId)
        Log.d(TAG, "Session cleared: $sessionId")
    }

    fun clearAllSessions() {
        sessions.clear()
        Log.d(TAG, "All sessions cleared")
    }
}

