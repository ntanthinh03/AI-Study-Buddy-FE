@file:Suppress("unused", "UNUSED_VALUE")

package com.thinh.aistudybuddy.ui.pages

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.ui.components.*
import com.thinh.aistudybuddy.data.models.*
import com.thinh.aistudybuddy.utils.VoiceManager
import com.thinh.aistudybuddy.viewmodel.*
import com.thinh.aistudybuddy.ui.theme.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userDisplayName: String,
    userAvatar: String? = null,
    quizViewModel: QuizViewModel,
    studyPlanViewModel: StudyPlanViewModel,
    onProfileClick: () -> Unit,
    onStartQuiz: (ChatMessage) -> Unit,
    onAccountClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStudyPlanClick: () -> Unit,
    onFlashcardsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onDailySessionClick: () -> Unit,
    onFocusClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onMockExamClick: () -> Unit,
    onStudyRoomClick: () -> Unit,
    onVersusArenaClick: () -> Unit,
    onMindMapClick: (String, String) -> Unit,
    onConversationHistoryClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
    analyticsViewModel: AnalyticsViewModel = viewModel(),
    flashcardViewModel: FlashcardViewModel = viewModel(),
    viewModel: ChatViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        analyticsViewModel.loadDashboard()
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    val voiceManager = remember { VoiceManager(context) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening()
        }
    }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
        onDispose {
            voiceManager.destroy()
            tts?.stop()
            tts?.shutdown()
        }
    }

    LaunchedEffect(voiceManager.recognizedText) {
        if (voiceManager.recognizedText.isNotBlank()) {
            inputText = voiceManager.recognizedText
        }
    }
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<String?>(null) }
    var newTitle by remember { mutableStateOf("") }
    val activeConversation = viewModel.conversations.find { it.id == viewModel.activeConversationId }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            val uriPath = uri.toString().lowercase()
            val hasAllowedExtension = uriPath.endsWith(".pdf") || uriPath.endsWith(".png") || uriPath.endsWith(".jpg") || uriPath.endsWith(".jpeg") || uriPath.endsWith(".webp") || uriPath.endsWith(".gif")
            val isAllowed = mimeType == "application/pdf" || mimeType?.startsWith("image/") == true || hasAllowedExtension
            if (isAllowed) {
                val fileName = runCatching {
                    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                    }
                }.getOrNull()
                
                if (mimeType == "application/pdf" || uriPath.endsWith(".pdf")) {
                    val pdfFileName = fileName ?: "attachment.pdf"
                    viewModel.setPendingPdf(uri, pdfFileName)
                } else if (mimeType?.startsWith("image/") == true || hasAllowedExtension) {
                    val imageName = fileName ?: when {
                        uriPath.endsWith(".png") -> "image.png"
                        uriPath.endsWith(".jpg") -> "image.jpg"
                        uriPath.endsWith(".jpeg") -> "image.jpeg"
                        uriPath.endsWith(".webp") -> "image.webp"
                        uriPath.endsWith(".gif") -> "image.gif"
                        else -> "image.jpg"
                    }
                    viewModel.setPendingImage(uri, imageName)
                } else {
                    viewModel.errorMessage = "Unsupported file type. Please choose a PDF or image."
                }
            } else {
                viewModel.errorMessage = "Unsupported file type. Please choose a PDF or image."
            }
        }
    }

    LaunchedEffect(viewModel.activeMessages.size, viewModel.isTyping) {
        if (viewModel.activeMessages.isNotEmpty() || viewModel.isTyping) {
            listState.animateScrollToItem(
                if (viewModel.isTyping) viewModel.activeMessages.size else viewModel.activeMessages.size - 1
            )
        }
    }

    LaunchedEffect(quizViewModel) {
        viewModel.onQuizGenerated = { questions ->
            quizViewModel.loadQuestions(questions)
        }
    }

    LaunchedEffect(studyPlanViewModel) {
        viewModel.onPlanGenerated = { rawJson ->
            studyPlanViewModel.loadStudyPlanFromJson(rawJson)
        }
    }

    LaunchedEffect(viewModel.sessionExpired) {
        if (viewModel.sessionExpired) {
            viewModel.consumeSessionExpired()
            onSessionExpired()
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        val message = viewModel.successMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeSuccessMessage()
    }

    LaunchedEffect(viewModel.errorMessage) {
        val error = viewModel.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.consumeErrorMessage()
    }


    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chat", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameConversation(viewModel.activeConversationId, newTitle)
                    showRenameDialog = false
                }) { Text("Save", color = Color(0xFF1976D2)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Chat", color = Color.White) },
            text = { Text("Are you sure you want to delete this conversation?", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    val id = conversationToDelete ?: viewModel.activeConversationId
                    if (id.isNotBlank()) {
                        viewModel.deleteConversation(id)
                    }
                    conversationToDelete = null
                    showDeleteDialog = false
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    conversationToDelete = null
                    showDeleteDialog = false 
                }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                userDisplayName = userDisplayName,
                userAvatar = userAvatar,
                searchQuery = viewModel.searchQuery,
                conversations = viewModel.filteredConversations,
                pendingConversations = viewModel.pendingConversations,
                activeConversationId = viewModel.activeConversationId,
                onNewChatClick = { viewModel.startNewChat(); scope.launch { drawerState.close() } },
                onConversationSelected = { id -> viewModel.selectConversation(id); scope.launch { drawerState.close() } },
                onDeleteConversationRequested = { conversation ->
                    conversationToDelete = conversation.id
                    showDeleteDialog = true
                    scope.launch { drawerState.close() }
                },
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onAccountClick = { scope.launch { drawerState.close() }; onAccountClick() },
                onSettingsClick = { scope.launch { drawerState.close() }; onSettingsClick() },
                onFlashcardsClick = { scope.launch { drawerState.close() }; onFlashcardsClick() },
                onAnalyticsClick = { scope.launch { drawerState.close() }; onAnalyticsClick() },
                onDailySessionClick = { scope.launch { drawerState.close() }; onDailySessionClick() },
                onFocusClick = { scope.launch { drawerState.close() }; onFocusClick() },
                onLeaderboardClick = { scope.launch { drawerState.close() }; onLeaderboardClick() },
                onMockExamClick = { scope.launch { drawerState.close() }; onMockExamClick() },
                onStudyRoomClick = { scope.launch { drawerState.close() }; onStudyRoomClick() },
                onVersusArenaClick = { scope.launch { drawerState.close() }; onVersusArenaClick() }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

            val infiniteTransition = rememberInfiniteTransition(label = "chat_ambient")
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
                        center = Offset(size.width * 0.2f, size.height * 0.25f + floatOffset),
                        radius = size.width * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.1f * pulseScale), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.75f - floatOffset),
                        radius = size.width * 0.65f
                    )
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
            viewModel.errorMessage?.let { message ->
                Surface(color = Color(0xFF2C2C2E)) {
                    Text(
                        text = message,
                        color = Color(0xFFFF6B6B),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            TopAppBar(
                title = {
                    val title = when {
                        viewModel.currentChatType == ChatScreenType.CONTINUING_CHAT -> {
                            val conversation = activeConversation
                            if (conversation?.autoTitleApplied == true && conversation.title.isNotBlank()) conversation.title else "Chat"
                        }
                        else -> "Buddy"
                    }
                    if (viewModel.currentChatType == ChatScreenType.CONTINUING_CHAT && viewModel.activeConversationId.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { onConversationHistoryClick(viewModel.activeConversationId) }) {
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null, tint = Color.White) }
                },
                actions = {
                    if (viewModel.currentChatType == ChatScreenType.CONTINUING_CHAT) {
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color(0xFF2C2C2E))) {
                                DropdownMenuItem(text = { Text("Pin", color = Color.White) }, leadingIcon = { Icon(Icons.Default.PushPin, null, tint = Color.White, modifier = Modifier.size(18.dp)) }, onClick = { showMenu = false })
                                DropdownMenuItem(text = { Text("Rename", color = Color.White) }, leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, tint = Color.White, modifier = Modifier.size(18.dp)) }, onClick = { showMenu = false; newTitle = activeConversation?.title ?: ""; showRenameDialog = true })
                                DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp)) }, onClick = { showDeleteDialog = true; showMenu = false })
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.35f), thickness = 0.5.dp)
                                DropdownMenuItem(text = { Text("Report a problem", color = Color.White) }, leadingIcon = { Icon(Icons.Default.Report, null, tint = Color.White, modifier = Modifier.size(18.dp)) }, onClick = { showMenu = false })
                            }
                        }
                    } else {
                        val streak = analyticsViewModel.gamificationStats?.currentStreak ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            if (streak > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFFF9800).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .clickable { onDailySessionClick() }
                                ) {
                                    Icon(Icons.Default.Whatshot, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "$streak", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                             UserAvatar(
                                 avatar = userAvatar,
                                 fullName = userDisplayName,
                                 size = 32.dp,
                                 onClick = onProfileClick
                             )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (viewModel.currentChatType == ChatScreenType.NEW_CHAT) {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        BuddyLogo(modifier = Modifier.size(220.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        NewChatView(
                            userDisplayName = userDisplayName,
                            suggestions = viewModel.suggestions,
                            banner = viewModel.banner,
                            onSuggestionClick = { suggestion ->
                                when (suggestion.title) {
                                    "Create a Quiz" -> viewModel.sendMessage("Create a quiz from this topic and prepare it for start.")
                                    "Summarize PDF" -> viewModel.sendMessage("Imported: ai_research_2026.pdf. Summarize this.")
                                    "Study Plan" -> viewModel.sendMessage("Create a study plan for ${suggestion.subtitle}")
                                    else -> viewModel.sendMessage("Give me tips for ${suggestion.subtitle}")
                                }
                            },
                            onBannerCtaClick = { }
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                } else if (viewModel.activeMessages.isEmpty() && !viewModel.isTyping) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                        itemsIndexed(viewModel.activeMessages) { _, message ->
                            val isFirstAiMessage = !message.isUser && viewModel.activeMessages.firstOrNull { !it.isUser }?.id == message.id
                            ChatBubble(
                                message = message,
                                onStartQuiz = onStartQuiz,
                                onCheckPlan = { planJson ->
                                    if (!planJson.isNullOrBlank()) {
                                        studyPlanViewModel.loadStudyPlanFromJson(planJson)
                                    }
                                    studyPlanViewModel.refreshProgressTimeline()
                                    onStudyPlanClick()
                                },
                                onGenerateFlashcards = { docId ->
                                    flashcardViewModel.focusDocument(docId)
                                    flashcardViewModel.generateFlashcards(docId) {
                                        onFlashcardsClick()
                                    }
                                },
                                onGenerateMindMap = { docId ->
                                    onMindMapClick(docId, activeConversation?.title ?: "Document")
                                },
                                onSpeakClick = { text ->
                                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                                },
                                showArtifactButtons = isFirstAiMessage
                            )
                        }
                        if (viewModel.isTyping && !viewModel.isUploading) {
                            item { TypingIndicator() }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    SnackbarHost(hostState = snackbarHostState)
                }
            }

            ChatInputBar(
                inputText = inputText,
                pendingAttachmentName = viewModel.pendingPdfName ?: viewModel.pendingImageName,
                isImage = viewModel.pendingImageName != null,
                onInputTextChange = { inputText = it },
                onSendMessageClick = {
                    if (!viewModel.pendingPdfName.isNullOrBlank()) {
                        viewModel.sendMessageWithPendingPdf(context, inputText)
                        inputText = ""
                    } else if (!viewModel.pendingImageName.isNullOrBlank()) {
                        viewModel.sendMessageWithPendingImage(context, inputText)
                        inputText = ""
                    } else if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                onRemoveAttachment = {
                    viewModel.clearPendingPdf()
                    viewModel.clearPendingImage()
                },
                onAddClick = { filePickerLauncher.launch("*/*") }
            )
        }
    }
}
}