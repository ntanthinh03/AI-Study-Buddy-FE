package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.*
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonLearnScreen(
    lesson: Lesson,
    onBack: () -> Unit,
    onStartQuiz: () -> Unit,
    quizViewModel: QuizViewModel
) {
    val isLocked = lesson.status == ModuleStatus.LOCKED

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {
        // Pulsing space background glows
        val infiniteTransition = rememberInfiniteTransition(label = "lesson_ambient")
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
            initialValue = -40f,
            targetValue = 40f,
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
                    center = Offset(size.width * 0.8f, size.height * 0.2f + floatOffset),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.1f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.8f - floatOffset),
                    radius = size.width * 0.65f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Socratic Lesson", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Interactive Study Mode", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
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
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Lesson Header Card (Glassmorphic)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(shape = RoundedCornerShape(24.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                        .cyberBorder(shape = RoundedCornerShape(24.dp), borderWidth = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryNeonTeal.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MenuBook, null, tint = PrimaryNeonTeal, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = lesson.title,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 26.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Difficulty Badge
                            Box(
                                modifier = Modifier
                                    .glassCard(
                                        shape = RoundedCornerShape(8.dp),
                                        backgroundColor = SurfaceContainerHigh.copy(alpha = 0.6f),
                                        borderColor = Color(0x1Fffffff)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = lesson.difficulty.uppercase(),
                                    color = PrimaryNeonTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Time Badge
                            Box(
                                modifier = Modifier
                                    .glassCard(
                                        shape = RoundedCornerShape(8.dp),
                                        backgroundColor = SurfaceContainerHigh.copy(alpha = 0.6f),
                                        borderColor = Color(0x1Fffffff)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${lesson.estimatedMinutes} MINS",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Status Badge
                            Box(
                                modifier = Modifier
                                    .glassCard(
                                        shape = RoundedCornerShape(8.dp),
                                        backgroundColor = when (lesson.status) {
                                            ModuleStatus.COMPLETED -> EmeraldSuccess.copy(alpha = 0.15f)
                                            ModuleStatus.IN_PROGRESS -> PrimaryNeonTeal.copy(alpha = 0.15f)
                                            else -> SurfaceContainerHigh.copy(alpha = 0.6f)
                                        },
                                        borderColor = Color.Transparent
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (lesson.status) {
                                        ModuleStatus.COMPLETED -> "COMPLETED"
                                        ModuleStatus.IN_PROGRESS -> "IN PROGRESS"
                                        ModuleStatus.LOCKED -> "LOCKED"
                                    },
                                    color = when (lesson.status) {
                                        ModuleStatus.COMPLETED -> EmeraldSuccess
                                        ModuleStatus.IN_PROGRESS -> PrimaryNeonTeal
                                        else -> Color.White.copy(alpha = 0.4f)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Content Panel (Glassmorphic)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = PrimaryNeonTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SOCRATIC DISCOVERY TEXT",
                                color = PrimaryNeonTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = lesson.content.ifBlank { lesson.description },
                            color = Color.LightGray.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (lesson.userScore != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SecondaryTangerine.copy(alpha = 0.08f))
                            .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = SecondaryTangerine, endColor = SecondaryTangerine.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = SecondaryTangerine,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Personal Best Score: ${lesson.userScore}/10 Mastery Points",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        quizViewModel.loadQuestions(
                            newQuestions = lesson.quizQuestions,
                            documentId = lesson.documentId.takeIf { docId -> docId.isNotBlank() },
                            lessonId = null,
                            title = lesson.title
                        )
                        onStartQuiz()
                    },
                    enabled = !isLocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .then(
                            if (!isLocked) Modifier.cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.5.dp)
                            else Modifier
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLocked) SurfaceContainerHigh.copy(alpha = 0.4f) else PrimaryNeonTeal,
                        disabledContainerColor = SurfaceContainerHigh.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isLocked) "MODULE LOCKED" else "PRACTICE COMPREHENSION QUIZ",
                        color = if (isLocked) Color.White.copy(alpha = 0.3f) else Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}