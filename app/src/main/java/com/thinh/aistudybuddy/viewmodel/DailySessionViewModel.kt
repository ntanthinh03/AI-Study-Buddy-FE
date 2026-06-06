package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.services.network.RetrofitClient
import retrofit2.Response
import com.thinh.aistudybuddy.data.models.*
import kotlinx.coroutines.launch

class DailySessionViewModel : ViewModel() {
    var session by mutableStateOf<StudySession?>(null)
    var stats by mutableStateOf<GamificationStats?>(null)
    var loading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    
    var currentStep by mutableIntStateOf(0)
    var correctCount by mutableIntStateOf(0)
    var isFinished by mutableStateOf(false)
    var wasAlreadyCompleted by mutableStateOf(false)

    // Interactive quiz states
    var selectedOption by mutableStateOf<String?>(null)
    var showFeedback by mutableStateOf(false)
    val userAnswers = mutableStateMapOf<Int, String>()
    var reviewModeActive by mutableStateOf(false)

    fun loadDailySession() {
        currentStep = 0
        correctCount = 0
        isFinished = false
        wasAlreadyCompleted = false
        selectedOption = null
        showFeedback = false
        userAnswers.clear()
        reviewModeActive = false
        errorMessage = null

        viewModelScope.launch {
            loading = true
            try {
                val response = RetrofitClient.instance.getDailySession()
                if (response.isSuccessful) {
                    val s = response.body()
                    session = s
                    if (s?.status == "COMPLETED") {
                        wasAlreadyCompleted = true
                    } else {
                        wasAlreadyCompleted = false
                    }
                } else {
                    errorMessage = "Failed to load session: ${response.code()}"
                }
                
                val statsResponse = RetrofitClient.instance.getUserStats()
                if (statsResponse.isSuccessful) {
                    stats = statsResponse.body()
                }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                loading = false
            }
        }
    }

    fun selectOption(option: String) {
        if (!showFeedback) {
            selectedOption = option
        }
    }

    fun confirmAnswer(correctAnswer: String) {
        if (selectedOption != null && !showFeedback) {
            showFeedback = true
            userAnswers[currentStep] = selectedOption!!
            if (selectedOption == correctAnswer) {
                correctCount++
            }
        }
    }

    fun nextStep() {
        val totalSteps = session?.content?.quizQuestions?.size ?: 0
        if (currentStep < totalSteps - 1) {
            currentStep++
            selectedOption = null
            showFeedback = false
        } else {
            // Trigger review screen first as requested
            reviewModeActive = true
        }
    }

    fun completeSessionAfterReview() {
        if (wasAlreadyCompleted) {
            isFinished = true
        } else {
            finishSession()
        }
    }

    private fun finishSession() {
        val currentSession = session ?: return
        viewModelScope.launch {
            loading = true
            try {
                val totalQuestions = session?.content?.quizQuestions?.size ?: 0
                val response = RetrofitClient.instance.submitSessionResult(
                    currentSession.id,
                    SessionSubmitResult(correctCount, totalQuestions)
                )
                if (response.isSuccessful) {
                    stats = response.body()
                    isFinished = true
                } else {
                    errorMessage = "Failed to submit result"
                }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                loading = false
            }
        }
    }
    
    fun generateBatch(documentId: String) {
        viewModelScope.launch {
            loading = true
            try {
                val response = RetrofitClient.instance.generateBatch(documentId)
                if (response.isSuccessful) {
                    loadDailySession()
                }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                loading = false
            }
        }
    }
}
