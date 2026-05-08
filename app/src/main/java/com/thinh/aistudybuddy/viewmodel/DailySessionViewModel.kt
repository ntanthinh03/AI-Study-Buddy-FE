package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.network.RetrofitClient
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

    fun loadDailySession() {
        viewModelScope.launch {
            loading = true
            errorMessage = null
            try {
                val response = RetrofitClient.instance.getDailySession()
                if (response.isSuccessful) {
                    session = response.body()
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

    fun submitAnswer(isCorrect: Boolean) {
        if (isCorrect) correctCount++
        
        val totalSteps = (session?.content?.quizQuestions?.size ?: 0) + (session?.content?.flashcards?.size ?: 0)
        if (currentStep < totalSteps - 1) {
            currentStep++
        } else {
            finishSession()
        }
    }

    private fun finishSession() {
        val currentSession = session ?: return
        viewModelScope.launch {
            loading = true
            try {
                val totalQuestions = (session?.content?.quizQuestions?.size ?: 0)
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
