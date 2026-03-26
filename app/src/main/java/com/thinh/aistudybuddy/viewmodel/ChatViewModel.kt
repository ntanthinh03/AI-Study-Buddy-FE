package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.thinh.aistudybuddy.data.model.Banner
import com.thinh.aistudybuddy.data.model.ChatMessage
import com.thinh.aistudybuddy.data.model.Conversation
import com.thinh.aistudybuddy.data.model.Suggestion
import java.util.UUID

enum class ChatScreenType {
    NEW_CHAT, CONTINUING_CHAT
}

class ChatViewModel : ViewModel() {

    private val _conversations = mutableStateListOf<Conversation>()
    val conversations: List<Conversation> get() = _conversations

    var activeConversationId by mutableStateOf("")
    var searchQuery by mutableStateOf("")

    private var _currentChatType = mutableStateOf(ChatScreenType.NEW_CHAT)
    val currentChatType: ChatScreenType get() = _currentChatType.value

    private val _suggestions = mutableStateListOf<Suggestion>()
    val suggestions: List<Suggestion> get() = _suggestions

    private val _banner = mutableStateOf<Banner?>(null)
    val banner: Banner? get() = _banner.value

    init {
        setupMockData()
        startNewChat()
    }

    private fun setupMockData() {
        val quizConv = Conversation(UUID.randomUUID().toString(), "Java Quiz Creation", isQuiz = true).apply {
            chatMessages.add(ChatMessage("Hi Buddy! Can you create a 10-question quiz about Java for me?", true))
            chatMessages.add(ChatMessage("Certainly! I have generated a 10-question quiz on Java concepts. Ready to start?", false))
        }

        val conv1 = Conversation(UUID.randomUUID().toString(), "Machine Learning Concepts").apply {
            chatMessages.add(ChatMessage("What is supervised learning?", true))
            chatMessages.add(ChatMessage("Supervised learning is an ML approach where models are trained on labeled data.", false))
        }

        val conv2 = Conversation(UUID.randomUUID().toString(), "PDF Summary: AI Ethics").apply {
            chatMessages.add(ChatMessage("Summarize the uploaded PDF document.", true))
            chatMessages.add(ChatMessage("The document discusses the ethical considerations of developing AI.", false))
        }

        _conversations.addAll(listOf(quizConv, conv1, conv2))
        activeConversationId = quizConv.id
        _currentChatType.value = ChatScreenType.CONTINUING_CHAT

        _suggestions.addAll(listOf(
            Suggestion("Help me write", "a simple story"),
            Suggestion("Give me study tips", "for Java concepts"),
            Suggestion("Inspire me with", "a new idea"),
            Suggestion("Save me time with", "a research plan")
        ))
        _banner.value = Banner("New: Data Analysis Gem is here!", "Try Now")
    }

    fun startNewChat() {
        _currentChatType.value = ChatScreenType.NEW_CHAT
        activeConversationId = ""
    }

    fun sendMessage(text: String) {
        if (_currentChatType.value == ChatScreenType.NEW_CHAT) {
            val newConvId = UUID.randomUUID().toString()
            val newConv = Conversation(newConvId, text.take(20)).apply {
                chatMessages.add(ChatMessage(text, true))
            }
            _conversations.add(0, newConv)
            activeConversationId = newConvId
            _currentChatType.value = ChatScreenType.CONTINUING_CHAT
        } else {
            val currentConv = _conversations.find { it.id == activeConversationId }
            currentConv?.let {
                it.chatMessages.add(ChatMessage(text, true))
            }
        }
    }

    fun selectConversation(id: String) {
        activeConversationId = id
        _currentChatType.value = ChatScreenType.CONTINUING_CHAT
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun renameConversation(id: String, newTitle: String) {
        val index = _conversations.indexOfFirst { it.id == id }
        if (index != -1 && newTitle.isNotBlank()) {
            _conversations[index] = _conversations[index].copy(title = newTitle)
        }
    }

    fun deleteConversation(id: String) {
        _conversations.removeIf { it.id == id }
        if (activeConversationId == id) {
            startNewChat()
        }
    }

    fun importPdfSummarize(fileName: String) {
        val messageText = "Imported PDF: $fileName. Please summarize this document."
        sendMessage(messageText)
    }

    val activeMessages: List<ChatMessage>
        get() = _conversations.find { it.id == activeConversationId }?.chatMessages ?: emptyList()

    val filteredConversations: List<Conversation>
        get() = if (searchQuery.isEmpty()) {
            _conversations
        } else {
            _conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
}