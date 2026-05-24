package com.thinh.aistudybuddy.viewmodel

import android.os.CountDownTimer
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.models.FocusSubmitRequest
import com.thinh.aistudybuddy.services.network.RetrofitClient
import kotlinx.coroutines.launch

class FocusViewModel : ViewModel() {
    var isRunning by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    
    private val defaultTimeMs = 25 * 60 * 1000L
    var timeRemainingMs by mutableLongStateOf(defaultTimeMs)
    
    var showCompletionDialog by mutableStateOf(false)
    var earnedXp by mutableIntStateOf(0)
    
    private var timer: CountDownTimer? = null

    fun startTimer() {
        if (isRunning && !isPaused) return
        
        isRunning = true
        isPaused = false
        
        timer = object : CountDownTimer(timeRemainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMs = millisUntilFinished
            }

            override fun onFinish() {
                isRunning = false
                timeRemainingMs = 0
                submitFocusSession()
            }
        }.start()
    }

    fun pauseTimer() {
        if (!isRunning || isPaused) return
        timer?.cancel()
        isPaused = true
    }

    fun stopTimer() {
        timer?.cancel()
        isRunning = false
        isPaused = false
        timeRemainingMs = defaultTimeMs
    }

    private fun submitFocusSession() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.submitFocusSession(FocusSubmitRequest(25))
                if (response.isSuccessful) {
                    earnedXp = 20
                    showCompletionDialog = true
                }
            } catch (e: Exception) {
            }
        }
    }

    fun dismissDialog() {
        showCompletionDialog = false
        stopTimer()
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
