package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.QuizViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FireworksEffect() {
    val particles = remember {
        List(120) {
            Particle(
                angle = Random.nextFloat() * 2 * Math.PI.toFloat(),
                speed = Random.nextFloat() * 18f + 6f,
                color = Color(
                    red = Random.nextFloat() * 0.4f + 0.6f, // warm neon tones
                    green = Random.nextFloat() * 0.8f + 0.2f,
                    blue = Random.nextFloat() * 0.9f + 0.1f,
                    alpha = 1f
                )
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "fireworks")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 3.5f)
        particles.forEach { particle ->
            val x = center.x + cos(particle.angle) * particle.speed * progress * 480f
            val y = center.y + sin(particle.angle) * particle.speed * progress * 480f
            drawCircle(
                color = particle.color.copy(alpha = 1f - progress),
                radius = 10f * (1f - progress),
                center = Offset(x, y)
            )
        }
    }
}

private data class Particle(val angle: Float, val speed: Float, val color: Color)

@Composable
fun QuizResultScreen(
    viewModel: QuizViewModel,
    onReviewQuestion: (Int) -> Unit,
    onRetake: () -> Unit,
    onClose: () -> Unit
) {
    val totalQuestions = viewModel.questions.size
    val maxScore = totalQuestions * 10
    val isPerfectScore = viewModel.score == maxScore && totalQuestions > 0

    val infiniteTransition = rememberInfiniteTransition(label = "record")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {
        // Space backgrounds
        val glowTransition = rememberInfiniteTransition(label = "result_glow")
        val pulseScale by glowTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.3f),
                    radius = size.width * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.8f),
                    radius = size.width * 0.6f
                )
            )
        }

        if (isPerfectScore) {
            FireworksEffect()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (viewModel.isNewRecord && !isPerfectScore) {
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .padding(bottom = 16.dp)
                        .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = SecondaryTangerine.copy(alpha = 0.2f))
                        .cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.dp, startColor = SecondaryTangerine, endColor = SecondaryTangerine)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🎉 NEW RECORD! 🎉",
                        color = SecondaryTangerine,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .glassCard(shape = CircleShape, backgroundColor = SurfaceCardContainer.copy(alpha = 0.6f))
                    .then(
                        if (isPerfectScore || viewModel.isNewRecord) Modifier.cyberBorder(shape = CircleShape, borderWidth = 2.dp, startColor = SecondaryTangerine, endColor = SecondaryTangerine)
                        else Modifier.cyberBorder(shape = CircleShape, borderWidth = 1.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (isPerfectScore || viewModel.isNewRecord) SecondaryTangerine else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isPerfectScore) "PERFECT SCORE!" else "Quiz Results",
                color = if (isPerfectScore) PrimaryNeonTeal else Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Score: ${viewModel.score} / $maxScore Mastery Points",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Subtitle mapping info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = PrimaryNeonTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TAP ANY NODE TO REVIEW DETAILS",
                    color = PrimaryNeonTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.width(320.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.wrapContentHeight()
                ) {
                    items(totalQuestions) { index ->
                        val status = viewModel.getQuestionStatus(index)
                        
                        val (bgColor, borderColor) = when (status) {
                            QuizViewModel.QuestionStatus.CORRECT -> Pair(EmeraldSuccess.copy(alpha = 0.2f), EmeraldSuccess)
                            QuizViewModel.QuestionStatus.INCORRECT -> Pair(RoseWarning.copy(alpha = 0.2f), RoseWarning)
                            QuizViewModel.QuestionStatus.UNANSWERED -> Pair(SurfaceContainerHigh.copy(alpha = 0.5f), Color.Transparent)
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .then(
                                    if (borderColor != Color.Transparent) Modifier.cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.5.dp, startColor = borderColor, endColor = borderColor)
                                    else Modifier
                                )
                                .glassCard(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = bgColor,
                                    borderColor = Color(0x1Fffffff)
                                )
                                .clickable { onReviewQuestion(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onRetake,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "RETAKE WITH NEW QUESTIONS",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = Color.White.copy(alpha = 0.2f), endColor = Color.White.copy(alpha = 0.2f)),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "RETURN TO WORKSPACE",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}