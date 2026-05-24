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
import com.thinh.aistudybuddy.ui.theme.*

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

    val primaryCyan = PrimaryNeonTeal

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {
        // Space Ambient glows
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
                                    viewModel.leaveRoom()
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
                    WaitingForHostView("Preparing your challenge questions...", primaryCyan)
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
                            onStartQuiz = { viewModel.requestQuizSelection(state.roomCode) },
                            onStartFocus = { duration -> viewModel.requestStartFocus(state.roomCode, duration) }
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

@Composable
fun LobbyView(
    state: StudyRoomUiState.InLobby, 
    primaryColor: Color, 
    onStartQuiz: () -> Unit,
    onStartFocus: (Int) -> Unit
) {
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
                    state.roomCode, 
                    color = PrimaryNeonTeal, 
                    fontSize = 58.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    letterSpacing = 8.sp
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

        Spacer(Modifier.height(32.dp))
        Text(
            "COMMUNITY PARTICIPANTS", 
            color = Color.White.copy(alpha = 0.5f), 
            fontSize = 12.sp, 
            fontWeight = FontWeight.ExtraBold, 
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp), 
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(state.participants) { index, nickname ->
                val avatarGradient = when(index % 3) {
                    0 -> Brush.linearGradient(listOf(PrimaryNeonTeal, Color(0xFF00E5FF)))
                    1 -> Brush.linearGradient(listOf(SecondaryTangerine, Color(0xFFFFB300)))
                    else -> Brush.linearGradient(listOf(TertiaryCosmicIndigo, Color(0xFF651FFF)))
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(avatarGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                nickname.take(1).uppercase(), 
                                color = Color.Black, 
                                fontWeight = FontWeight.Black, 
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            nickname, 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.weight(1f))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EmeraldSuccess))
                            Spacer(Modifier.width(6.dp))
                            Text("Ready", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
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
                        .clickable { onStartFocus(25) },
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

                Button(
                    onClick = onStartQuiz,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(60.dp)
                        .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = SecondaryTangerine),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTangerine),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Quiz, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("START QUIZ", fontWeight = FontWeight.Black, color = Color.Black, letterSpacing = 0.5.sp)
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
}

@Composable
fun SelectingMaterialView(
    roomCode: String, 
    documents: List<com.thinh.aistudybuddy.data.models.Document>, 
    isLoading: Boolean, 
    primaryColor: Color, 
    onDocumentSelected: (com.thinh.aistudybuddy.data.models.Document) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(24.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .glassCard(shape = CircleShape, backgroundColor = PrimaryNeonTeal.copy(alpha = 0.1f))
                        .cyberBorder(shape = CircleShape, borderWidth = 1.dp, startColor = PrimaryNeonTeal, endColor = TertiaryCosmicIndigo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Description, null, tint = PrimaryNeonTeal)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Choose Material", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Select the source for the RAG-generated quiz", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
        
        Spacer(Modifier.height(28.dp))
        
        if (isLoading) {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { 
                CircularProgressIndicator(color = PrimaryNeonTeal, strokeWidth = 4.dp) 
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp), 
                modifier = Modifier.weight(1f)
            ) {
                items(documents) { doc ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                            .cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.dp, startColor = Color.White.copy(alpha = 0.05f), endColor = Color.White.copy(alpha = 0.05f))
                            .clickable { onDocumentSelected(doc) }
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .glassCard(shape = RoundedCornerShape(8.dp), backgroundColor = SecondaryTangerine.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null, tint = SecondaryTangerine, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    doc.fileName, 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Bold, 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "${doc.createdAt ?: "Recently Added"}", 
                                    color = Color.White.copy(alpha = 0.4f), 
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.3f))
                .cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.5.dp, startColor = PrimaryNeonTeal, endColor = TertiaryCosmicIndigo)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CloudUpload, null, tint = PrimaryNeonTeal)
                Spacer(Modifier.width(12.dp))
                Text("UPLOAD NEW SYLLABUS PDF", color = PrimaryNeonTeal, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
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

            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .size(80.dp)
                    .glassCard(shape = CircleShape, backgroundColor = PrimaryNeonTeal.copy(alpha = 0.1f))
                    .cyberBorder(shape = CircleShape, borderWidth = 1.5.dp, startColor = PrimaryNeonTeal, endColor = TertiaryCosmicIndigo),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    null, 
                    tint = PrimaryNeonTeal, 
                    modifier = Modifier.size(36.dp)
                )
            }
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
    onNext: () -> Unit
) {
    val question = state.questions.getOrNull(state.currentIndex) ?: return
    var selectedAnswer by remember(state.currentIndex) { mutableStateOf<String?>(null) }
    var timeRemaining by remember(state.currentIndex) { mutableStateOf(30f) }
    
    LaunchedEffect(state.endsAt) {
        if (state.endsAt > 0) {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = state.endsAt - now
                timeRemaining = (diff / 1000f).coerceAtLeast(0f)
                if (timeRemaining <= 0) {
                    if (selectedAnswer == null) {
                        onAnswer("", 0f)
                        selectedAnswer = "" 
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
                        .clickable(enabled = selectedAnswer == null) {
                            selectedAnswer = key
                            onAnswer(key, timeRemaining / 30f)
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
                onClick = onNext,
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
        }

        Spacer(Modifier.height(24.dp))
        
        Text(
            "LIVE STANDINGS PODIUM", 
            color = Color.White.copy(alpha = 0.4f), 
            fontSize = 11.sp, 
            fontWeight = FontWeight.ExtraBold, 
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val top3 = state.rankings.take(3)
            val displayList = List(3) { index -> top3.getOrNull(index) }
            
            displayList.forEachIndexed { i, player ->
                val rankColor = when(i) { 
                    0 -> Color(0xFFFFD700) 
                    1 -> Color(0xFFC0C0C0) 
                    else -> Color(0xFFCD7F32) 
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .animateContentSize()
                        .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                        .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = rankColor.copy(alpha = 0.4f), endColor = rankColor.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(rankColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${i+1}", 
                                color = rankColor, 
                                fontWeight = FontWeight.Black, 
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        if (player != null) {
                            Text(
                                player.username, 
                                color = Color.White, 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold, 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${player.score}", 
                                color = PrimaryNeonTeal, 
                                fontSize = 15.sp, 
                                fontWeight = FontWeight.Black
                            )
                        } else {
                            Text(
                                "--", 
                                color = Color.White.copy(alpha = 0.2f), 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "0", 
                                color = Color.White.copy(alpha = 0.2f), 
                                fontSize = 15.sp, 
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
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
