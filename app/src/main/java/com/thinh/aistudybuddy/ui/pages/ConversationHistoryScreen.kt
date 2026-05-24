package com.thinh.aistudybuddy.ui.pages

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
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.AlertDialog
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
import com.thinh.aistudybuddy.viewmodel.MindMapViewModel
import com.thinh.aistudybuddy.viewmodel.ConversationStudyPlanItem
import com.thinh.aistudybuddy.viewmodel.ConversationStudyPlanLessonItem
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Tab
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.style.TextAlign
import com.thinh.aistudybuddy.ui.theme.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

enum class HistoryTab {
    QUIZ, FLASHCARD, STUDY_PLAN, PDF
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
    conversationId: String,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenLesson: (planRawJson: String, lessonId: String) -> Unit,
    onOpenMindMap: (String) -> Unit, 
    timelineStatusByDocumentId: Map<String, ModuleStatus>,
    onRefresh: (String) -> Unit,
    onStartQuiz: (ChatMessage) -> Unit,
    onCheckPlan: (String?) -> Unit,
    chatViewModel: ChatViewModel = viewModel(),
    mindMapViewModel: MindMapViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(HistoryTab.QUIZ) }

    LaunchedEffect(conversationId) {
        onRefresh(conversationId)
        mindMapViewModel.loadHistory()
    }

    val conversation = chatViewModel.conversations.firstOrNull { it.id == conversationId }
    val allMessages = conversation?.chatMessages.orEmpty()
    
    val quizItems = allMessages.filter { it.showQuizButton }
        .distinctBy { it.id }
    
    val flashcardItems = allMessages.filter { it.showFlashcardButton || it.artifactType == "FLASHCARDS" }
        .distinctBy { it.id }
    
    val studyPlanItems = allMessages.filter { it.showStudyPlanButton }
        .distinctBy { it.id }
    
    val pdfItems = allMessages.filter { !it.attachmentName.isNullOrBlank() }
        .distinctBy { it.id }

    val screenTitle = if (conversation?.autoTitleApplied == true && conversation.title.isNotBlank()) {
        conversation.title
    } else {
        "Conversation History"
    }

    val currentItems = when (selectedTab) {
        HistoryTab.QUIZ -> quizItems
        HistoryTab.FLASHCARD -> flashcardItems
        HistoryTab.STUDY_PLAN -> studyPlanItems
        HistoryTab.PDF -> pdfItems
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {
        // Ambient floating canvas glows
        val infiniteTransition = rememberInfiniteTransition(label = "history_ambient")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_pulse"
        )
        val floatOffset by infiniteTransition.animateFloat(
            initialValue = -30f,
            targetValue = 30f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_float"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.25f + floatOffset),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.1f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.75f - floatOffset),
                    radius = size.width * 0.7f
                )
            )
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
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Open Chat", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = SurfaceCardContainer.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = when (selectedTab) {
                                HistoryTab.QUIZ -> PrimaryNeonTeal
                                HistoryTab.FLASHCARD -> SecondaryTangerine
                                HistoryTab.STUDY_PLAN -> PrimaryNeonTeal
                                HistoryTab.PDF -> SecondaryTangerine
                            }
                        )
                    }
                ) {
                    HistoryTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = when (tab) {
                                        HistoryTab.QUIZ -> "Quiz"
                                        HistoryTab.FLASHCARD -> "Flashcards"
                                        HistoryTab.STUDY_PLAN -> "Study Plan"
                                        HistoryTab.PDF -> "Docs"
                                    },
                                    color = if (selectedTab == tab) Color.White else Color.White.copy(alpha = 0.5f),
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (selectedTab) {
                                    HistoryTab.QUIZ -> Icons.Default.Quiz
                                    HistoryTab.FLASHCARD -> Icons.Default.Style
                                    HistoryTab.STUDY_PLAN -> Icons.Default.School
                                    HistoryTab.PDF -> Icons.Default.PictureAsPdf
                                },
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No history found for this category.",
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(currentItems) { message ->
                            HistoryArtifactCard(
                                message = message,
                                onStartQuiz = { onStartQuiz(message) },
                                onCheckPlan = { onCheckPlan(message.planJson) },
                                onOpenMindMap = { docId -> onOpenMindMap(docId) },
                                onOpenChat = {
                                    conversation?.let { onOpenConversation(it.id) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class LocalFlashcardItem(val front: String, val back: String)

private fun parseFlashcardsFromJson(jsonElement: com.google.gson.JsonElement?): List<LocalFlashcardItem> {
    if (jsonElement == null) return emptyList()
    return try {
        val gson = com.google.gson.Gson()
        if (jsonElement.isJsonArray) {
            val listType = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
            val rawList: List<Map<String, Any>> = gson.fromJson(jsonElement, listType)
            rawList.mapNotNull {
                val f = (it["front"] ?: it["question"])?.toString()
                val b = (it["back"] ?: it["answer"])?.toString()
                if (f != null && b != null) LocalFlashcardItem(f, b) else null
            }
        } else if (jsonElement.isJsonObject) {
            val obj = jsonElement.asJsonObject
            val array = obj.getAsJsonArray("flashcards") ?: obj.getAsJsonArray("cards")
            if (array != null) {
                val listType = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                val rawList: List<Map<String, Any>> = gson.fromJson(array, listType)
                rawList.mapNotNull {
                    val f = (it["front"] ?: it["question"])?.toString()
                    val b = (it["back"] ?: it["answer"])?.toString()
                    if (f != null && b != null) LocalFlashcardItem(f, b) else null
                }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        android.util.Log.e("ConversationHistory", "Failed to parse flashcards JSON", e)
        emptyList()
    }
}

@Composable
private fun HistoryArtifactCard(
    message: ChatMessage,
    onStartQuiz: () -> Unit,
    onCheckPlan: () -> Unit,
    onOpenMindMap: (String) -> Unit,
    onOpenChat: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showFlashcardDialog by remember { mutableStateOf(false) }

    val (icon, title, accent) = when {
        message.showQuizButton -> Triple(Icons.Default.Quiz, "Quiz", Color(0xFF1976D2))
        message.showFlashcardButton || message.artifactType == "FLASHCARDS" -> Triple(Icons.Default.Style, "Flashcards", Color(0xFFE91E63))
        message.showStudyPlanButton -> Triple(Icons.Default.School, "Study Plan", Color(0xFF4CAF50))
        !message.attachmentName.isNullOrBlank() -> Triple(Icons.Default.PictureAsPdf, "PDF Document", Color(0xFFE53935))
        message.showMindMapButton -> Triple(Icons.Default.AccountTree, "Mind Map", Color(0xFFFF9800))
        else -> Triple(Icons.Default.Info, "Document", Color.Gray)
    }

    if (showFlashcardDialog) {
        val cards = remember(message.artifactJson) { parseFlashcardsFromJson(message.artifactJson) }
        AlertDialog(
            onDismissRequest = { showFlashcardDialog = false },
            title = { Text("Flashcard List", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                if (cards.isEmpty()) {
                    Text("Failed to load flashcards or list is empty.", color = Color.Gray)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cards) { card ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text(text = "Front:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = card.front, color = Color.White, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "Back:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = card.back, color = Color(0xFF00E5FF), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFlashcardDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Close", color = Color.Black)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    val planTitleFromJson = remember(message.planJson) {
        if (message.planJson.isNullOrBlank()) null
        else runCatching<String?> { 
            com.google.gson.JsonParser().parse(message.planJson).asJsonObject.get("title")?.asString 
        }.getOrNull()
    }

    val specificName = when {
        !planTitleFromJson.isNullOrBlank() -> planTitleFromJson
        !message.specificTitle.isNullOrBlank() -> message.specificTitle
        !message.messageLabel.isNullOrBlank() -> message.messageLabel
        !message.attachmentName.isNullOrBlank() -> message.attachmentName
        message.courses.isNotEmpty() -> message.courses.first().title
        else -> null
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = specificName ?: title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message.createdAt,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    if (specificName != null) {
                        Text(text = title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                    val displayText = when {
                        !message.attachmentName.isNullOrBlank() -> "Document uploaded: ${message.attachmentName}"
                        message.showQuizButton -> "Quiz questions have been created."
                        message.showFlashcardButton || message.artifactType == "FLASHCARDS" -> "Flashcard deck has been created."
                        message.showStudyPlanButton -> "Study plan has been established."
                        else -> message.text
                    }
                    Text(
                        text = displayText.ifBlank { "Document is ready." },
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (message.showQuizButton) {
                            Button(
                                onClick = onStartQuiz,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Quiz", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        if (message.showFlashcardButton || message.artifactType == "FLASHCARDS") {
                            Button(
                                onClick = { showFlashcardDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Flashcards", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        if (message.showStudyPlanButton) {
                            Button(
                                onClick = onCheckPlan,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Plan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        if (!message.attachmentName.isNullOrBlank()) {
                            Button(
                                onClick = onOpenChat,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Go to Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        if (message.showMindMapButton && !message.documentId.isNullOrBlank()) {
                            Button(
                                onClick = { onOpenMindMap(message.documentId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.AccountTree, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Mind Map", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = accent.copy(alpha = 0.2f), thickness = 1.dp)
                }
            }
            
            if (!expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                val displayTextShort = when {
                    !message.attachmentName.isNullOrBlank() -> "Document uploaded: ${message.attachmentName}"
                    message.showQuizButton -> "Quiz questions have been created."
                    message.showFlashcardButton || message.artifactType == "FLASHCARDS" -> "Flashcard deck has been created."
                    message.showStudyPlanButton -> "Study plan has been established."
                    else -> message.text
                }
                Text(
                    text = displayTextShort.ifBlank { "Document is ready." },
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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(18.dp))
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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(18.dp))
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

