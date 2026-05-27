package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.geometry.Offset
import com.thinh.aistudybuddy.data.models.QuizQuestion
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.MockExamUiState
import com.thinh.aistudybuddy.viewmodel.MockExamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamScreen(
    onNavigateBack: () -> Unit,
    viewModel: MockExamViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val isLoadingDocs by viewModel.isLoadingDocs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDocuments()
    }

    var selectedDocId by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "exam_ambient")
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
                    center = Offset(size.width * 0.15f, size.height * 0.2f + floatOffset),
                    radius = size.width * 0.55f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.8f - floatOffset),
                    radius = size.width * 0.55f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Exam Simulator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Real-time AI Verification", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (uiState is MockExamUiState.InProgress) showExitDialog = true
                            else onNavigateBack()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val state = uiState) {
                    is MockExamUiState.Initial -> {
                        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                                    .cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryNeonTeal.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Description, null, tint = PrimaryNeonTeal)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("Select Source Material", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("Choose which notes or PDFs Buddy should use to generate your exam", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, lineHeight = 16.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            if (isLoadingDocs) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = PrimaryNeonTeal)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                    items(documents) { doc ->
                                        val isSelected = selectedDocId == doc.id
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (isSelected) Modifier.cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.5.dp)
                                                    else Modifier
                                                )
                                                .glassCard(
                                                    shape = RoundedCornerShape(20.dp),
                                                    backgroundColor = if (isSelected) PrimaryNeonTeal.copy(alpha = 0.12f) else SurfaceCardContainer.copy(alpha = 0.4f),
                                                    borderColor = Color(0x1Fffffff)
                                                )
                                                .clickable { selectedDocId = doc.id }
                                        ) {
                                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isSelected) PrimaryNeonTeal.copy(alpha = 0.15f) else SurfaceContainerHigh.copy(alpha = 0.6f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.PictureAsPdf, null, tint = if (isSelected) PrimaryNeonTeal else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                                                }
                                                Spacer(Modifier.width(16.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(doc.fileName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                                                    Text("${doc.createdAt ?: "Recently"}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                                }
                                                if (isSelected) {
                                                    Icon(Icons.Default.CheckCircle, null, tint = PrimaryNeonTeal)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            Button(
                                onClick = { selectedDocId?.let { viewModel.startMockExam(20, it) } },
                                enabled = selectedDocId != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .then(
                                        if (selectedDocId != null) Modifier.cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.5.dp)
                                        else Modifier
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedDocId != null) PrimaryNeonTeal else SurfaceContainerHigh.copy(alpha = 0.4f),
                                    disabledContainerColor = SurfaceContainerHigh.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    "INITIATE AI MOCK EXAM",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedDocId != null) Color.Black else Color.White.copy(alpha = 0.3f),
                                    letterSpacing = 1.sp,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    is MockExamUiState.Loading -> PremiumLoadingView("Crafting your unique exam...", PrimaryNeonTeal)
                    is MockExamUiState.InProgress -> InProgressView(state, viewModel, PrimaryNeonTeal)
                    is MockExamUiState.Submitting -> PremiumLoadingView("Grading your exam...", PrimaryNeonTeal)
                    is MockExamUiState.Completed -> CompletedView(state, PrimaryNeonTeal, onNavigateBack)
                    is MockExamUiState.Error -> LocalErrorView(state.message, onNavigateBack)
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Quit Exam Simulator?", fontWeight = FontWeight.Bold) },
            text = { Text("Your ongoing simulator progress will be completely cleared. Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onNavigateBack()
                }) { Text("EXIT", color = RoseWarning, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("CONTINUE", color = Color.Gray) }
            },
            containerColor = SurfaceCardContainer,
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }
}

@Composable
fun InProgressView(state: MockExamUiState.InProgress, viewModel: MockExamViewModel, primaryColor: Color) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val question = state.questions.getOrNull(currentIndex) ?: return
    val selectedAnswer = viewModel.getSelectedAnswer(currentIndex)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("QUESTION ${currentIndex + 1}", color = primaryColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp)
                Text("${state.questions.size} Questions Total", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                CircularProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / state.questions.size },
                    color = primaryColor,
                    trackColor = SurfaceContainerLowest,
                    strokeWidth = 4.dp
                )
                Icon(Icons.Default.Timer, null, tint = primaryColor.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(28.dp))


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(24.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                .cyberBorder(shape = RoundedCornerShape(24.dp), borderWidth = 1.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXAM COMPREHENSION TOPIC",
                        color = primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = question.question,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            itemsIndexed(question.options) { index, text ->
                val key = when(index) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" }
                val isSelected = selectedAnswer == key

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSelected) Modifier.cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.5.dp)
                            else Modifier
                        )
                        .glassCard(
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else SurfaceCardContainer.copy(alpha = 0.4f),
                            borderColor = Color(0x1Fffffff)
                        )
                        .clickable { viewModel.selectAnswer(currentIndex, key) }
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) primaryColor else SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(key, color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(text, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentIndex > 0) {
                OutlinedButton(
                    onClick = { currentIndex-- },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text("PREVIOUS", fontWeight = FontWeight.Bold)
                }
            }

            val isLast = currentIndex == state.questions.size - 1
            Button(
                onClick = { if (isLast) viewModel.submitExam() else currentIndex++ },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .then(
                        if (isLast) Modifier.cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.5.dp)
                        else Modifier
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLast) primaryColor else SurfaceContainerHigh.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isLast) "FINISH EXAM" else "NEXT QUESTION",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLast) Color.Black else Color.White,
                    letterSpacing = 1.sp,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CompletedView(state: MockExamUiState.Completed, primaryColor: Color, onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .glassCard(shape = CircleShape, backgroundColor = SurfaceCardContainer.copy(alpha = 0.6f))
                .cyberBorder(shape = CircleShape, borderWidth = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${state.correctAnswers}/${state.totalQuestions}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = primaryColor)
                Text("SCORE", fontSize = 11.sp, color = primaryColor.copy(alpha = 0.6f), letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(40.dp))

        Text("Exam Completed!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = SecondaryTangerine.copy(alpha = 0.15f))
                .cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.dp, startColor = SecondaryTangerine, endColor = SecondaryTangerine)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text("🎉 You earned +${state.xpEarned} Mastery XP 🎉", color = SecondaryTangerine, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(56.dp))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.5.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("FINISH SIMULATOR", fontWeight = FontWeight.ExtraBold, color = Color.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun PremiumLoadingView(message: String, primaryColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_anim")
    val scale by infiniteTransition.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "loading_scale")
    val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "loading_rotation")

    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
                drawCircle(
                    brush = Brush.sweepGradient(colors = listOf(primaryColor, TertiaryCosmicIndigo, primaryColor)),
                    radius = size.minDimension / 2 * scale,
                    alpha = 0.3f
                )
            }
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .glassCard(shape = CircleShape, backgroundColor = SurfaceCardContainer.copy(alpha = 0.8f))
                    .cyberBorder(shape = CircleShape, borderWidth = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    null,
                    tint = primaryColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        Text(message, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Buddy AI is comprehensively analyzing your notes to generate tailored interactive queries.", color = Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
fun LocalErrorView(message: String, onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = RoseWarning, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(message, color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateBack,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
            modifier = Modifier.cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.dp)
        ) {
            Text("Back to Material Selection", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
