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

data class PlayerRanking(val username: String, val score: Int)

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

    private var myUsername: String = ""

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
                
                
                val current = _uiState.value
                if (current is StudyRoomUiState.InLobby) {
                    val newList = (current.participants + username).distinct()
                    _uiState.value = current.copy(participants = newList)
                }
            }

            socket?.on("materialSelectionStarted") { args ->
                val data = args[0] as JSONObject
                val hostId = data.getString("hostId")
                val isMeHost = socket?.id() == hostId
                
                val current = _uiState.value
                if (current is StudyRoomUiState.InLobby) {
                    if (isMeHost) {
                        _uiState.value = StudyRoomUiState.SelectingMaterial(current.roomCode)
                    } else {
                        _uiState.value = StudyRoomUiState.WaitingForHost(current.roomCode, "Host is selecting material...")
                    }
                }
            }

            socket?.on("hostSelectedMaterial") { args ->
                val data = args[0] as JSONObject
                val fileName = data.getString("fileName")
                val current = _uiState.value
                if (current is StudyRoomUiState.WaitingForHost) {
                    _uiState.value = current.copy(message = "Host selected: $fileName\nPreparing questions...")
                } else if (current is StudyRoomUiState.InLobby && !current.isHost) {
                    _uiState.value = StudyRoomUiState.WaitingForHost(current.roomCode, "Host selected: $fileName\nPreparing questions...")
                }
            }

            socket?.on("quizStarted") { args ->
                _isPreparingQuiz.value = false
                val data = args[0] as JSONObject
                val roomCode = data.optString("roomCode", "")
                val questionsJson = data.getJSONArray("questions")
                val endsAt = data.optLong("endsAt", 0)
                val questions = parseQuestions(questionsJson)
                
                val current = _uiState.value
                val isHost = if (current is StudyRoomUiState.InLobby) current.isHost 
                             else if (current is StudyRoomUiState.SelectingMaterial) true
                             else false

                _uiState.value = StudyRoomUiState.QuizActive(
                    roomCode, questions, 0, emptyList(), 
                    endsAt = endsAt, isHost = isHost, isGeneratingRemaining = true
                )
            }

            socket?.on("questionsUpdated") { args ->
                
            }

            socket?.on("newQuestion") { args ->
                val data = args[0] as JSONObject
                val nextIndex = data.getInt("index")
                val endsAt = data.optLong("endsAt", 0)
                val current = _uiState.value
                if (current is StudyRoomUiState.QuizActive) {
                    _uiState.value = current.copy(
                        currentIndex = nextIndex, 
                        endsAt = endsAt,
                        answeredCount = 0
                    )
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
                _uiState.value = StudyRoomUiState.Initial
                _messages.value = _messages.value + "Quiz ended"
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
                if (args != null && args.isNotEmpty()) {
                    val response = args[0] as JSONObject
                    val isHost = response.optBoolean("isHost", false)
                    _uiState.value = StudyRoomUiState.InLobby(roomCode, listOf(username), isHost = isHost)
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
        socket?.disconnect()
        socket = null
        _uiState.value = StudyRoomUiState.Initial
    }

    fun selectDocumentForQuiz(roomCode: String, document: Document) {
        val data = JSONObject()
        data.put("roomCode", roomCode)
        data.put("documentId", document.id)
        data.put("fileName", document.fileName)
        socket?.emit("materialSelected", data)
        
        _isPreparingQuiz.value = true
        
        generateAndStartQuiz(roomCode, document.id)
    }

    private fun generateAndStartQuiz(roomCode: String, documentId: String) {
        viewModelScope.launch {
            try {
                
                val response1 = api.generateMockExam(MockExamRequest(questionCount = 5, documentId = documentId))
                if (response1.isSuccessful && response1.body() != null) {
                    val questions1 = response1.body()!!.content.quizQuestions
                    emitStartQuiz(roomCode, questions1)

                    
                    launch {
                        
                        val response2 = api.generateMockExam(MockExamRequest(questionCount = 5, documentId = documentId))
                        if (response2.isSuccessful && response2.body() != null) {
                            emitAddQuestions(roomCode, response2.body()!!.content.quizQuestions)
                        }

                        
                        val response3 = api.generateMockExam(MockExamRequest(questionCount = 5, documentId = documentId))
                        if (response3.isSuccessful && response3.body() != null) {
                            emitAddQuestions(roomCode, response3.body()!!.content.quizQuestions)
                        }

                        
                        val response4 = api.generateMockExam(MockExamRequest(questionCount = 5, documentId = documentId))
                        if (response4.isSuccessful && response4.body() != null) {
                            emitAddQuestions(roomCode, response4.body()!!.content.quizQuestions)
                        }
                    }
                } else {
                    _isPreparingQuiz.value = false
                    _uiState.value = StudyRoomUiState.Error("API failed to start: ${response1.code()}")
                }
            } catch (e: Exception) {
                _isPreparingQuiz.value = false
                _uiState.value = StudyRoomUiState.Error("Failed to generate quiz: ${e.message}")
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
        val data = JSONObject()
        data.put("roomCode", roomCode)
        data.put("answer", answer)
        data.put("timeRemainingRatio", timeRemainingRatio)
        socket?.emit("submitAnswer", data)
    }

    fun nextQuestion(roomCode: String) {
        val data = JSONObject()
        data.put("roomCode", roomCode)
        socket?.emit("nextQuestion", data)
    }

    override fun onCleared() {
        super.onCleared()
        socket?.disconnect()
        socket?.off()
    }
}
