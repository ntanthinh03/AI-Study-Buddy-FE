package com.thinh.aistudybuddy.data.models

import com.google.gson.annotations.SerializedName

data class Flashcard(
    val id: String,
    val front: String,
    val back: String,
    val box: Int,
    @SerializedName("next_review")
    val nextReview: String,
    @SerializedName("document_id")
    val documentId: String? = null,
    val document: Document? = null,
    @SerializedName(value = "createdAt", alternate = ["created_at"])
    val createdAt: String? = null,
    @SerializedName(value = "updatedAt", alternate = ["updated_at"])
    val updatedAt: String? = null
)

data class ReviewUpdateRequest(
    val isCorrect: Boolean
)

data class CreateFlashcardRequest(
    val front: String,
    val back: String
)
