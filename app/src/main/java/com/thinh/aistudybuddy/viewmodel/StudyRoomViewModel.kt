package com.thinh.aistudybuddy.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.thinh.aistudybuddy.services.network.RetrofitClient
import com.thinh.aistudybuddy.data.models.*
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class PlayerRanking(val username: String, val score: Int)

enum class QuizGenerationStatus {
    IDLE,
    GENERATING,
    READY,
    ERROR
}

sealed class StudyRoomUiState {
    object Initial : StudyRoomUiState()
    data class InLobby(val roomCode: String, val participants: List<String>, val isHost: Boolean = false) : StudyRoomUiState()
    data class SelectingMaterial(val roomCode: String) : StudyRoomUiState()
    data class WaitingForHost(val roomCode: String, val message: String) : StudyRoomUiState()
    data class FocusActive(val roomCode: String, val durationMinutes: Int, val startedBy: String) : StudyRoomUiState()
    data class QuizActive(
        val roomCode: String,
        val questions: List<QuizQuestion>,
        val currentIndex: Int,
        val rankings: List<PlayerRanking>,
        val endsAt: Long = 0,
        val answeredCount: Int = 0,
        val totalPlayers: Int = 0,
        val isHost: Boolean = false,
        val isGeneratingRemaining: Boolean = false
    ) : StudyRoomUiState()
    data class QuizCountdown(
        val roomCode: String,
        val questions: List<QuizQuestion>,
        val endsAt: Long = 0,
        val isHost: Boolean = false
    ) : StudyRoomUiState()
    data class QuizSummary(
        val roomCode: String,
        val questions: List<QuizQuestion>,
        val rankings: List<PlayerRanking>,
        val userAnswers: Map<Int, String>,
        val isHost: Boolean = false
    ) : StudyRoomUiState()
    data class Error(val message: String) : StudyRoomUiState()
}

class StudyRoomViewModel : ViewModel() {
    private var socket: Socket? = null
    private val api = RetrofitClient.instance

    private val _uiState = MutableStateFlow<StudyRoomUiState>(StudyRoomUiState.Initial)
    val uiState: StateFlow<StudyRoomUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _isLoadingDocs = MutableStateFlow(false)
    val isLoadingDocs: StateFlow<Boolean> = _isLoadingDocs.asStateFlow()

    private val _isPreparingQuiz = MutableStateFlow(false)
    val isPreparingQuiz: StateFlow<Boolean> = _isPreparingQuiz.asStateFlow()

    private val _selectedDocumentName = MutableStateFlow<String?>(null)
    val selectedDocumentName: StateFlow<String?> = _selectedDocumentName.asStateFlow()

    private val _generationStatus = MutableStateFlow(QuizGenerationStatus.IDLE)
    val generationStatus: StateFlow<QuizGenerationStatus> = _generationStatus.asStateFlow()

    private var selectedDocument: Document? = null
    private val preGeneratedQuestions = mutableListOf<BackendQuizQuestion>()
    private var isStartRequested = false
    private var quizGenerationJob: kotlinx.coroutines.Job? = null

    private var myUsername: String = ""
    private val roomParticipants = mutableListOf<String>()
    private val userAnswers = mutableMapOf<Int, String>()

    init {
        setupSocket()
        loadDocuments()
    }

