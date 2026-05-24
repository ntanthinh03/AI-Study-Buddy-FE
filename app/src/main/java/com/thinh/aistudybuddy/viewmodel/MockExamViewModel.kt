package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.services.network.RetrofitClient
import com.thinh.aistudybuddy.data.models.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MockExamViewModel : ViewModel() {
    private val api = RetrofitClient.instance

    private val _uiState = MutableStateFlow<MockExamUiState>(MockExamUiState.Initial)
    val uiState: StateFlow<MockExamUiState> = _uiState.asStateFlow()

    private val _timeLeft = MutableStateFlow(0)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _isLoadingDocs = MutableStateFlow(false)
    val isLoadingDocs: StateFlow<Boolean> = _isLoadingDocs.asStateFlow()

    private var timerJob: Job? = null
    private var currentSession: StudySession? = null
    
    private val selectedAnswers = mutableStateMapOf<Int, String>()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _isLoadingDocs.value = true
            try {
                val docs = api.getDocuments()
                _documents.value = docs.sortedByDescending { it.createdAt ?: "" }
            } catch (e: Exception) {
                
            } finally {
                _isLoadingDocs.value = false
            }
        }
    }

    fun startMockExam(questionCount: Int = 20, documentId: String? = null) {
        viewModelScope.launch {
            _uiState.value = MockExamUiState.Loading
            try {
                val response = api.generateMockExam(
                    MockExamRequest(
                        questionCount = questionCount,
                        documentId = documentId
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val session = response.body()!!
                    val questions = session.content.quizQuestions.mapIndexed { idx: Int, bq: BackendQuizQuestion ->
                        QuizQuestion(
                            id = "q$idx",
                            question = bq.question,
                            options = listOf(bq.options["A"] ?: "", bq.options["B"] ?: "", bq.options["C"] ?: "", bq.options["D"] ?: ""),
                            correctAnswerIndex = when(bq.correctAnswer) { "A" -> 0; "B" -> 1; "C" -> 2; else -> 3 },
                            explanation = bq.explanation
                        )
                    }
                    
                    if (questions.isEmpty()) {
                        _uiState.value = MockExamUiState.Error("Not enough questions generated. Please try another document.")
                        return@launch
                    }

                    currentSession = session
                    selectedAnswers.clear()
                    
                    _timeLeft.value = questions.size * 60
                    _uiState.value = MockExamUiState.InProgress(questions)
                    
                    startTimer()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: ""
                    if (errorMsg.contains("Not enough questions")) {
                        _uiState.value = MockExamUiState.Error("Question pool is empty. Please select a document to generate an exam.")
                    } else {
                        _uiState.value = MockExamUiState.Error("Unable to create exam. Please try again.")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = MockExamUiState.Error(e.message ?: "Connection error")
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0) {
                delay(1000L)
                _timeLeft.value -= 1
            }
            submitExam()
        }
    }

    fun selectAnswer(questionIndex: Int, answer: String) {
        selectedAnswers[questionIndex] = answer
    }

    fun getSelectedAnswer(questionIndex: Int): String? {
        return selectedAnswers[questionIndex]
    }

    fun submitExam() {
        timerJob?.cancel()
        val session = currentSession ?: return
        val questions = session.content?.quizQuestions ?: emptyList()
        
        var correct = 0
        questions.forEachIndexed { index, q ->
            if (selectedAnswers[index] == q.correctAnswer) {
                correct++
            }
        }

        viewModelScope.launch {
            _uiState.value = MockExamUiState.Submitting
            try {
                val response = api.submitSessionResult(
                    session.id,
                    SessionSubmitResult(correctAnswers = correct, totalQuestions = questions.size)
                )
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!
                    _uiState.value = MockExamUiState.Completed(
                        correctAnswers = correct,
                        totalQuestions = questions.size,
                        xpEarned = correct * 10 + 20,
                        newXpTotal = stats.totalXP,
                        newLevel = stats.level
                    )
                } else {
                    _uiState.value = MockExamUiState.Error("Failed to submit exam: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = MockExamUiState.Error(e.message ?: "Connection error")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

sealed class MockExamUiState {
    object Initial : MockExamUiState()
    object Loading : MockExamUiState()
    data class InProgress(val questions: List<QuizQuestion>) : MockExamUiState()
    object Submitting : MockExamUiState()
    data class Completed(
        val correctAnswers: Int,
        val totalQuestions: Int,
        val xpEarned: Int,
        val newXpTotal: Int,
        val newLevel: Int
    ) : MockExamUiState()
    data class Error(val message: String) : MockExamUiState()
}
