@file:Suppress("unused", "UNUSED_VALUE")

package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onCloseClick: () -> Unit,
    viewModel: QuizViewModel = viewModel()
) {
    var showResultScreen by remember { mutableStateOf(false) }
    var isReviewMode by remember { mutableStateOf(false) }
    var showUnansweredDialog by remember { mutableStateOf(false) }
    var unansweredText by remember { mutableStateOf("") }

    if (viewModel.isGeneratingQuiz) {
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
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

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Forging quiz questions...",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Analyzing key concepts and reading PDF document under the hood.",
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 20.sp
                )
            }
        }
        return
    }

    val genError = viewModel.quizGenerationError
    if (genError != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RoseWarning,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Quiz Generation Failed",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = genError,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val docId = viewModel.currentDocumentId
                        if (!docId.isNullOrBlank()) {
                            viewModel.generateQuizForDocument(docId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(
                        text = "RETRY",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onCloseClick) {
                    Text(
                        text = "CLOSE",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }

    LaunchedEffect(showResultScreen) {
        if (showResultScreen && !isReviewMode) {
            viewModel.onQuizComplete?.invoke(viewModel.score)
            viewModel.submitQuizResultToBackend()
        }
    }

    if (showResultScreen && !isReviewMode) {
        QuizResultScreen(
            viewModel = viewModel,
            onReviewQuestion = { index: Int ->
                viewModel.jumpToQuestion(index)
                isReviewMode = true
            },
            onRetake = {
                viewModel.resetQuiz()
                showResultScreen = false
                isReviewMode = false
            },
            onClose = onCloseClick
        )
        return
    }

    if (showUnansweredDialog) {
        AlertDialog(
            onDismissRequest = { showUnansweredDialog = false },
            title = { Text("Finish Quiz?", fontWeight = FontWeight.Bold) },
            text = { Text("There are unanswered quiz questions: $unansweredText. Do you want to submit anyway?") },
            confirmButton = {
                TextButton(onClick = {
                    showUnansweredDialog = false
                    viewModel.calculateFinalScore()
                    showResultScreen = true
                }) { Text("SUBMIT", color = PrimaryNeonTeal, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showUnansweredDialog = false }) { Text("CANCEL", color = Color.Gray) }
            },
            containerColor = SurfaceCardContainer,
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    val currentIdx = viewModel.currentQuestionIndex
    val isCorrectShown = isReviewMode
    val hasSelected = viewModel.userAnswers[currentIdx] != -1

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "quiz_ambient")
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
                    radius = size.width * 0.55f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.75f - floatOffset),
                    radius = size.width * 0.55f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = if (isReviewMode) { { isReviewMode = false } } else onCloseClick,
                    modifier = Modifier.background(SurfaceContainerHigh.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "QUESTION ${currentIdx + 1}",
                        color = PrimaryNeonTeal,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    if (viewModel.isLoadingMoreQuestions) {
                        Text("Synthesizing more questions...", color = PrimaryNeonTeal.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                    CircularProgressIndicator(
                        progress = { if (viewModel.questions.isNotEmpty()) (currentIdx + 1).toFloat() / viewModel.questions.size else 0f },
                        color = PrimaryNeonTeal,
                        trackColor = SurfaceContainerLowest,
                        strokeWidth = 4.dp
                    )
                    Icon(Icons.Default.Schedule, null, tint = PrimaryNeonTeal.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            if (viewModel.questions.isNotEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(shape = RoundedCornerShape(24.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                        .cyberBorder(shape = RoundedCornerShape(24.dp), borderWidth = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = PrimaryNeonTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI COMPREHENSION QUERY",
                                color = PrimaryNeonTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = viewModel.currentQuestion.question,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    itemsIndexed(viewModel.currentQuestion.options) { index, option ->
                        val key = when(index) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" }
                        val isSelected = viewModel.userAnswers[currentIdx] == index
                        val isCorrect = index == viewModel.currentQuestion.correctAnswerIndex

                        val (bgColor, borderColor) = when {
                            isCorrectShown && isCorrect -> Pair(EmeraldSuccess.copy(alpha = 0.12f), EmeraldSuccess)
                            isCorrectShown && isSelected && !isCorrect -> Pair(RoseWarning.copy(alpha = 0.12f), RoseWarning)
                            isSelected -> Pair(PrimaryNeonTeal.copy(alpha = 0.15f), PrimaryNeonTeal)
                            else -> Pair(SurfaceCardContainer.copy(alpha = 0.5f), Color.Transparent)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (borderColor != Color.Transparent) Modifier.cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.5.dp, startColor = borderColor, endColor = borderColor)
                                    else Modifier
                                )
                                .glassCard(
                                    shape = RoundedCornerShape(20.dp),
                                    backgroundColor = bgColor,
                                    borderColor = Color(0x1Fffffff)
                                )
                                .clickable(enabled = !isCorrectShown) { viewModel.selectOption(index) }
                        ) {
                            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isCorrectShown && isCorrect -> EmeraldSuccess
                                                isCorrectShown && isSelected && !isCorrect -> RoseWarning
                                                isSelected -> PrimaryNeonTeal
                                                else -> SurfaceContainerHigh
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        key,
                                        color = if (isSelected || (isCorrectShown && isCorrect)) Color.Black else Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(option, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))

                                if (isCorrectShown) {
                                    if (isCorrect) Icon(Icons.Default.CheckCircle, null, tint = EmeraldSuccess)
                                    else if (isSelected) Icon(Icons.Default.Close, null, tint = RoseWarning)
                                }
                            }
                        }
                    }

                    if (isCorrectShown) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = EmeraldSuccess.copy(alpha = 0.08f))
                                    .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = EmeraldSuccess, endColor = EmeraldSuccess.copy(alpha = 0.3f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Info, null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Explanation: ${viewModel.currentQuestion.explanation}",
                                        color = Color.LightGray.copy(alpha = 0.9f),
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))


            if (isReviewMode) {
                Button(
                    onClick = { isReviewMode = false },
                    modifier = Modifier.fillMaxWidth().height(56.dp).cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.5.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("BACK TO RESULTS", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentIdx > 0) {
                        OutlinedButton(
                            onClick = { viewModel.previousQuestion() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("PREVIOUS", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    val isLast = currentIdx == viewModel.questions.size - 1
                    Button(
                        onClick = {
                            if (isLast) {
                                val unanswered = viewModel.getUnansweredQuestions()
                                if (unanswered.isNotEmpty()) {
                                    unansweredText = unanswered.joinToString(", ") { "#$it" }
                                    showUnansweredDialog = true
                                } else {
                                    viewModel.calculateFinalScore()
                                    showResultScreen = true
                                }
                            } else {
                                viewModel.nextQuestion()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .then(
                                if (isLast) Modifier.cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.5.dp)
                                else Modifier
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLast) PrimaryNeonTeal else SurfaceContainerHigh.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        val text = if (isLast) "FINISH" else "NEXT"
                        Text(
                            text = text,
                            color = if (isLast) Color.Black else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}