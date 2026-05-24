package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.services.ApiService
import com.thinh.aistudybuddy.data.models.MindMapCreateRequest
import com.thinh.aistudybuddy.data.models.MindMapResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MindMapState {
    IDLE, GENERATING, COMPLETED, ERROR
}

class MindMapViewModel(private val apiService: ApiService) : ViewModel() {
    var state by mutableStateOf(MindMapState.IDLE)
    var currentMindMap by mutableStateOf<MindMapResponse?>(null)
    var mindMapHistory by mutableStateOf<List<MindMapResponse>>(emptyList())
    var errorMessage by mutableStateOf<String?>(null)
    var generatingMessage by mutableStateOf("Analyzing document structure...")

    private val generatingMessages = listOf(
        "Analyzing document structure...",
        "Extracting key concepts...",
        "Identifying hierarchical relationships...",
        "Organizing nodes and branches...",
        "Finalizing your visual mind map...",
        "Almost ready!"
    )

    fun generateMindMap(documentId: String, text: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            state = MindMapState.GENERATING
            errorMessage = null
            
            
            val messageJob = launch {
                for (msg in generatingMessages) {
                    generatingMessage = msg
                    delay(3000)
                }
            }

            try {
                val response = apiService.generateMindMap(MindMapCreateRequest(documentId, text))
                messageJob.cancel()
                currentMindMap = response
                state = MindMapState.COMPLETED
                onComplete()
                loadHistory() 
            } catch (e: Exception) {
                messageJob.cancel()
                state = MindMapState.ERROR
                errorMessage = e.message ?: "Failed to generate mind map"
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                mindMapHistory = apiService.getMindMaps()
            } catch (e: Exception) {
                
            }
        }
    }

    fun loadDetail(id: String) {
        viewModelScope.launch {
            state = MindMapState.IDLE
            try {
                currentMindMap = apiService.getMindMapDetail(id)
                state = MindMapState.COMPLETED
            } catch (e: Exception) {
                state = MindMapState.ERROR
                errorMessage = e.message ?: "Failed to load mind map detail"
            }
        }
    }

    fun resetState() {
        state = MindMapState.IDLE
        currentMindMap = null
        errorMessage = null
    }
}
