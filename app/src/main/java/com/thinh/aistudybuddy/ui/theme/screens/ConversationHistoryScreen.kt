package com.thinh.aistudybuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.data.model.ChatMessage
import com.thinh.aistudybuddy.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
    conversationId: String,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onRefresh: (String) -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    LaunchedEffect(conversationId) {
        onRefresh(conversationId)
    }

    val conversation = chatViewModel.conversations.firstOrNull { it.id == conversationId }
    val historyItems = conversation?.chatMessages.orEmpty().filter { it.showQuizButton || it.showStudyPlanButton }
    val screenTitle = if (conversation?.autoTitleApplied == true && conversation.title.isNotBlank()) {
        conversation.title
    } else {
        "Conversation History"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    if (conversation != null) {
                        Button(
                            onClick = { onOpenConversation(conversation.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open Chat", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = conversation?.title ?: "Conversation not found",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (historyItems.isEmpty()) {
                            "No quiz or study plan history yet."
                        } else {
                            "${historyItems.size} history item(s)"
                        },
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (historyItems.isEmpty()) {
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No quiz or plan history available.",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create a quiz or study plan in chat to see it here.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(historyItems) { message ->
                        HistoryArtifactCard(message = message)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryArtifactCard(message: ChatMessage) {
    val isQuiz = message.showQuizButton
    val (icon, title, accent) = when {
        isQuiz -> Triple(Icons.Default.Quiz, "Quiz", Color(0xFF00E5FF))
        else -> Triple(Icons.Default.School, "Study Plan", Color(0xFF4CAF50))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message.text.ifBlank { if (isQuiz) "Quiz is ready." else "Plan is ready." },
                color = Color.LightGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = accent.copy(alpha = 0.35f), thickness = 1.dp)
        }
    }
}




