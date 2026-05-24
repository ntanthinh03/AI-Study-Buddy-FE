package com.thinh.aistudybuddy.data.models

import java.util.Date

data class MindMapNode(
    val id: String,
    val label: String,
    val parentId: String? = null
)

data class MindMapResponse(
    val id: String,
    val title: String,
    val content: List<MindMapNode>,
    val userId: String,
    val documentId: String?,
    val createdAt: Date
)

data class MindMapCreateRequest(
    val documentId: String,
    val text: String
)
