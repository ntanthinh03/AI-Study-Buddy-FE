package com.thinh.aistudybuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.data.models.*
import com.thinh.aistudybuddy.viewmodel.ChatViewModel
import com.thinh.aistudybuddy.viewmodel.ConversationStudyPlanItem
import com.thinh.aistudybuddy.viewmodel.ConversationStudyPlanLessonItem

private enum class HistoryViewMode {
    ARTIFACTS,
    COURSES,
    LESSONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
    conversationId: String,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenLesson: (planRawJson: String, lessonId: String) -> Unit,
    timelineStatusByDocumentId: Map<String, ModuleStatus>,
    onRefresh: (String) -> Unit,
    onStartQuiz: (ChatMessage) -> Unit,
    onCheckPlan: (String?) -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    var viewMode by remember { mutableStateOf(HistoryViewMode.ARTIFACTS) }
    var selectedPlan by remember { mutableStateOf<ConversationStudyPlanItem?>(null) }

    LaunchedEffect(conversationId) {
        onRefresh(conversationId)
        viewMode = HistoryViewMode.ARTIFACTS
        selectedPlan = null
    }

    val conversation = chatViewModel.conversations.firstOrNull { it.id == conversationId }
    val historyItems = conversation?.chatMessages.orEmpty().filter { it.showQuizButton || it.showStudyPlanButton }
    val conversationPlans = chatViewModel.getConversationStudyPlans(conversationId)
    val screenTitle = if (conversation?.autoTitleApplied == true && conversation.title.isNotBlank()) {
        conversation.title
    } else {
        "Conversation History"
    }
    val summaryText = when (viewMode) {
        HistoryViewMode.ARTIFACTS -> if (historyItems.isEmpty()) {
            "No quiz or study plan history yet."
        } else {
            "${historyItems.size} history item(s)"
        }
        HistoryViewMode.COURSES -> if (conversationPlans.isEmpty()) {
            "No study plan found in this conversation."
        } else {
            "${conversationPlans.size} course(s)"
        }
        HistoryViewMode.LESSONS -> {
            val count = selectedPlan?.lessonCount ?: 0
            if (count == 0) "No lessons in this course." else "$count lesson(s)"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (viewMode) {
                            HistoryViewMode.LESSONS -> {
                                viewMode = HistoryViewMode.COURSES
                                selectedPlan = null
                            }
                            HistoryViewMode.COURSES -> viewMode = HistoryViewMode.ARTIFACTS
                            HistoryViewMode.ARTIFACTS -> onBack()
                        }
                    }) {
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
                        text = summaryText,
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewMode == HistoryViewMode.ARTIFACTS && historyItems.isEmpty()) {
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
            } else if (viewMode == HistoryViewMode.ARTIFACTS) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(historyItems) { message ->
                        HistoryArtifactCard(
                            message = message,
                            onStartQuiz = { onStartQuiz(message) },
                            onCheckPlan = { onCheckPlan(message.planJson) }
                        )
                    }
                }
            } else if (viewMode == HistoryViewMode.COURSES) {
                if (conversationPlans.isEmpty()) {
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
                                text = "No course plans found.",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Generate a study plan in this chat, then open history again.",
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
                        items(conversationPlans) { plan ->
                            CoursePlanCard(
                                title = plan.title,
                                lessonCount = plan.lessonCount,
                                onClick = {
                                    selectedPlan = plan
                                    viewMode = HistoryViewMode.LESSONS
                                }
                            )
                        }
                    }
                }
            } else {
                val lessons = selectedPlan?.lessons.orEmpty()
                if (lessons.isEmpty()) {
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
                                text = "No lessons found.",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(lessons) { lesson ->
                            val status = timelineStatusByDocumentId[lesson.documentId]
                            LessonItemCard(
                                lesson = lesson,
                                status = status,
                                onClick = {
                                    selectedPlan?.let { plan ->
                                        onOpenLesson(plan.rawJson, lesson.lessonId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryArtifactCard(
    message: ChatMessage,
    onStartQuiz: () -> Unit,
    onCheckPlan: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isQuiz = message.showQuizButton
    val (icon, title, accent) = when {
        isQuiz -> Triple(Icons.Default.Quiz, "Quiz", Color(0xFF00E5FF))
        else -> Triple(Icons.Default.School, "Study Plan", Color(0xFF4CAF50))
    }

    // Try to find a specific name for the quiz/course
    val specificName = when {
        !message.specificTitle.isNullOrBlank() -> message.specificTitle
        !message.messageLabel.isNullOrBlank() -> message.messageLabel
        !message.attachmentName.isNullOrBlank() -> message.attachmentName
        message.courses.isNotEmpty() -> message.courses.first().title
        else -> null
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (specificName != null) {
                        Text(
                            text = specificName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(text = title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    } else {
                        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message.text.ifBlank { if (isQuiz) "Quiz is ready." else "Plan is ready." },
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    
                    if (!message.isUser && (message.showQuizButton || message.showStudyPlanButton)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (message.showQuizButton) {
                                Button(
                                    onClick = onStartQuiz,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Quiz", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                            if (message.showStudyPlanButton) {
                                Button(
                                    onClick = onCheckPlan,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Course", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = accent.copy(alpha = 0.2f), thickness = 1.dp)
                }
            }
            
            if (!expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message.text.ifBlank { if (isQuiz) "Quiz is ready." else "Plan is ready." },
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CoursePlanCard(
    title: String,
    lessonCount: Int,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.School, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "$lessonCount lesson(s)", color = Color.LightGray, fontSize = 13.sp)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LessonItemCard(
    lesson: ConversationStudyPlanLessonItem,
    status: ModuleStatus?,
    onClick: () -> Unit
) {
    val badgeText = when (status) {
        ModuleStatus.COMPLETED -> "Completed"
        ModuleStatus.IN_PROGRESS -> "In Progress"
        else -> null
    }
    val badgeColor = when (status) {
        ModuleStatus.COMPLETED -> Color(0xFF4CAF50)
        ModuleStatus.IN_PROGRESS -> Color(0xFF00E5FF)
        else -> Color.Transparent
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.School, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = lesson.title, color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Lesson ${lesson.order}", color = Color.LightGray, fontSize = 12.sp)
            }
            if (badgeText != null) {
                Row(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.18f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(
                        modifier = Modifier
                            .size(7.dp)
                            .background(badgeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.size(8.dp))
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
        }
    }
}




