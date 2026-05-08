package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.data.models.QuizQuestion
import com.thinh.aistudybuddy.viewmodel.PlayerRanking
import com.thinh.aistudybuddy.viewmodel.StudyRoomUiState
import com.thinh.aistudybuddy.viewmodel.StudyRoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyRoomScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudyRoomViewModel = viewModel(),
    userDisplayName: String
) {
    val uiState by viewModel.uiState.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val isLoadingDocs by viewModel.isLoadingDocs.collectAsState()
    val isPreparingQuiz by viewModel.isPreparingQuiz.collectAsState()

    val primaryCyan = Color(0xFF00E5FF)
    val darkBg = Color(0xFF0F0F0F)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Co-Study Rooms", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState !is StudyRoomUiState.Initial) {
                            viewModel.leaveRoom()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        },
        containerColor = darkBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Background subtle gradient
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(darkBg, Color(0xFF1A1A1A)))
            ))

            if (isPreparingQuiz) {
                WaitingForHostView("Preparing your questions...", primaryCyan)
            } else {
                when (val state = uiState) {
                    is StudyRoomUiState.Initial -> InitialRoomView(
                        primaryColor = primaryCyan,
                        onJoin = { code -> viewModel.joinRoom(code, userDisplayName) },
                        onCreateRoom = { viewModel.createRoom(userDisplayName) }
                    )
                    is StudyRoomUiState.InLobby -> LobbyView(
                        state = state,
                        primaryColor = primaryCyan,
                        onStartQuiz = { viewModel.requestQuizSelection(state.roomCode) }
                    )
                    is StudyRoomUiState.SelectingMaterial -> SelectingMaterialView(
                        roomCode = state.roomCode,
                        documents = documents,
                        isLoading = isLoadingDocs,
                        primaryColor = primaryCyan,
                        onDocumentSelected = { doc -> viewModel.selectDocumentForQuiz(state.roomCode, doc) }
                    )
                    is StudyRoomUiState.WaitingForHost -> WaitingForHostView(state.message, primaryCyan)
                    is StudyRoomUiState.QuizActive -> QuizRoomView(
                        state = state,
                        primaryColor = primaryCyan,
                        onAnswer = { answer, ratio -> viewModel.submitAnswer(state.roomCode, answer, ratio) },
                        onNext = { viewModel.nextQuestion(state.roomCode) }
                    )
                    is StudyRoomUiState.Error -> ErrorView(state.message, onRetry = onNavigateBack)
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun InitialRoomView(primaryColor: Color, onJoin: (String) -> Unit, onCreateRoom: () -> Unit) {
    var roomCode by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = primaryColor.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Groups, null, tint = primaryColor, modifier = Modifier.size(50.dp))
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            "Ready to Study Together?",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Join a squad or start your own",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(48.dp))

        OutlinedTextField(
            value = roomCode,
            onValueChange = { if (it.length <= 6) roomCode = it.uppercase() },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center, 
                color = primaryColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            ),
            placeholder = { 
                Text(
                    "ROOM CODE", 
                    modifier = Modifier.fillMaxWidth(), 
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray,
                    letterSpacing = 2.sp
                ) 
            },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color(0xFF2C2C2E),
                focusedContainerColor = Color(0xFF1A1A1A),
                unfocusedContainerColor = Color(0xFF1A1A1A)
            ),
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onJoin(roomCode) },
            enabled = roomCode.length >= 4,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(if (roomCode.length >= 4) 12.dp else 0.dp, RoundedCornerShape(20.dp), spotColor = primaryColor),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                disabledContainerColor = Color(0xFF2C2C2E)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                "JOIN ROOM", 
                fontWeight = FontWeight.ExtraBold, 
                color = if (roomCode.length >= 4) Color.Black else Color.Gray,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onCreateRoom,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, primaryColor)
        ) {
            Icon(Icons.Default.Add, null, tint = primaryColor)
            Spacer(Modifier.width(8.dp))
            Text("CREATE NEW ROOM", fontWeight = FontWeight.Bold, color = primaryColor)
        }
    }
}

