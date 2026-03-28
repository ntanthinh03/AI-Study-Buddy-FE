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

        // 1. Hội thoại Quiz mẫu
        val quizId = UUID.randomUUID().toString()
        val quizConv = Conversation(quizId, "Java Basics Quiz", isQuiz = true).apply {
            chatMessages.addAll(listOf(
                ChatMessage("Can you create a quiz about Java fundamentals?", true),
                ChatMessage(
                    text = "I've generated a 10-question quiz on Java concepts. You can start testing your knowledge now!",
                    isUser = false,
                    showQuizButton = true // Nút dính chặt vào tin nhắn này
                )
            ))
        }

        // 2. Hội thoại Tóm tắt mẫu
        val summaryId = UUID.randomUUID().toString()
        val summaryConv = Conversation(summaryId, "Summary: Machine Learning PDF").apply {
            chatMessages.addAll(listOf(
                ChatMessage("Summarize the uploaded PDF about Machine Learning.", true),
                ChatMessage(
                    text = "The document explains that Machine Learning is a subset of AI focused on building systems that learn from data. Key topics include Supervised Learning and Neural Networks.",
                    isUser = false
                )
            ))
        }

        // 3. Hội thoại Giải thích mẫu
        val explainId = UUID.randomUUID().toString()
        val explainConv = Conversation(explainId, "Explain: AWS S3 vs EBS").apply {
            chatMessages.addAll(listOf(
                ChatMessage("What is the difference between AWS S3 and EBS?", true),
                ChatMessage(
                    text = "S3 is object storage accessible via the internet, ideal for photos and backups. EBS is block storage designed to be used as a hard drive for EC2 instances.",
                    isUser = false
                )
            ))
        }

        _conversations.addAll(listOf(quizConv, summaryConv, explainConv))

        // Gợi ý ở màn hình New Chat
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
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        if (_currentChatType.value == ChatScreenType.NEW_CHAT) {
            val newConvId = UUID.randomUUID().toString()
            // Tự động nhận diện nếu người dùng yêu cầu Quiz để bật flag của hội thoại
            val isAskingQuiz = text.lowercase().contains("quiz")

            val newConv = Conversation(newConvId, text.take(20), isQuiz = isAskingQuiz).apply {
                chatMessages.add(ChatMessage(text, true))
            }
            _conversations.add(0, newConv)
            activeConversationId = newConvId
            _currentChatType.value = ChatScreenType.CONTINUING_CHAT
            generateAiReply(newConvId, text)
        } else {
            val index = _conversations.indexOfFirst { it.id == activeConversationId }
            if (index != -1) {
                val updatedConv = _conversations[index]
                updatedConv.chatMessages.add(ChatMessage(text, true))

                // Cập nhật lại list để Compose trigger Recomposition
                _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                generateAiReply(activeConversationId, text)
            }
        }
    }

    private fun generateAiReply(convId: String, userMessage: String) {
        viewModelScope.launch {
            delay(1000) // Giả lập độ trễ phản hồi của AI
            val index = _conversations.indexOfFirst { it.id == convId }
            if (index != -1) {
                val updatedConv = _conversations[index]
                val lowerMsg = userMessage.lowercase()

                var shouldShowButton = false

                val aiResponse = when {
                    lowerMsg.contains("hello") || lowerMsg.contains("hi") ->
                        "Hello! I'm Buddy, your AI study assistant. How can I help you today?"

                    lowerMsg.contains("quiz") -> {
                        shouldShowButton = true
                        "I've prepared a specialized quiz based on your request. Tap the button below to start!"
                    }

                    lowerMsg.contains("summarize") || lowerMsg.contains("pdf") ->
                        "I've analyzed the document. Here is a concise summary of the key points found."

                    lowerMsg.contains("explain") || lowerMsg.contains("what is") ->
                        "That's an interesting topic! Here is a detailed explanation to help you understand it better."

                    else -> "I don't understand"
                }

                updatedConv.chatMessages.add(
                    ChatMessage(text = aiResponse, isUser = false, showQuizButton = shouldShowButton)
                )

                // Cập nhật trạng thái Quiz của hội thoại nếu AI vừa tạo Quiz
                if (shouldShowButton) {
                    _conversations[index] = updatedConv.copy(
                        chatMessages = updatedConv.chatMessages,
                        isQuiz = true
                    )
                } else {
                    _conversations[index] = updatedConv.copy(chatMessages = updatedConv.chatMessages)
                }
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

    // Lấy danh sách tin nhắn của hội thoại đang active
    val activeMessages: List<ChatMessage>
        get() = _conversations.find { it.id == activeConversationId }?.chatMessages ?: emptyList()

    // Lọc danh sách hội thoại theo ô tìm kiếm trong Drawer
    val filteredConversations: List<Conversation>
        get() = if (searchQuery.isEmpty()) {
            _conversations
        } else {
            _conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
}