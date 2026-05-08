package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.models.Flashcard
import com.thinh.aistudybuddy.data.models.ReviewUpdateRequest
import com.thinh.aistudybuddy.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class FlashcardViewModel : ViewModel() {
    private val _flashcards = mutableStateListOf<Flashcard>()
    val flashcards: List<Flashcard> get() = _flashcards

    private val _flashcardsToReview = mutableStateListOf<Flashcard>()
    val flashcardsToReview: List<Flashcard> get() = _flashcardsToReview

    var isLoading by mutableStateOf(false)
    var isGenerating by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun loadAllFlashcards() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            error = null
            try {
                val result = RetrofitClient.instance.getFlashcards()
                _flashcards.clear()
                _flashcards.addAll(result)
            } catch (e: Exception) {
                error = "Failed to load flashcards: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadFlashcardsToReview() {
        viewModelScope.launch(Dispatchers.IO) {
            error = null
            try {
                val result = RetrofitClient.instance.getFlashcardsToReview()
                _flashcardsToReview.clear()
                _flashcardsToReview.addAll(result)
            } catch (e: Exception) {
                error = "Failed to load reviews: ${e.message}"
            }
        }
    }

    fun generateFlashcards(documentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isGenerating = true
            error = null
            try {
                val result = RetrofitClient.instance.generateFlashcards(documentId)
                // Add to list immediately so they appear even if we don't reload
                _flashcards.addAll(0, result)
            } catch (e: Exception) {
                error = "Failed to generate flashcards: ${e.message}"
            } finally {
                isGenerating = false
            }
        }
    }

    fun submitReview(flashcardId: String, isCorrect: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                RetrofitClient.instance.updateFlashcardReview(
                    flashcardId,
                    ReviewUpdateRequest(isCorrect)
                )
                loadFlashcardsToReview()
            } catch (e: Exception) {
                error = "Failed to update review: ${e.message}"
            }
        }
    }

    fun addManualFlashcard(front: String, back: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            try {
                val result = RetrofitClient.instance.createFlashcard(
                    com.thinh.aistudybuddy.data.models.CreateFlashcardRequest(front, back)
                )
                _flashcards.add(0, result)
            } catch (e: Exception) {
                error = "Failed to add flashcard: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