@Composable
fun LobbyView(state: StudyRoomUiState.InLobby, primaryColor: Color, onStartQuiz: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Room Info Card
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFF2C2C2E))
        ) {
            Column(
                modifier = Modifier.padding(32.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("LIVE ROOM CODE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                Text(state.roomCode, color = primaryColor, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp)
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                        Spacer(Modifier.width(8.dp))
                        Text("${state.participants.size} PLAYERS ONLINE", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("PARTICIPANTS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(state.participants) { nickname ->
                Surface(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF262626))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(
                                Brush.linearGradient(listOf(primaryColor, Color(0xFF00B8D4)))
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(nickname.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(nickname, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        Text("Ready", color = Color(0xFF4CAF50), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { /* Focus Logic */ },
                modifier = Modifier.weight(1f).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E))
            ) {
                Icon(Icons.Default.Timer, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("FOCUS", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onStartQuiz,
                modifier = Modifier.weight(1.5f).height(64.dp).shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFFFB300)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Quiz, null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("START QUIZ", fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }
    }
}

@Composable
fun SelectingMaterialView(roomCode: String, documents: List<com.thinh.aistudybuddy.data.models.Document>, isLoading: Boolean, primaryColor: Color, onDocumentSelected: (com.thinh.aistudybuddy.data.models.Document) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = primaryColor.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, null, tint = primaryColor) }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Choose Material", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Select the source for the quiz", color = Color.Gray, fontSize = 12.sp)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = primaryColor) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(documents) { doc ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onDocumentSelected(doc) },
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF262626))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = primaryColor, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.fileName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${doc.createdAt ?: "Recently"}", color = Color.Gray, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.DarkGray)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { /* Upload */ },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, primaryColor)
        ) {
            Icon(Icons.Default.CloudUpload, null, tint = primaryColor)
            Spacer(Modifier.width(12.dp))
            Text("UPLOAD NEW PDF", color = primaryColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WaitingForHostView(message: String, primaryColor: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(0.8f, 1.2f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse))

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            CircularProgressIndicator(
                modifier = Modifier.size(140.dp), 
                color = primaryColor, 
                strokeWidth = 4.dp,
                trackColor = Color(0xFF1E1E1E)
            )
            Icon(
                Icons.Default.AutoAwesome, 
                null, 
                tint = primaryColor.copy(alpha = 0.8f), 
                modifier = Modifier.size(60.dp).scale(scale)
            )
        }
        
        Spacer(Modifier.height(48.dp))
        
        Text(
            message,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "The challenge will start automatically as soon as the Host is ready.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun QuizRoomView(state: StudyRoomUiState.QuizActive, primaryColor: Color, onAnswer: (String, Float) -> Unit, onNext: () -> Unit) {
    val question = state.questions.getOrNull(state.currentIndex) ?: return
    var selectedAnswer by remember(state.currentIndex) { mutableStateOf<String?>(null) }
    var timeRemaining by remember(state.currentIndex) { mutableStateOf(30f) }
    
    LaunchedEffect(state.currentIndex) {
        while(timeRemaining > 0 && selectedAnswer == null) {
            delay(100)
            timeRemaining -= 0.1f
        }
        if (selectedAnswer == null) {
            onAnswer("", 0f)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Progress Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("QUESTION ${state.currentIndex + 1}", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("${state.questions.size} Questions Total", color = Color.Gray, fontSize = 11.sp)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                CircularProgressIndicator(
                    progress = { timeRemaining / 30f },
                    color = if (timeRemaining < 10) Color(0xFFFF5252) else primaryColor,
                    trackColor = Color(0xFF1E1E1E),
                    strokeWidth = 4.dp
                )
                Text("${timeRemaining.toInt()}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(32.dp))
        
        // Question Surface
        Surface(
            color = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF262626))
        ) {
            Text(
                question.question, 
                color = Color.White, 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(24.dp),
                lineHeight = 28.sp
            )
        }
        
        Spacer(Modifier.height(24.dp))

        // Options
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(question.options) { idx, text ->
                val key = when(idx) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" }
                val isSelected = selectedAnswer == key
                
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = selectedAnswer == null) {
                        selectedAnswer = key
                        onAnswer(key, timeRemaining / 30f)
                    },
                    color = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isSelected) primaryColor else Color(0xFF2C2C2E))
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isSelected) primaryColor else Color(0xFF2C2C2E)), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(key, color = if (isSelected) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(text, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Mini Leaderboard
        Text("LIVE STANDINGS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            state.rankings.take(3).forEachIndexed { i, r ->
                val rankColor = when(i) { 0 -> Color(0xFFFFD700); 1 -> Color(0xFFC0C0C0); else -> Color(0xFFCD7F32) }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, rankColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${i+1}st", color = rankColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(r.username, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${r.score}", color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(color = Color(0xFFFF5252).copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(80.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFFF5252), modifier = Modifier.size(40.dp)) }
        }
        Spacer(Modifier.height(24.dp))
        Text(message, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        Button(
            onClick = onRetry, 
            modifier = Modifier.padding(top = 32.dp).fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Go Back") }
    }
}

suspend fun delay(time: Long) {
    kotlinx.coroutines.delay(time)
}

private fun Modifier.scale(scale: Float): Modifier = this.graphicsLayer(
    scaleX = scale,
    scaleY = scale
)
