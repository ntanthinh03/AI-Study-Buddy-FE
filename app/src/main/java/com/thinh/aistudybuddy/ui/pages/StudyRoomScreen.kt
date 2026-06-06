package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
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
import com.thinh.aistudybuddy.viewmodel.QuizGenerationStatus
import com.thinh.aistudybuddy.ui.theme.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.content.Context
import android.net.Uri

private val StudyRoomSecondary = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyRoomScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudyRoomViewModel = viewModel(),
    userDisplayName: String
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQuitConfirmation by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState !is StudyRoomUiState.Initial) {
        showQuitConfirmation = true
    }
    val documents by viewModel.documents.collectAsState()
    val isLoadingDocs by viewModel.isLoadingDocs.collectAsState()
    val isPreparingQuiz by viewModel.isPreparingQuiz.collectAsState()

    val primaryCyan = PrimaryNeonTeal

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "study_room_ambient")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_pulse"
        )
        val floatOffset by infiniteTransition.animateFloat(
            initialValue = -35f,
            targetValue = 35f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = FastOutSlowInEasing),
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
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.75f - floatOffset),
                    radius = size.width * 0.6f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Co-Study Rooms", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (uiState !is StudyRoomUiState.Initial) {
                                    showQuitConfirmation = true
                                } else {
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.padding(start = 8.dp).background(SurfaceContainerHigh.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isPreparingQuiz) {
                    WaitingForHostView("Generating Quiz...", primaryCyan)
                } else {
                    when (val state = uiState) {
                        is StudyRoomUiState.Initial -> InitialRoomView(
                            primaryColor = primaryCyan,
                            onJoin = { code -> viewModel.joinRoom(code, userDisplayName) },
                            onCreateRoom = { viewModel.createRoom(userDisplayName) }
                        )
                        is StudyRoomUiState.InLobby -> LobbyView(
                            state = state,
                            viewModel = viewModel,
                            primaryColor = primaryCyan
                        )
                        is StudyRoomUiState.QuizCountdown -> StudyRoomCountdownView(
                            state = state,
                            onFinished = {
                                viewModel.startQuizAfterCountdown(
                                    roomCode = state.roomCode,
                                    questions = state.questions,
                                    endsAt = state.endsAt,
                                    isHost = state.isHost
                                )
                            }
                        )
                        is StudyRoomUiState.QuizActive -> QuizRoomView(
                            state = state,
                            primaryColor = primaryCyan,
                            onAnswer = { answer, ratio -> viewModel.submitAnswer(state.roomCode, answer, ratio) },
                            onNext = { viewModel.nextQuestion(state.roomCode) },
                            userDisplayName = userDisplayName
                        )
                        is StudyRoomUiState.QuizSummary -> QuizSummaryView(
                            state = state,
                            userDisplayName = userDisplayName,
                            onBackToLobby = { viewModel.goBackToLobby() },
                            onExit = { viewModel.exitRoom() }
                        )
                        is StudyRoomUiState.Error -> ErrorView(state.message, onRetry = onNavigateBack)
                        else -> {}
                    }
                }
            }
            
            if (showQuitConfirmation) {
                AlertDialog(
                    onDismissRequest = { showQuitConfirmation = false },
                    containerColor = DeepSpaceBackground.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Text("Quit Study Room?", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to leave the room? If you are the Host, this will end the session and cancel quiz generation.",
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 22.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = RoseWarning),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                showQuitConfirmation = false
                                viewModel.leaveRoom()
                            }
                        ) {
                            Text("LEAVE", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showQuitConfirmation = false }) {
                            Text("CANCEL", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.border(1.dp, RoseWarning.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                )
            }
        }
    }
}

