package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.viewmodel.QuizViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FireworksEffect() {
    val particles = remember {
        List(100) {
            Particle(
                angle = Random.nextFloat() * 2 * Math.PI.toFloat(),
                speed = Random.nextFloat() * 15f + 5f,
                color = Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "fireworks")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 3)
        particles.forEach { particle ->
            val x = center.x + cos(particle.angle) * particle.speed * progress * 500f
            val y = center.y + sin(particle.angle) * particle.speed * progress * 500f
            drawCircle(
                color = particle.color.copy(alpha = 1f - progress),
                radius = 8f * (1f - progress),
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
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
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
                Text(
                    text = "🎉 NEW RECORD! 🎉",
                    color = Color(0xFFFFD700),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.scale(scale).padding(bottom = 8.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = if (isPerfectScore || viewModel.isNewRecord) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isPerfectScore) "PERFECT SCORE!" else "Quiz Results",
                color = if (isPerfectScore) Color(0xFF00E5FF) else Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text("Score: ${viewModel.score} / $maxScore", color = Color.Gray, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(48.dp))

            Box(modifier = Modifier.width(300.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.wrapContentHeight()
                ) {
                    items(totalQuestions) { index ->
                        val status = viewModel.getQuestionStatus(index)
                        val boxColor = when (status) {
                            QuizViewModel.QuestionStatus.CORRECT -> Color(0xFF2E7D32)
                            QuizViewModel.QuestionStatus.INCORRECT -> Color(0xFFD32F2F)
                            QuizViewModel.QuestionStatus.UNANSWERED -> Color(0xFF2C2C2E)
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(boxColor, shape = RoundedCornerShape(12.dp))
                                .clickable { onReviewQuestion(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text((index + 1).toString(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Retake with New Quiz", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back to Home", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}