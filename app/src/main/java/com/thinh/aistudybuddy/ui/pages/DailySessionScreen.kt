package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.viewmodel.DailySessionViewModel
import com.thinh.aistudybuddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySessionScreen(
    onBack: () -> Unit,
    viewModel: DailySessionViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.loadDailySession()
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {
        // Space Ambient glows
        val infiniteTransition = rememberInfiniteTransition(label = "daily_session_ambient")
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
                    center = Offset(size.width * 0.15f, size.height * 0.2f + floatOffset),
                    radius = size.width * 0.55f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.1f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.8f - floatOffset),
                    radius = size.width * 0.6f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Daily Workout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Knowledge Retention Boost", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    actions = {
                        val streak = viewModel.stats?.currentStreak ?: 0
                        if (streak > 0) {
                            Row(
                                modifier = Modifier.padding(end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Whatshot, "Streak", tint = SecondaryTangerine, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$streak d",
                                    color = SecondaryTangerine,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent,
            content = { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    if (viewModel.loading && viewModel.session == null) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = PrimaryNeonTeal
                        )
                    } else if (viewModel.isFinished) {
                        SessionSummary(viewModel, onBack)
                    } else if (viewModel.session == null) {
                        EmptySessionState(viewModel)
                    } else {
                        val session = viewModel.session!!
                        val totalSteps = session.content.quizQuestions.size + session.content.flashcards.size
                        if (totalSteps == 0) {
                            EmptySessionState(viewModel)
                        } else {
                            ActiveSessionContent(viewModel)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ActiveSessionContent(viewModel: DailySessionViewModel) {
    val session = viewModel.session!!
    val quizQuestions = session.content.quizQuestions
    val flashcards = session.content.flashcards
    val totalSteps = quizQuestions.size + flashcards.size
    val currentStep = viewModel.currentStep
    val safeTotalSteps = totalSteps.coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Task ${currentStep + 1} of $safeTotalSteps",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${((currentStep).toFloat() / safeTotalSteps.toFloat() * 100).toInt()}% Done",
                color = PrimaryNeonTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / safeTotalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PrimaryNeonTeal,
            trackColor = SurfaceContainerLowest,
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (currentStep < quizQuestions.size) {
            val question = quizQuestions[currentStep]
            QuizStep(question) { isCorrect ->
                viewModel.submitAnswer(isCorrect)
            }
        } else {
            val flashcard = flashcards[currentStep - quizQuestions.size]
            FlashcardStep(flashcard) {
                viewModel.submitAnswer(true)
            }
        }
    }
}

@Composable
fun QuizStep(question: com.thinh.aistudybuddy.data.models.BackendQuizQuestion, onAnswer: (Boolean) -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                .padding(24.dp)
        ) {
            Text(
                text = question.question,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        val options: List<Pair<String, String>> = listOf(
            "A" to (question.options["A"] ?: ""),
            "B" to (question.options["B"] ?: ""),
            "C" to (question.options["C"] ?: ""),
            "D" to (question.options["D"] ?: "")
        )

        options.forEach { item ->
            val key = item.first
            val text = item.second
            if (text.isNotBlank()) {
                Surface(
                    color = Color.Transparent,
                    onClick = { onAnswer(key == question.correctAnswer) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(56.dp)
                        .glassCard(shape = RoundedCornerShape(14.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.3f))
                        .cyberBorder(shape = RoundedCornerShape(14.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.2f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(PrimaryNeonTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, PrimaryNeonTeal.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                color = PrimaryNeonTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = text,
                            color = Color.LightGray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardStep(flashcard: com.thinh.aistudybuddy.data.models.Flashcard, onDone: () -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "card_flip"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Daily Spaced Flashcard", color = PrimaryNeonTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tap to reveal the definition",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { revealed = !revealed },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .glassCard(shape = RoundedCornerShape(24.dp), borderColor = PrimaryNeonTeal.copy(alpha = 0.3f))
                    .cyberBorder(shape = RoundedCornerShape(24.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.3f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.3f)),
                color = Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    if (rotation <= 90f) {
                        Text(
                            text = flashcard.front,
                            color = Color.White,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = flashcard.back,
                            color = PrimaryNeonTeal,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.graphicsLayer { rotationY = 180f }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (!revealed) {
            Button(
                onClick = { revealed = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Show Answer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Got it!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SessionSummary(viewModel: DailySessionViewModel, onBack: () -> Unit) {
    val stats = viewModel.stats
    val infiniteTransition = rememberInfiniteTransition(label = "flame_scale")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            // Glowing background ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SecondaryTangerine.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width / 1.8f * pulseScale
                    )
                )
            }
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                tint = SecondaryTangerine,
                modifier = Modifier.size(100.dp).graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "${stats?.currentStreak ?: 0} Day Streak!",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = Typography.headlineLarge.fontFamily
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Daily Workout Completed Successfully",
            color = PrimaryNeonTeal,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.6f))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("XP Earned", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                        Text("+${viewModel.correctCount * 10 + 20}", color = SecondaryTangerine, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.1f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Accuracy", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                        Text("${(viewModel.correctCount.toFloat() / (viewModel.session?.content?.quizQuestions?.size?.plus(viewModel.session?.content?.flashcards?.size ?: 0) ?: 1).toFloat() * 100).toInt()}%", color = PrimaryNeonTeal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Back to Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun EmptySessionState(viewModel: DailySessionViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(24.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No sessions ready for today!",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Upload documents or generate a study plan to start your daily study routine.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