@Composable
fun InitialRoomView(primaryColor: Color, onJoin: (String) -> Unit, onCreateRoom: () -> Unit) {
    var roomCode by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "initial_room_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "icon_scale"
        )

        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .glassCard(shape = CircleShape, backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                .cyberBorder(shape = CircleShape, borderWidth = 2.dp, startColor = PrimaryNeonTeal, endColor = TertiaryCosmicIndigo)
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Groups, 
                contentDescription = null, 
                tint = PrimaryNeonTeal, 
                modifier = Modifier.size(50.dp)
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            "Ready to Study Together?",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            "Join a workspace squad or initiate your own",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.3f))
                .cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.4f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.4f))
        ) {
            OutlinedTextField(
                value = roomCode,
                onValueChange = { if (it.length <= 6) roomCode = it.uppercase() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center, 
                    color = PrimaryNeonTeal,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                ),
                placeholder = { 
                    Text(
                        "ENTER ROOM CODE", 
                        modifier = Modifier.fillMaxWidth(), 
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.2f),
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ) 
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        Spacer(Modifier.height(28.dp))

        val isJoinEnabled = roomCode.length >= 4
        Button(
            onClick = { onJoin(roomCode) },
            enabled = isJoinEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(if (isJoinEnabled) 16.dp else 0.dp, RoundedCornerShape(20.dp), spotColor = PrimaryNeonTeal),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryNeonTeal,
                disabledContainerColor = SurfaceCardContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                "JOIN TEAM ROOM", 
                fontWeight = FontWeight.ExtraBold, 
                color = if (isJoinEnabled) Color.Black else Color.White.copy(alpha = 0.3f),
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceContainerHigh.copy(alpha = 0.4f))
                .cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.5.dp, startColor = PrimaryNeonTeal, endColor = TertiaryCosmicIndigo)
                .clickable { onCreateRoom() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Add, null, tint = PrimaryNeonTeal)
                Spacer(Modifier.width(8.dp))
                Text(
                    "CREATE NEW ROOM", 
                    fontWeight = FontWeight.ExtraBold, 
                    color = PrimaryNeonTeal,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyView(
    state: StudyRoomUiState.InLobby, 
    viewModel: StudyRoomViewModel,
    primaryColor: Color
) {
    val selectedDocName by viewModel.selectedDocumentName.collectAsState()
    val genStatus by viewModel.generationStatus.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val isLoadingDocs by viewModel.isLoadingDocs.collectAsState()

    var showDocSelector by remember { mutableStateOf(false) }
    var docToConfirm by remember { mutableStateOf<com.thinh.aistudybuddy.data.models.Document?>(null) }
    val context = LocalContext.current
    val externalFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAndSelectExternalDocument(context, uri, state.roomCode)
            showDocSelector = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(28.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                .cyberBorder(shape = RoundedCornerShape(28.dp), borderWidth = 1.5.dp, startColor = PrimaryNeonTeal, endColor = TertiaryCosmicIndigo)
        ) {
            Column(
                modifier = Modifier.padding(28.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "LIVE CHALLENGE ROOM CODE", 
                    color = Color.White.copy(alpha = 0.4f), 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold, 
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.roomCode, 
                    color = PrimaryNeonTeal, 
                    fontSize = 58.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(Modifier.height(20.dp))
                
                val infiniteTransition = rememberInfiniteTransition(label = "players_online_pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                
                Box(
                    modifier = Modifier
                        .glassCard(shape = CircleShape, backgroundColor = EmeraldSuccess.copy(alpha = 0.1f))
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.3f), CircleShape)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess.copy(alpha = alpha))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${state.participants.size} PLAYERS ONLINE", 
                            color = EmeraldSuccess, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "COMMUNITY PARTICIPANTS", 
            color = Color.White.copy(alpha = 0.5f), 
            fontSize = 11.sp, 
            fontWeight = FontWeight.ExtraBold, 
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp), 
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(state.participants) { index, nickname ->
                val avatarGradient = when(index % 3) {
                    0 -> Brush.linearGradient(listOf(PrimaryNeonTeal, Color(0xFF00E5FF)))
                    1 -> Brush.linearGradient(listOf(StudyRoomSecondary, Color(0xFF60A5FA)))
                    else -> Brush.linearGradient(listOf(TertiaryCosmicIndigo, Color(0xFF651FFF)))
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(avatarGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                nickname.take(1).uppercase(), 
                                color = Color.Black, 
                                fontWeight = FontWeight.Black, 
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            nickname, 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.weight(1f))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(EmeraldSuccess))
                            Spacer(Modifier.width(5.dp))
                            Text("Ready", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "STUDY MATERIAL SOURCE", 
            color = Color.White.copy(alpha = 0.5f), 
            fontSize = 11.sp, 
            fontWeight = FontWeight.ExtraBold, 
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                .cyberBorder(
                    shape = RoundedCornerShape(20.dp), 
                    borderWidth = 1.dp, 
                    startColor = if (selectedDocName != null) PrimaryNeonTeal.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f), 
                    endColor = if (selectedDocName != null) TertiaryCosmicIndigo.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
                )
                .clickable(enabled = state.isHost) { showDocSelector = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .glassCard(
                            shape = RoundedCornerShape(10.dp), 
                            backgroundColor = if (selectedDocName != null) PrimaryNeonTeal.copy(alpha = 0.1f) else SurfaceContainerHigh.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedDocName != null) Icons.Default.PictureAsPdf else Icons.Default.LibraryBooks,
                        contentDescription = null,
                        tint = if (selectedDocName != null) PrimaryNeonTeal else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (selectedDocName != null) {
                        Text(
                            text = selectedDocName!!,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(Modifier.height(4.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (genStatus) {
                                QuizGenerationStatus.GENERATING -> {
                                    val infiniteTransition = rememberInfiniteTransition("mascot_spin")
                                    val rotation by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
                                        label = "rotation"
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Cached,
                                        contentDescription = null,
                                        tint = StudyRoomSecondary,
                                        modifier = Modifier.size(12.dp).graphicsLayer(rotationZ = rotation)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "AI generating quiz questions under the hood...",
                                        color = StudyRoomSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                QuizGenerationStatus.READY -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "AI Quiz compiled & ready to launch!",
                                        color = EmeraldSuccess,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                QuizGenerationStatus.ERROR -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = RoseWarning,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Generation failed. Tap to retry compiling.",
                                        color = RoseWarning,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                else -> {
                                    Text(
                                        "Preparing background generation...",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No Material Selected",
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (state.isHost) "Tap to select a textbook PDF" else "Waiting for host to select syllabus",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 11.sp
                        )
                    }
                }

                if (state.isHost) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        
        if (state.isHost) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceContainerHigh.copy(alpha = 0.4f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.requestStartFocus(state.roomCode, 25) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Timer, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("FOCUS", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                val isStartQuizEnabled = selectedDocName != null
                Button(
                    onClick = { viewModel.startQuiz(state.roomCode) },
                    enabled = isStartQuizEnabled,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(60.dp)
                        .shadow(if (isStartQuizEnabled) 16.dp else 0.dp, RoundedCornerShape(20.dp), spotColor = StudyRoomSecondary),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudyRoomSecondary,
                        disabledContainerColor = SurfaceCardContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        Icons.Default.Quiz, 
                        null, 
                        tint = if (isStartQuizEnabled) Color.Black else Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "START QUIZ", 
                        fontWeight = FontWeight.Black, 
                        color = if (isStartQuizEnabled) Color.Black else Color.White.copy(alpha = 0.3f), 
                        letterSpacing = 0.5.sp
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                    .cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.3f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "guest_waiting_dots")
                    val alpha1 by infiniteTransition.animateFloat(0.2f, 1.0f, infiniteRepeatable(tween(600, delayMillis = 0), RepeatMode.Reverse))
                    val alpha2 by infiniteTransition.animateFloat(0.2f, 1.0f, infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse))
                    val alpha3 by infiniteTransition.animateFloat(0.2f, 1.0f, infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse))
                    
                    Text("Waiting for Host to start", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(".", color = Color.White.copy(alpha = alpha1), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(".", color = Color.White.copy(alpha = alpha2), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(".", color = Color.White.copy(alpha = alpha3), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    if (showDocSelector && state.isHost) {
        AlertDialog(
            onDismissRequest = { showDocSelector = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(DeepSpaceBackground.copy(alpha = 0.95f))
                .cyberBorder(shape = RoundedCornerShape(28.dp), borderWidth = 1.5.dp, startColor = PrimaryNeonTeal, endColor = TertiaryCosmicIndigo),
            title = {
                Text(
                    text = "Select Study Material",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Select the PDF textbook/syllabus for the AI to dynamically compile quiz questions.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = PrimaryNeonTeal.copy(alpha = 0.08f))
                            .clickable {
                                externalFilePickerLauncher.launch("application/pdf")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryNeonTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Upload, null, tint = PrimaryNeonTeal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Import PDF from Storage",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Upload a new syllabus to generate AI quiz instantly",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Icon(Icons.Default.Add, null, tint = PrimaryNeonTeal)
                        }
                    }
                    
                    if (isLoadingDocs) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryNeonTeal)
                        }
                    } else if (documents.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No documents uploaded yet.", color = Color.White.copy(alpha = 0.4f))
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(documents) { doc ->
                                 val docStatus = (doc.summaryStatus ?: doc.status).trim().uppercase()
                                 val isReady = docStatus == "COMPLETED"
                                 val statusText = when (docStatus) {
                                     "COMPLETED" -> "Ready"
                                     "FAILED" -> "Failed"
                                     else -> "Processing..."
                                 }
                                 val statusColor = when (docStatus) {
                                     "COMPLETED" -> EmeraldSuccess
                                     "FAILED" -> RoseWarning
                                     else -> SecondaryTangerine
                                 }

                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .glassCard(
                                             shape = RoundedCornerShape(16.dp),
                                             backgroundColor = if (isReady) SurfaceCardContainer.copy(alpha = 0.4f) else SurfaceCardContainer.copy(alpha = 0.15f)
                                         )
                                         .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = Color.White.copy(alpha = 0.05f), endColor = Color.White.copy(alpha = 0.05f))
                                         .clickable(enabled = isReady) {
                                             docToConfirm = doc
                                             showDocSelector = false
                                         }
                                 ) {
                                     Row(
                                         modifier = Modifier.padding(16.dp),
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Box(
                                             modifier = Modifier
                                                 .size(36.dp)
                                                 .glassCard(shape = RoundedCornerShape(8.dp), backgroundColor = if (isReady) StudyRoomSecondary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)),
                                             contentAlignment = Alignment.Center
                                         ) {
                                             Icon(
                                                 imageVector = Icons.Default.PictureAsPdf,
                                                 contentDescription = null,
                                                 tint = if (isReady) StudyRoomSecondary else Color.White.copy(alpha = 0.2f),
                                                 modifier = Modifier.size(20.dp)
                                             )
                                         }
                                         Spacer(Modifier.width(12.dp))
                                         Column(modifier = Modifier.weight(1f)) {
                                             Text(
                                                 text = doc.fileName,
                                                 color = if (isReady) Color.White else Color.White.copy(alpha = 0.4f),
                                                 fontWeight = FontWeight.Bold,
                                                 fontSize = 14.sp,
                                                 maxLines = 1,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                             Row(
                                                 modifier = Modifier.padding(top = 2.dp),
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text(
                                                     text = doc.createdAt ?: "Recently Added",
                                                     color = Color.White.copy(alpha = 0.3f),
                                                     fontSize = 11.sp
                                                 )
                                                 Spacer(Modifier.width(8.dp))
                                                 Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(statusColor))
                                                 Spacer(Modifier.width(4.dp))
                                                 Text(
                                                     text = statusText,
                                                     color = statusColor,
                                                     fontSize = 11.sp,
                                                     fontWeight = FontWeight.Bold
                                                 )
                                             }
                                         }
                                         if (isReady) {
                                             Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
                                         }
                                     }
                                 }
                             }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocSelector = false }) {
                    Text("CLOSE", color = PrimaryNeonTeal, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (docToConfirm != null) {
        AlertDialog(
            onDismissRequest = { docToConfirm = null },
            containerColor = DeepSpaceBackground.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (selectedDocName == null) "Generate AI Quiz?" else "Change Study Material?",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = if (selectedDocName == null) {
                        "Confirm selecting \"${docToConfirm?.fileName}\" for the dynamic quiz? AI will begin parsing the content under the hood immediately."
                    } else {
                        "You are changing the syllabus document to \"${docToConfirm?.fileName}\". This will immediately cancel the active quiz generation for the previous file to prevent server congestion and start fresh. Proceed?"
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        docToConfirm?.let { doc ->
                            viewModel.selectDocumentForQuiz(state.roomCode, doc)
                        }
                        docToConfirm = null
                    }
                ) {
                    Text("CONFIRM", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { docToConfirm = null }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
            },
            modifier = Modifier.border(1.dp, PrimaryNeonTeal.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        )
    }
}

@Composable
fun WaitingForHostView(message: String, primaryColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "waiting_for_host_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer(rotationZ = rotation)
            ) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(PrimaryNeonTeal, TertiaryCosmicIndigo, PrimaryNeonTeal)),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }

            InteractiveMascot(
                mode = "BALANCED",
                modifier = Modifier
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .size(100.dp)
                    .offset(y = 12.dp)
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
            "The challenge session will synchronize automatically as soon as the Host acts.",
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 20.sp
        )
    }
}

@Composable
fun QuizRoomView(
    state: StudyRoomUiState.QuizActive, 
    primaryColor: Color, 
    onAnswer: (String, Float) -> Unit, 
    onNext: () -> Unit,
    userDisplayName: String
) {
    val question = state.questions.getOrNull(state.currentIndex) ?: return
    var selectedAnswer by remember(state.currentIndex) { mutableStateOf<String?>(null) }
    var hasSubmitted by remember(state.currentIndex) { mutableStateOf(false) }
    var timeRemaining by remember(state.currentIndex) { mutableStateOf(30f) }
    
    LaunchedEffect(state.endsAt) {
        if (state.endsAt > 0) {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = state.endsAt - now
                timeRemaining = (diff / 1000f).coerceAtLeast(0f)
                if (timeRemaining <= 0) {
                    if (!hasSubmitted) {
                        onAnswer(selectedAnswer ?: "", 0f)
                        hasSubmitted = true
                    }
                    if (state.isHost) {
                        onNext()
                    }
                    break
                }
                delay(100)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "QUESTION ${state.currentIndex + 1}/${state.questions.size}", 
                    color = PrimaryNeonTeal, 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
                if (state.totalPlayers > 0) {
                    Text(
                        "${state.answeredCount}/${state.totalPlayers} players answered", 
                        color = Color.White.copy(alpha = 0.4f), 
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            Box(
                contentAlignment = Alignment.Center, 
                modifier = Modifier.size(54.dp)
            ) {
                val progress = (timeRemaining / 30f).coerceIn(0f, 1f)
                val strokeColor = if (timeRemaining < 6) RoseWarning else PrimaryNeonTeal
                
                CircularProgressIndicator(
                    progress = { progress },
                    color = strokeColor,
                    trackColor = SurfaceContainerLowest,
                    strokeWidth = 4.dp
                )
                Text(
                    "${timeRemaining.toInt()}", 
                    color = Color.White, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(24.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                .cyberBorder(shape = RoundedCornerShape(24.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.4f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = PrimaryNeonTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "AI SYLLABUS MULTI-CHALLENGE",
                        color = PrimaryNeonTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    question.question, 
                    color = Color.White, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(question.options) { idx, text ->
                val key = when(idx) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" }
                val isSelected = selectedAnswer == key
                
                val cardColor = if (isSelected) PrimaryNeonTeal.copy(alpha = 0.12f) else SurfaceCardContainer.copy(alpha = 0.3f)
                val borderGradient = if (isSelected) {
                    listOf(PrimaryNeonTeal, Color(0xFF00E5FF))
                } else {
                    listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = cardColor)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(colors = borderGradient),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(enabled = !hasSubmitted) {
                            selectedAnswer = key
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryNeonTeal else Color.White.copy(alpha = 0.08f)), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                key, 
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.5f), 
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text, 
                            color = Color.White, 
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (state.isHost) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!hasSubmitted) {
                        onAnswer(selectedAnswer ?: "", timeRemaining / 30f)
                        hasSubmitted = true
                    }
                    onNext()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryNeonTeal),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (state.currentIndex < state.questions.size - 1) "NEXT QUESTION" else "FINISH MULTI-QUIZ",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        } else {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!hasSubmitted && selectedAnswer != null) {
                        onAnswer(selectedAnswer!!, timeRemaining / 30f)
                        hasSubmitted = true
                    }
                },
                enabled = !hasSubmitted && selectedAnswer != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = if (hasSubmitted) Color.Transparent else PrimaryNeonTeal),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasSubmitted) SurfaceCardContainer.copy(alpha = 0.5f) else PrimaryNeonTeal,
                    disabledContainerColor = SurfaceCardContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (hasSubmitted) "ANSWER SUBMITTED" else "SUBMIT ANSWER",
                    color = if (hasSubmitted) Color.White.copy(alpha = 0.5f) else Color.Black,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Text(
            "LIVE STANDINGS", 
            color = Color.White.copy(alpha = 0.4f), 
            fontSize = 11.sp, 
            fontWeight = FontWeight.ExtraBold, 
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.3f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rankings = state.rankings
            val myRankIndex = rankings.indexOfFirst { it.username == userDisplayName }
            val showRankings = rankings.take(3)
            
            showRankings.forEachIndexed { index, player ->
                val rankColor = when(index) {
                    0 -> Color(0xFFFFD700)
                    1 -> Color(0xFFC0C0C0)
                    2 -> Color(0xFFCD7F32)
                    else -> Color.White.copy(alpha = 0.6f)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (player.username == userDisplayName) PrimaryNeonTeal.copy(alpha = 0.1f) 
                            else Color.Transparent, 
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "#${index + 1}",
                        color = rankColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        player.username,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${player.score} XP",
                        color = PrimaryNeonTeal,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
            
            if (myRankIndex >= 3) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                val myPlayer = rankings[myRankIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryNeonTeal.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "#${myRankIndex + 1}",
                        color = PrimaryNeonTeal,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        "${myPlayer.username} (You)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${myPlayer.score} XP",
                        color = PrimaryNeonTeal,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp), 
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .glassCard(shape = CircleShape, backgroundColor = RoseWarning.copy(alpha = 0.1f))
                .border(1.5.dp, RoseWarning, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ErrorOutline, 
                contentDescription = null, 
                tint = RoseWarning, 
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            message, 
            color = Color.White, 
            textAlign = TextAlign.Center, 
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        )
        
        Button(
            onClick = onRetry, 
            modifier = Modifier
                .padding(top = 36.dp)
                .fillMaxWidth()
                .height(56.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = RoseWarning.copy(alpha = 0.5f)),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh),
            shape = RoundedCornerShape(16.dp)
        ) { 
            Text("RETURN TO CO-STUDY HUB", fontWeight = FontWeight.Bold, color = Color.White) 
        }
    }
}

suspend fun delay(time: Long) {
    kotlinx.coroutines.delay(time)
}

private fun Modifier.scale(scale: Float): Modifier = this.graphicsLayer(
    scaleX = scale,
    scaleY = scale
)

@Composable
fun QuizSummaryView(
    state: StudyRoomUiState.QuizSummary,
    userDisplayName: String,
    onBackToLobby: () -> Unit,
    onExit: () -> Unit
) {
    var expandedReviewIndex by remember { mutableStateOf<Int?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(100.dp)
                .glassCard(shape = CircleShape, backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                .cyberBorder(shape = CircleShape, borderWidth = 2.dp, startColor = StudyRoomSecondary, endColor = PrimaryNeonTeal),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.EmojiEvents, 
                contentDescription = null, 
                tint = StudyRoomSecondary, 
                modifier = Modifier.size(54.dp)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "Challenge Completed!",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        
        Text(
            "Final Standings for Room ${state.roomCode}",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        

        Text(
            "FINAL LEADERBOARD", 
            color = Color.White.copy(alpha = 0.4f), 
            fontSize = 11.sp, 
            fontWeight = FontWeight.ExtraBold, 
            letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(10.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(24.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                .cyberBorder(shape = RoundedCornerShape(24.dp), borderWidth = 1.dp, startColor = Color.White.copy(alpha = 0.05f), endColor = Color.White.copy(alpha = 0.05f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.rankings.forEachIndexed { idx, player ->
                val isMe = player.username == userDisplayName
                val rankColor = when(idx) {
                    0 -> Color(0xFFFFD700)
                    1 -> Color(0xFFC0C0C0)
                    2 -> Color(0xFFCD7F32)
                    else -> Color.White.copy(alpha = 0.5f)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isMe) PrimaryNeonTeal.copy(alpha = 0.12f) else Color.Transparent, 
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${idx + 1}",
                        color = rankColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        modifier = Modifier.width(36.dp)
                    )
                    
                    Text(
                        text = player.username + (if (isMe) " (You)" else ""),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = "${player.score} XP",
                        color = PrimaryNeonTeal,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        

        Text(
            "CHALLENGE QUIZ REVIEW", 
            color = Color.White.copy(alpha = 0.4f), 
            fontSize = 11.sp, 
            fontWeight = FontWeight.ExtraBold, 
            letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(10.dp))
        
        state.questions.forEachIndexed { qIdx, question ->
            val userAnswer = state.userAnswers[qIdx] ?: ""
            val isCorrect = question.correctAnswerIndex == when(userAnswer) {
                "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3; else -> -1
            }
            
            val isExpanded = expandedReviewIndex == qIdx
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.3f))
                    .cyberBorder(
                        shape = RoundedCornerShape(20.dp), 
                        borderWidth = 1.dp, 
                        startColor = if (userAnswer.isEmpty()) Color.White.copy(alpha = 0.05f) 
                                     else if (isCorrect) EmeraldSuccess.copy(alpha = 0.4f) 
                                     else RoseWarning.copy(alpha = 0.4f),
                        endColor = Color.White.copy(alpha = 0.05f)
                    )
                    .clickable { expandedReviewIndex = if (isExpanded) null else qIdx }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (userAnswer.isEmpty()) Color.White.copy(alpha = 0.08f)
                                    else if (isCorrect) EmeraldSuccess.copy(alpha = 0.15f)
                                    else RoseWarning.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${qIdx + 1}",
                                color = if (userAnswer.isEmpty()) Color.White.copy(alpha = 0.6f)
                                        else if (isCorrect) EmeraldSuccess
                                        else RoseWarning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        Text(
                            text = question.question,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = if (isExpanded) 100 else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f)
                        )
                    }
                    
                    if (isExpanded) {
                        Spacer(Modifier.height(16.dp))
                        
                        question.options.forEachIndexed { oIdx, opt ->
                            val optionKey = when(oIdx) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" }
                            val isCorrectOption = question.correctAnswerIndex == oIdx
                            val isUserSelected = userAnswer == optionKey
                            
                            val optBg = if (isCorrectOption) EmeraldSuccess.copy(alpha = 0.12f)
                                        else if (isUserSelected) RoseWarning.copy(alpha = 0.12f)
                                        else Color.White.copy(alpha = 0.03f)
                                        
                            val optBorder = if (isCorrectOption) EmeraldSuccess
                                            else if (isUserSelected) RoseWarning
                                            else Color.White.copy(alpha = 0.08f)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(optBg)
                                    .border(1.dp, optBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isCorrectOption) EmeraldSuccess else if (isUserSelected) RoseWarning else Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = optionKey,
                                        color = if (isCorrectOption || isUserSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                
                                Spacer(Modifier.width(12.dp))
                                
                                Text(
                                    text = opt,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        if (question.explanation.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Explanation: ${question.explanation}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(36.dp))
        

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceContainerHigh.copy(alpha = 0.4f))
                    .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = Color.White.copy(alpha = 0.2f), endColor = Color.White.copy(alpha = 0.2f))
                    .clickable { onExit() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EXIT HUB",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
            
            Button(
                onClick = onBackToLobby,
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryNeonTeal),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "BACK TO LOBBY",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(Modifier.height(48.dp))
    }
}
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StudyRoomCountdownView(
    state: StudyRoomUiState.QuizCountdown,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var count by remember { mutableStateOf(3) }
    var showStartText by remember { mutableStateOf(false) }

    fun triggerVibration(durationMs: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    LaunchedEffect(Unit) {
        triggerVibration(70L)
        delay(1000)
        
        count = 2
        triggerVibration(70L)
        delay(1000)
        
        count = 1
        triggerVibration(70L)
        delay(1000)
        
        showStartText = true
        triggerVibration(400L)
        delay(800)
        
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = if (showStartText) "START!" else count.toString(),
            transitionSpec = {
                (fadeIn(animationSpec = tween(200, easing = EaseInQuad)) + scaleIn(initialScale = 0.3f, animationSpec = tween(300, easing = EaseOutBack))) togetherWith
                        fadeOut(animationSpec = tween(200, easing = EaseOutQuad)) + scaleOut(targetScale = 1.8f, animationSpec = tween(200))
            },
            label = "countdown_animation"
        ) { targetCount ->
            Text(
                text = targetCount,
                color = if (showStartText) StudyRoomSecondary else PrimaryNeonTeal,
                fontSize = if (showStartText) 72.sp else 120.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
