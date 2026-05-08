package com.thinh.aistudybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.network.RetrofitClient
import com.thinh.aistudybuddy.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MindMapUiState {
    object Initial : MindMapUiState()
    object Loading : MindMapUiState()
    data class Success(val nodes: List<MindMapNode>) : MindMapUiState()
    data class Error(val message: String) : MindMapUiState()
}

class MindMapViewModel : ViewModel() {
    private val api = RetrofitClient.instance

    private val _uiState = MutableStateFlow<MindMapUiState>(MindMapUiState.Initial)
    val uiState: StateFlow<MindMapUiState> = _uiState.asStateFlow()

    fun generateMindMap(documentId: String) {
        viewModelScope.launch {
            _uiState.value = MindMapUiState.Loading
            try {
                val response = api.generateMindMap(documentId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = MindMapUiState.Success(response.body()!!.nodes)
                } else {
                    _uiState.value = MindMapUiState.Error("Failed to generate mind map: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = MindMapUiState.Error("Connection error: ${e.message}")
            }
        }
    }
}
