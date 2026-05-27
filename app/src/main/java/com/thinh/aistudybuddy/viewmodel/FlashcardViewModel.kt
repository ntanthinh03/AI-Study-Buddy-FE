package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.models.Flashcard
import com.thinh.aistudybuddy.data.models.ReviewUpdateRequest
import com.thinh.aistudybuddy.services.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

class FlashcardViewModel : ViewModel() {
    private val _flashcards = mutableStateListOf<Flashcard>()
    val flashcards: List<Flashcard> get() = _flashcards

    private val _flashcardsToReview = mutableStateListOf<Flashcard>()
    val flashcardsToReview: List<Flashcard> get() = _flashcardsToReview

    var isLoading by mutableStateOf(false)
    var isGenerating by mutableStateOf(false)
    var hasLoadedFlashcards by mutableStateOf(false)
    var hasLoadedReviewFlashcards by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)
    var focusDocumentId by mutableStateOf<String?>(null)

    fun hasFlashcardsForDocument(documentId: String): Boolean {
        return _flashcards.any { it.documentId == documentId }
    }

    fun focusDocument(documentId: String?) {
        focusDocumentId = documentId?.takeIf { it.isNotBlank() }
    }

    fun showNotice(message: String, durationMillis: Long = 3000L) {
        viewModelScope.launch {
            notice = message
            delay(durationMillis)
            if (notice == message) {
                notice = null
            }
        }
    }

    fun clearNotice() {
        viewModelScope.launch {
            notice = null
        }
    }

    fun loadAllFlashcards() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isLoading = true
                hasLoadedFlashcards = false
                error = null
            }
            try {
                val result = RetrofitClient.instance.getFlashcards()
                withContext(Dispatchers.Main) {
                    _flashcards.clear()
                    _flashcards.addAll(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "Failed to load flashcards: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    hasLoadedFlashcards = true
                }
            }
        }
    }

    fun loadFlashcardsToReview() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                hasLoadedReviewFlashcards = false
                error = null
            }
            try {
                val result = RetrofitClient.instance.getFlashcardsToReview()
                withContext(Dispatchers.Main) {
                    _flashcardsToReview.clear()
                    _flashcardsToReview.addAll(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "Failed to load reviews: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    hasLoadedReviewFlashcards = true
                }
            }
        }
    }

    fun generateFlashcards(documentId: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (documentId.isBlank()) {
                withContext(Dispatchers.Main) {
                    error = "Missing document id for flashcard generation."
                    onComplete?.invoke(false)
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                if (isGenerating) {
                    onComplete?.invoke(false)
                    return@withContext
                }
                isGenerating = true
                error = null
                notice = null
            }
            try {
                val existingFlashcards = if (_flashcards.isNotEmpty()) {
                    _flashcards.filter { it.documentId == documentId }
                } else {
                    val allFlashcards = RetrofitClient.instance.getFlashcards()
                    withContext(Dispatchers.Main) {
                        _flashcards.clear()
                        _flashcards.addAll(allFlashcards)
                    }
                    allFlashcards.filter { it.documentId == documentId }
                }

                if (existingFlashcards.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        showNotice("Flashcards for this PDF already exist. Opening Flashcards Library.")
                        onComplete?.invoke(false)
                    }
                    return@launch
                }

                val result = RetrofitClient.instance.generateFlashcards(documentId)
                withContext(Dispatchers.Main) {
                    _flashcards.removeAll { it.documentId == documentId }
                    _flashcards.addAll(0, result)
                    showNotice("Flashcards generated successfully. Opening Flashcards Library.")
                    onComplete?.invoke(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "Failed to generate flashcards: ${e.message}"
                    onComplete?.invoke(false)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isGenerating = false
                }
            }
        }
    }

    fun generateFlashcardsByTopic(documentId: String, topic: String, onComplete: ((List<Flashcard>?) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (documentId.isBlank() || topic.isBlank()) {
                withContext(Dispatchers.Main) {
                    error = "Missing documentId or topic name."
                    onComplete?.invoke(null)
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                isGenerating = true
                error = null
            }
            try {
                val result = RetrofitClient.instance.generateFlashcardsByTopic(
                    com.thinh.aistudybuddy.data.models.FlashcardTopicRequest(documentId, topic)
                )
                withContext(Dispatchers.Main) {
                    _flashcards.addAll(0, result)
                    onComplete?.invoke(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "Failed to generate flashcards by topic: ${e.message}"
                    onComplete?.invoke(null)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isGenerating = false
                }
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
                withContext(Dispatchers.Main) {
                    loadAllFlashcards()
                    loadFlashcardsToReview()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "Failed to update review: ${e.message}"
                }
            }
        }
    }

    fun addManualFlashcard(front: String, back: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isLoading = true
                error = null
            }
            try {
                val result = RetrofitClient.instance.createFlashcard(
                    com.thinh.aistudybuddy.data.models.CreateFlashcardRequest(front, back)
                )
                withContext(Dispatchers.Main) {
                    _flashcards.add(0, result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "Failed to add flashcard: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }
}