    private fun setupSocket() {
        try {
            val opts = IO.Options()
            opts.forceNew = true
            opts.reconnection = true
            
            val baseUrl = RetrofitClient.getBaseUrl().replace("/api/", "")
            socket = IO.socket("${baseUrl}study-rooms", opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("StudyRoomVM", "Connected to socket")
            }

            socket?.on("userJoined") { args ->
                val data = args[0] as JSONObject
                val username = data.getString("username")
                _messages.value = _messages.value + "$username joined the room"
                
                val partJson = data.optJSONArray("participants")
                val participants = mutableListOf<String>()
                if (partJson != null) {
                    for (i in 0 until partJson.length()) {
                        participants.add(partJson.getString(i))
                    }
                } else {
                    participants.add(username)
                }
                
                roomParticipants.clear()
                roomParticipants.addAll(participants)
                
                val current = _uiState.value
                if (current is StudyRoomUiState.InLobby) {
                    _uiState.value = current.copy(participants = participants)
                }
            }

            socket?.on("userLeft") { args ->
                val data = args[0] as JSONObject
                val username = data.getString("username")
                _messages.value = _messages.value + "$username left the room"
                
                val partJson = data.optJSONArray("participants")
                val participants = mutableListOf<String>()
                if (partJson != null) {
                    for (i in 0 until partJson.length()) {
                        participants.add(partJson.getString(i))
                    }
                }
                
                roomParticipants.clear()
                roomParticipants.addAll(participants)
                
                val current = _uiState.value
                if (current is StudyRoomUiState.InLobby) {
                    _uiState.value = current.copy(participants = participants)
                }
            }

            socket?.on("materialSelectionStarted") { args ->

            }

            socket?.on("quizPreparing") { args ->
                _isPreparingQuiz.value = true
            }

            socket?.on("hostSelectedMaterial") { args ->
                val data = args[0] as JSONObject
                val fileName = data.getString("fileName")
                _selectedDocumentName.value = fileName
            }

            socket?.on("quizStarted") { args ->
                _isPreparingQuiz.value = false
                _selectedDocumentName.value = null
                val data = args[0] as JSONObject
                val roomCode = data.optString("roomCode", "")
                val questionsJson = data.getJSONArray("questions")
                val endsAt = data.optLong("endsAt", 0)
                val questions = parseQuestions(questionsJson)
                
                val current = _uiState.value
                val isHost = if (current is StudyRoomUiState.InLobby) current.isHost 
                             else if (current is StudyRoomUiState.SelectingMaterial) true
                             else if (current is StudyRoomUiState.WaitingForHost) false
                             else false

                userAnswers.clear()

                _uiState.value = StudyRoomUiState.QuizCountdown(
                    roomCode, questions, endsAt = endsAt, isHost = isHost
                )
            }

            socket?.on("questionsUpdated") { args ->
                
            }

            socket?.on("waitingForQuestions") { args ->
                _isPreparingQuiz.value = true
            }

            socket?.on("newQuestion") { args ->
                try {
                    val data = args[0] as JSONObject
                    val nextIndex = data.getInt("index")
                    val endsAt = data.optLong("endsAt", 0)
                    val qObj = data.optJSONObject("question")
                    

                    _isPreparingQuiz.value = false
                    
                    val current = _uiState.value
                    if (current is StudyRoomUiState.QuizActive) {
                        val updatedQuestions = current.questions.toMutableList()
                        if (qObj != null) {
                            val parsedQ = parseSingleQuestion(qObj, nextIndex)
                            if (nextIndex < updatedQuestions.size) {
                                updatedQuestions[nextIndex] = parsedQ
                            } else {
                                while (updatedQuestions.size <= nextIndex) {
                                    if (updatedQuestions.size == nextIndex) {
                                        updatedQuestions.add(parsedQ)
                                    } else {
                                        updatedQuestions.add(QuizQuestion("", "", emptyList(), 0, ""))
                                    }
                                }
                            }
                        }
                        
                        _uiState.value = current.copy(
                            questions = updatedQuestions,
                            currentIndex = nextIndex, 
                            endsAt = endsAt,
                            answeredCount = 0
                        )
                    }
                } catch (e: Exception) {
                    Log.e("StudyRoomVM", "Error in 'newQuestion' listener: ${e.message}", e)
                }
            }

            socket?.on("answerProgress") { args ->
                val data = args[0] as JSONObject
                val answered = data.getInt("answeredCount")
                val total = data.getInt("totalCount")
                val current = _uiState.value
                if (current is StudyRoomUiState.QuizActive) {
                    _uiState.value = current.copy(answeredCount = answered, totalPlayers = total)
                }
            }

            socket?.on("leaderboardUpdate") { args ->
                val data = args[0] as JSONArray
                val rankings = mutableListOf<PlayerRanking>()
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    rankings.add(PlayerRanking(obj.getString("username"), obj.getInt("score")))
                }
                val current = _uiState.value
                if (current is StudyRoomUiState.QuizActive) {
                    _uiState.value = current.copy(rankings = rankings)
                }
            }

            socket?.on("quizEnded") { args ->
                _isPreparingQuiz.value = false
                val data = args[0] as JSONObject
                val rankJson = data.optJSONArray("finalRankings")
                val rankings = mutableListOf<PlayerRanking>()
                if (rankJson != null) {
                    for (i in 0 until rankJson.length()) {
                        val obj = rankJson.getJSONObject(i)
                        rankings.add(PlayerRanking(obj.getString("username"), obj.getInt("score")))
                    }
                }
                
                val current = _uiState.value
                if (current is StudyRoomUiState.QuizActive) {
                    _uiState.value = StudyRoomUiState.QuizSummary(
                        roomCode = current.roomCode,
                        questions = current.questions,
                        rankings = rankings,
                        userAnswers = HashMap(userAnswers),
                        isHost = current.isHost
                    )
                } else {
                    _uiState.value = StudyRoomUiState.Initial
                }
            }

            socket?.on("joined") { args ->
                try {
                    Log.d("StudyRoomVM", "Socket event 'joined' received. Args size: ${args?.size}")
                    if (args != null && args.isNotEmpty()) {
                        val firstArg = args[0]
                        Log.d("StudyRoomVM", "Event 'joined' arg: $firstArg")
                        
                        val response = firstArg as? JSONObject
                        if (response != null) {
                            val dataObj = response.optJSONObject("data") ?: response
                            val roomCode = dataObj.getString("roomCode")
                            val isHost = dataObj.optBoolean("isHost", false)
                            val partJson = dataObj.optJSONArray("participants")
                            val participants = mutableListOf<String>()
                            if (partJson != null) {
                                for (i in 0 until partJson.length()) {
                                    participants.add(partJson.getString(i))
                                }
                            } else {
                                participants.add(myUsername)
                            }
                            
                            roomParticipants.clear()
                            roomParticipants.addAll(participants)
                            userAnswers.clear()
                            
                            val selectedDocName = dataObj.optString("selectedDocumentName", "")
                            _selectedDocumentName.value = if (selectedDocName.isEmpty()) null else selectedDocName
                            
                            _uiState.value = StudyRoomUiState.InLobby(roomCode, participants, isHost = isHost)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StudyRoomVM", "Error in 'joined' listener: ${e.message}", e)
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            _uiState.value = StudyRoomUiState.Error("Socket initialization failed")
        }
    }

    private fun parseQuestions(jsonArray: JSONArray): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        for (i in 0 until jsonArray.length()) {
            val q = jsonArray.getJSONObject(i)
            val optArr = q.getJSONObject("options")
            list.add(QuizQuestion(
                id = q.optString("id", "q$i"),
                question = q.getString("question"),
                options = listOf(optArr.getString("A"), optArr.getString("B"), optArr.getString("C"), optArr.getString("D")),
                correctAnswerIndex = when(q.getString("correctAnswer")) { "A" -> 0; "B" -> 1; "C" -> 2; else -> 3 },
                explanation = q.optString("explanation", "")
            ))
        }
        return list
    }

    private fun parseSingleQuestion(q: JSONObject, index: Int): QuizQuestion {
        val optArr = q.getJSONObject("options")
        return QuizQuestion(
            id = q.optString("id", "q$index"),
            question = q.getString("question"),
            options = listOf(optArr.getString("A"), optArr.getString("B"), optArr.getString("C"), optArr.getString("D")),
            correctAnswerIndex = when(q.getString("correctAnswer")) { "A" -> 0; "B" -> 1; "C" -> 2; else -> 3 },
            explanation = q.optString("explanation", "")
        )
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _isLoadingDocs.value = true
            try {
                val docs = api.getDocuments()
                _documents.value = docs.sortedByDescending { it.createdAt ?: "" }
            } catch (e: Exception) { }
            finally { _isLoadingDocs.value = false }
        }
    }

    fun createRoom(username: String) {
        val code = (1..6)
            .map { (('A'..'Z') + ('0'..'9')).random() }
            .joinToString("")
        joinRoom(code, username)
    }

    fun joinRoom(roomCode: String, username: String) {
        myUsername = username
        val data = JSONObject()
        data.put("roomCode", roomCode)
        data.put("username", username)
        
        socket?.emit("joinRoom", data, object : io.socket.client.Ack {
            override fun call(vararg args: Any?) {
                try {
                    Log.d("StudyRoomVM", "joinRoom ACK callback triggered. Args size: ${args?.size}")
                    if (args != null && args.isNotEmpty()) {
                        val firstArg = args[0]
                        Log.d("StudyRoomVM", "First arg: $firstArg")
                        
                        val response = firstArg as? JSONObject
                        if (response != null) {

                            val dataObj = response.optJSONObject("data") ?: response
                            
                            val isHost = dataObj.optBoolean("isHost", false)
                            val partJson = dataObj.optJSONArray("participants")
                            val participants = mutableListOf<String>()
                            if (partJson != null) {
                                for (i in 0 until partJson.length()) {
                                    participants.add(partJson.getString(i))
                                }
                            } else {
                                participants.add(username)
                            }
                            
                            roomParticipants.clear()
                            roomParticipants.addAll(participants)
                            userAnswers.clear()
                            
                            val selectedDocName = dataObj.optString("selectedDocumentName", "")
                            _selectedDocumentName.value = if (selectedDocName.isEmpty()) null else selectedDocName
                            
                            _uiState.value = StudyRoomUiState.InLobby(roomCode, participants, isHost = isHost)
                        } else {
                            Log.e("StudyRoomVM", "joinRoom ACK first argument is not a JSONObject: $firstArg")
                            _uiState.value = StudyRoomUiState.InLobby(roomCode, listOf(username), isHost = false)
                        }
                    } else {
                        Log.e("StudyRoomVM", "joinRoom ACK arguments are null or empty")
                        _uiState.value = StudyRoomUiState.InLobby(roomCode, listOf(username), isHost = false)
                    }
                } catch (e: Exception) {
                    Log.e("StudyRoomVM", "Error parsing joinRoom ACK: ${e.message}", e)

                    _uiState.value = StudyRoomUiState.InLobby(roomCode, listOf(username), isHost = false)
                }
            }
        })
    }

    fun requestStartFocus(roomCode: String, durationMinutes: Int) {
        val data = JSONObject()
        data.put("roomCode", roomCode)
        data.put("durationMinutes", durationMinutes)
        socket?.emit("startFocus", data)
    }

    fun requestQuizSelection(roomCode: String) {
        val data = JSONObject()
        data.put("roomCode", roomCode)
        socket?.emit("requestMaterialSelection", data)
    }

    fun leaveRoom() {
        quizGenerationJob?.cancel()
        quizGenerationJob = null
        isStartRequested = false
        _generationStatus.value = QuizGenerationStatus.IDLE
        preGeneratedQuestions.clear()

        socket?.disconnect()
        socket = null
        _uiState.value = StudyRoomUiState.Initial
    }

    fun selectDocumentForQuiz(roomCode: String, document: Document) {
        selectedDocument = document
        _selectedDocumentName.value = document.fileName
        _generationStatus.value = QuizGenerationStatus.GENERATING
        
        val data = JSONObject()
        data.put("roomCode", roomCode)
        data.put("documentId", document.id)
        data.put("fileName", document.fileName)
        socket?.emit("materialSelected", data)
        
        preGenerateQuizQuestions(roomCode, document.id)
    }

    fun uploadAndSelectExternalDocument(context: Context, uri: Uri, roomCode: String) {
        viewModelScope.launch {
            _generationStatus.value = QuizGenerationStatus.GENERATING
            _selectedDocumentName.value = "Uploading syllabus..."
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                
                val fileName = runCatching {
                    contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                    }
                }.getOrNull() ?: "syllabus.pdf"
                
                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalArgumentException("Could not read selected file.")
                
                val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, body)
                
                val uploaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    api.uploadDocument(part)
                }
                
