package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.model.Banner
import com.thinh.aistudybuddy.data.model.ChatMessage
import com.thinh.aistudybuddy.data.model.Conversation
import com.thinh.aistudybuddy.data.model.Suggestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class ChatScreenType {
    NEW_CHAT, CONTINUING_CHAT
}

class ChatViewModel : ViewModel() {

    private val _conversations = mutableStateListOf<Conversation>()
    val conversations: List<Conversation> get() = _conversations

    var activeConversationId by mutableStateOf("")
    var searchQuery by mutableStateOf("")
    var isTyping by mutableStateOf(false)

    private var _currentChatType = mutableStateOf(ChatScreenType.NEW_CHAT)
    val currentChatType: ChatScreenType get() = _currentChatType.value

    private val _suggestions = mutableStateListOf<Suggestion>()
    val suggestions: List<Suggestion> get() = _suggestions

    private val _banner = mutableStateOf<Banner?>(null)
    val banner: Banner? get() = _banner.value

    init {
        setupMockData()
    }

    private fun setupMockData() {
        _conversations.clear()
        val quizId = UUID.randomUUID().toString()
        val quizConv = Conversation(quizId, "Java Basics Quiz", isQuiz = true).apply {
            chatMessages.addAll(listOf(
                ChatMessage(id = UUID.randomUUID().toString(), text = "Can you create a quiz about Java fundamentals?", isUser = true),
                ChatMessage(id = UUID.randomUUID().toString(), text = "I've generated a 10-question quiz on Java concepts. You can start testing your knowledge now!", isUser = false, showQuizButton = true)
            ))
        }
        val planId = UUID.randomUUID().toString()
        val planConv = Conversation(planId, "Study Plan: Data Structures").apply {
            chatMessages.addAll(listOf(
                ChatMessage(id = UUID.randomUUID().toString(), text = "Create a study plan for Data Structures", isUser = true),
                ChatMessage(id = UUID.randomUUID().toString(), text = "I've designed a personalized roadmap for Data Structures. Tap 'Check Plan' to start learning!", isUser = false, showStudyPlanButton = true)
            ))
        }
        _conversations.addAll(listOf(quizConv, planConv))
        _suggestions.addAll(listOf(
            Suggestion("Create a Quiz", "on Python Programming"),
            Suggestion("Summarize PDF", "Research paper summary"),
            Suggestion("Explain Concept", "How React Native works"),
            Suggestion("Study Plan", "for Data Structures")
        ))
    }

    fun startNewChat() {
        _currentChatType.value = ChatScreenType.NEW_CHAT
        activeConversationId = ""
        isTyping = false
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (_currentChatType.value == ChatScreenType.NEW_CHAT) {
            val newConvId = UUID.randomUUID().toString()
            val newConv = Conversation(newConvId, text.take(25)).apply {
                chatMessages.add(ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = true))
            }
            _conversations.add(0, newConv)
            activeConversationId = newConvId
            _currentChatType.value = ChatScreenType.CONTINUING_CHAT
            generateAiReply(newConvId, text)
        } else {
            val index = _conversations.indexOfFirst { it.id == activeConversationId }
            if (index != -1) {
                val updatedConv = _conversations[index]
                updatedConv.chatMessages.add(ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = true))
                _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                generateAiReply(activeConversationId, text)
            }
        }
    }

    private fun generateAiReply(convId: String, userMessage: String) {
        viewModelScope.launch {
            isTyping = true
            delay(2000)
            val index = _conversations.indexOfFirst { it.id == convId }
            if (index != -1) {
                val updatedConv = _conversations[index]
                val lowerMsg = userMessage.lowercase()
                var showQuiz = false
                var showPlan = false
                val aiResponse = when {
                    lowerMsg.contains("ai_research_2026.pdf") -> "I've analyzed 'AI_Research_2026.pdf'. Key findings include a 40% increase in engagement using AI Study Buddies."
                    lowerMsg.contains("quiz") -> { showQuiz = true; "I've prepared a quiz for you. Tap below to start!" }
                    lowerMsg.contains("plan") -> { showPlan = true; "Here is your study roadmap. Tap 'Check Plan' to see details!" }
                    else -> "That's interesting! Let me help you with that."
                }
                updatedConv.chatMessages.add(ChatMessage(id = UUID.randomUUID().toString(), text = aiResponse, isUser = false, showQuizButton = showQuiz, showStudyPlanButton = showPlan))
                _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
            }
            isTyping = false
        }
    }

    fun selectConversation(id: String) {
        activeConversationId = id
        _currentChatType.value = ChatScreenType.CONTINUING_CHAT
        isTyping = false
    }

    fun updateSearchQuery(query: String) { searchQuery = query }
    fun renameConversation(id: String, newTitle: String) {
        val index = _conversations.indexOfFirst { it.id == id }
        if (index != -1 && newTitle.isNotBlank()) _conversations[index] = _conversations[index].copy(title = newTitle)
    }
    fun deleteConversation(id: String) {
        _conversations.removeIf { it.id == id }
        if (activeConversationId == id) startNewChat()
    }
    val activeMessages: List<ChatMessage> get() = _conversations.find { it.id == activeConversationId }?.chatMessages ?: emptyList()
    val filteredConversations: List<Conversation> get() = if (searchQuery.isEmpty()) _conversations else _conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
}