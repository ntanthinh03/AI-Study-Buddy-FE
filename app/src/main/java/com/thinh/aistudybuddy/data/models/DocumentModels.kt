package com.thinh.aistudybuddy.data.models

import com.google.gson.annotations.SerializedName

data class Document(
    val id: String,
    val fileName: String,
    val summary: String?,
    val status: String,
    val summaryStatus: String? = null,
    val ragStatus: String? = null,
    val createdAt: String? = null
)

data class DocumentStatusResponse(
    val id: String,
    val status: String,
    val progress: Int? = null,
    val errorMessage: String? = null
)

data class DocumentChatRequest(@SerializedName("question") val question: String)

data class RagUploadResponse(
    val success: Boolean, 
    val documentId: String? = null, 
    val chunks: Int? = null, 
    val status: String = "PROCESSING"
)

data class SaveDocumentArtifactRequest(
    val artifactType: String,
    val artifact: com.google.gson.JsonElement,
    val note: String? = null
)

data class MindMapNode(
    val id: String,
    val label: String,
    val parentId: String? = null
)

data class MindMapResponse(
    val nodes: List<MindMapNode>
)