                _selectedDocumentName.value = fileName
                

                var latest: Document = uploaded
                repeat(20) {
                    val docList = runCatching { api.getDocuments() }.getOrNull()
                    val found = docList?.firstOrNull { it.id == uploaded.id }
                    if (found != null) {
                        latest = found
                        val raw = (found.summaryStatus ?: found.status).trim().uppercase()
                        if (raw == "COMPLETED" || raw == "PROCESSING") {
                            return@repeat
                        }
                    }
                    delay(1000)
                }
                
                selectDocumentForQuiz(roomCode, latest)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _generationStatus.value = QuizGenerationStatus.ERROR
                _selectedDocumentName.value = null
                Log.e("StudyRoomVM", "Failed to upload external document: ${e.message}", e)
            }
        }
    }

    private fun preGenerateQuizQuestions(roomCode: String, documentId: String) {
        _generationStatus.value = QuizGenerationStatus.GENERATING
        preGeneratedQuestions.clear()
        isStartRequested = false
        
        quizGenerationJob?.cancel()
        quizGenerationJob = viewModelScope.launch {
            try {
                val response1 = api.generateMockExam(MockExamRequest(questionCount = 5, documentId = documentId))
                if (response1.isSuccessful && response1.body() != null) {
                    val questions1 = response1.body()!!.content.quizQuestions
                    preGeneratedQuestions.clear()
                    preGeneratedQuestions.addAll(questions1)
                    _generationStatus.value = QuizGenerationStatus.READY
                    
                    if (isStartRequested) {
                        _isPreparingQuiz.value = false
                        emitStartQuiz(roomCode, questions1)
                        quizGenerationJob = generateSubsequentChunks(roomCode, documentId)
                    }
                } else {
                    _generationStatus.value = QuizGenerationStatus.ERROR
                    if (isStartRequested) {
                        _isPreparingQuiz.value = false
                        isStartRequested = false
                        _uiState.value = StudyRoomUiState.Error("AI Generation failed: Code ${response1.code()}")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    throw e
                }
                _generationStatus.value = QuizGenerationStatus.ERROR
                if (isStartRequested) {
                    _isPreparingQuiz.value = false
                    isStartRequested = false
                    _uiState.value = StudyRoomUiState.Error("Failed to pre-generate quiz: ${e.message}")
                }
            }
        }
    }

    fun startQuiz(roomCode: String) {
        val status = _generationStatus.value
        val doc = selectedDocument
        if (doc == null) {
            _uiState.value = StudyRoomUiState.Error("No syllabus PDF selected!")
            return
        }
        
        if (status == QuizGenerationStatus.READY) {
            _isPreparingQuiz.value = false
            emitStartQuiz(roomCode, preGeneratedQuestions)
            quizGenerationJob?.cancel()
            quizGenerationJob = generateSubsequentChunks(roomCode, doc.id)
        } else {
            isStartRequested = true
            _isPreparingQuiz.value = true
            
            val data = JSONObject()
            data.put("roomCode", roomCode)
            socket?.emit("quizPreparing", data)
        }
    }

    private fun generateSubsequentChunks(roomCode: String, documentId: String): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            try {
                var totalQuestions = preGeneratedQuestions.size
                val maxQuestions = 20
                var chunkIndex = 0
                
                while (totalQuestions < maxQuestions) {
                    chunkIndex++
                    Log.d("StudyRoomVM", "Generating chunk $chunkIndex, current total: $totalQuestions")
                    val response = api.generateMockExam(MockExamRequest(questionCount = 5, documentId = documentId))
                    if (response.isSuccessful && response.body() != null) {
                        val newQuestions = response.body()!!.content.quizQuestions
                        if (newQuestions.isEmpty()) {
                            Log.d("StudyRoomVM", "AI returned 0 new questions at chunk $chunkIndex. PDF content exhausted.")
                            break
                        }
                        emitAddQuestions(roomCode, newQuestions)
                        totalQuestions += newQuestions.size
                        Log.d("StudyRoomVM", "Added ${newQuestions.size} questions, total now: $totalQuestions")
                    } else {
                        Log.w("StudyRoomVM", "Chunk $chunkIndex API failed with code ${response.code()}")
                        break
                    }
                }
                
                Log.d("StudyRoomVM", "Quiz generation complete. Total questions: $totalQuestions")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StudyRoomVM", "Failed to generate subsequent chunks: ${e.message}")
            } finally {

                val data = JSONObject()
                data.put("roomCode", roomCode)
                socket?.emit("generationComplete", data)
            }
        }
    }

    private fun emitStartQuiz(roomCode: String, questions: List<BackendQuizQuestion>) {
        val startData = JSONObject()
        startData.put("roomCode", roomCode)
        val qArray = JSONArray()
        questions.forEach { q ->
            val qObj = JSONObject()
            qObj.put("question", q.question)
            val optObj = JSONObject()
            optObj.put("A", q.options["A"]); optObj.put("B", q.options["B"])
            optObj.put("C", q.options["C"]); optObj.put("D", q.options["D"])
            qObj.put("options", optObj)
            qObj.put("correctAnswer", q.correctAnswer)
            qArray.put(qObj)
        }
        startData.put("questions", qArray)
        socket?.emit("startQuiz", startData)
    }

    private fun emitAddQuestions(roomCode: String, questions: List<BackendQuizQuestion>) {
        val addData = JSONObject()
        addData.put("roomCode", roomCode)
        val qArray = JSONArray()
        questions.forEach { q ->
            val qObj = JSONObject()
            qObj.put("question", q.question)
            val optObj = JSONObject()
            optObj.put("A", q.options["A"]); optObj.put("B", q.options["B"])
            optObj.put("C", q.options["C"]); optObj.put("D", q.options["D"])
            qObj.put("options", optObj)
            qObj.put("correctAnswer", q.correctAnswer)
            qArray.put(qObj)
        }
        addData.put("additionalQuestions", qArray)
        socket?.emit("addQuestions", addData)
    }

    fun submitAnswer(roomCode: String, answer: String, timeRemainingRatio: Float) {
        val current = _uiState.value
        if (current is StudyRoomUiState.QuizActive) {
            userAnswers[current.currentIndex] = answer
        }
        val data = JSONObject()
        data.put("roomCode", roomCode)
        data.put("answer", answer)
        data.put("timeRemainingRatio", timeRemainingRatio)
        socket?.emit("submitAnswer", data)
    }

    fun goBackToLobby() {
        val current = _uiState.value
        if (current is StudyRoomUiState.QuizSummary) {
            _uiState.value = StudyRoomUiState.InLobby(
                roomCode = current.roomCode,
                participants = ArrayList(roomParticipants),
                isHost = current.isHost
            )
        }
    }

    fun exitRoom() {
        leaveRoom()
    }

    fun startQuizAfterCountdown(roomCode: String, questions: List<QuizQuestion>, endsAt: Long, isHost: Boolean) {
        _uiState.value = StudyRoomUiState.QuizActive(
            roomCode = roomCode,
            questions = questions,
            currentIndex = 0,
            rankings = emptyList(),
            endsAt = endsAt,
            isHost = isHost,
            isGeneratingRemaining = true
        )
    }

    fun nextQuestion(roomCode: String) {
        val data = JSONObject()
        data.put("roomCode", roomCode)
        data.put("username", myUsername)
        socket?.emit("nextQuestion", data)
    }

    override fun onCleared() {
        super.onCleared()
        quizGenerationJob?.cancel()
        quizGenerationJob = null
        socket?.disconnect()
        socket?.off()
    }
}
