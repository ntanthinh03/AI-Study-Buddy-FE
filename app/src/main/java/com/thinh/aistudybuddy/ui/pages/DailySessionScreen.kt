package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NavigateNext
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
                    } else if (viewModel.reviewModeActive) {
                        QuizReviewContent(viewModel)
                    } else if (viewModel.wasAlreadyCompleted && !viewModel.reviewModeActive) {
                        AlreadyCompletedState(viewModel)
                    } else if (viewModel.session == null) {
                        EmptySessionState(viewModel)
                    } else {
                        val session = viewModel.session!!
                        val totalSteps = session.content.quizQuestions.size
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
    val totalSteps = quizQuestions.size
    val currentStep = viewModel.currentStep
    val safeTotalSteps = totalSteps.coerceAtLeast(1)

    val question = quizQuestions[currentStep]

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Question ${currentStep + 1} of $safeTotalSteps",
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
            progress = { (currentStep).toFloat() / safeTotalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PrimaryNeonTeal,
            trackColor = SurfaceContainerLowest,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Question display
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

        Spacer(modifier = Modifier.height(24.dp))

        // Options
        val options = listOf(
            "A" to (question.options["A"] ?: ""),
            "B" to (question.options["B"] ?: ""),
            "C" to (question.options["C"] ?: ""),
            "D" to (question.options["D"] ?: "")
        )

        options.forEach { item ->
            val key = item.first
            val text = item.second
            if (text.isNotBlank()) {
                val isSelected = viewModel.selectedOption == key
                val showFeedback = viewModel.showFeedback
                val isCorrectAnswer = key == question.correctAnswer

                val cardBgColor = when {
                    showFeedback && isCorrectAnswer -> EmeraldSuccess.copy(alpha = 0.2f)
                    showFeedback && isSelected && !isCorrectAnswer -> RoseWarning.copy(alpha = 0.2f)
                    isSelected -> PrimaryNeonTeal.copy(alpha = 0.15f)
                    else -> SurfaceCardContainer.copy(alpha = 0.3f)
                }

                val cardBorderColor = when {
                    showFeedback && isCorrectAnswer -> EmeraldSuccess
                    showFeedback && isSelected && !isCorrectAnswer -> RoseWarning
                    isSelected -> PrimaryNeonTeal
                    else -> Color.White.copy(alpha = 0.1f)
                }

                Surface(
                    color = Color.Transparent,
                    onClick = { viewModel.selectOption(key) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .height(56.dp)
                        .glassCard(shape = RoundedCornerShape(14.dp), backgroundColor = cardBgColor)
                        .border(1.dp, cardBorderColor, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (isSelected) PrimaryNeonTeal.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, if (isSelected) PrimaryNeonTeal else Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                color = if (isSelected) PrimaryNeonTeal else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = text,
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Explanation & Next controls
        if (viewModel.showFeedback) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                    .border(1.dp, PrimaryNeonTeal.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (viewModel.selectedOption == question.correctAnswer) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (viewModel.selectedOption == question.correctAnswer) EmeraldSuccess else RoseWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Explanation",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = question.explanation,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.nextStep() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (currentStep == totalSteps - 1) "Review Quiz Workout" else "Next Question",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.NavigateNext, contentDescription = null)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.confirmAnswer(question.correctAnswer) },
                enabled = viewModel.selectedOption != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryNeonTeal,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Confirm Answer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun QuizReviewContent(viewModel: DailySessionViewModel) {
    val session = viewModel.session!!
    val quizQuestions = session.content.quizQuestions
    val userAnswers = viewModel.userAnswers

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Quiz Review",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Text(
            text = "Review all questions and correct answers for today's Daily Workout.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        quizQuestions.forEachIndexed { index, question ->
            val selected = userAnswers[index]
            val isCorrect = selected == question.correctAnswer

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .glassCard(shape = RoundedCornerShape(18.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                    .border(
                        1.dp,
                        if (isCorrect) EmeraldSuccess.copy(alpha = 0.4f) else RoseWarning.copy(alpha = 0.4f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${index + 1}",
                            color = if (isCorrect) EmeraldSuccess else RoseWarning,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isCorrect) EmeraldSuccess else RoseWarning,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = question.question,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Show Options Summary
                    listOf("A", "B", "C", "D").forEach { opt ->
                        val optText = question.options[opt] ?: ""
                        if (optText.isNotBlank()) {
                            val wasSelected = selected == opt
                            val isCorrectAnswer = opt == question.correctAnswer
                            val textColor = when {
                                isCorrectAnswer -> EmeraldSuccess
                                wasSelected -> RoseWarning
                                else -> Color.White.copy(alpha = 0.5f)
                            }
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(textColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .border(1.dp, textColor, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(opt, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(optText, color = textColor, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Explanation: ${question.explanation}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.completeSessionAfterReview() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (viewModel.wasAlreadyCompleted) "Back to Daily Menu" else "Proceed to Streak Celebration",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SessionSummary(viewModel: DailySessionViewModel, onBack: () -> Unit) {
    val stats = viewModel.stats
    val streak = stats?.currentStreak ?: 0
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

    // Determine Milestone Badges based on Streak Days
    val (badgeName, badgeDesc, badgeColor, badgeIcon) = when {
        streak >= 200 -> Quad(
            "Cosmic Diamond Star",
            "Streak milestone of 200+ days achieved!",
            Color(0xFFE0F7FA),
            Icons.Default.EmojiEvents
        )
        streak >= 150 -> Quad(
            "Cosmic Platinum Shield",
            "Streak milestone of 150+ days achieved!",
            Color(0xFFECEFF1),
            Icons.Default.MilitaryTech
        )
        streak >= 100 -> Quad(
            "Golden Sun Master",
            "Streak milestone of 100+ days achieved!",
            Color(0xFFFFD54F),
            Icons.Default.EmojiEvents
        )
        streak >= 50 -> Quad(
            "Nebula Silver Expert",
            "Streak milestone of 50+ days achieved!",
            Color(0xFFCFD8DC),
            Icons.Default.MilitaryTech
        )
        streak >= 10 -> Quad(
            "Ember Bronze Champion",
            "Streak milestone of 10+ days achieved!",
            Color(0xFFFFB74D),
            Icons.Default.Whatshot
        )
        else -> Quad(
            "Starter Ember",
            "Keep studying daily to unlock milestone badges!",
            Color.White.copy(alpha = 0.6f),
            Icons.Default.Whatshot
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
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
            text = "$streak Day Streak!",
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

        // Badge display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                .border(1.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, badgeColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = badgeName,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = badgeDesc,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Score Card
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
                        val totalQ = viewModel.session?.content?.quizQuestions?.size ?: 1
                        Text("${(viewModel.correctCount.toFloat() / totalQ.toFloat() * 100).toInt()}%", color = PrimaryNeonTeal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

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
fun AlreadyCompletedState(viewModel: DailySessionViewModel) {
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
                    tint = SecondaryTangerine,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Completed Today!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You have successfully finished your Daily Workout for today. Come back tomorrow for new questions!",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.reviewModeActive = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Review Today's Quiz", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
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

// Custom structure helper
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
