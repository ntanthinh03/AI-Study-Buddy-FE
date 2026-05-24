package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.R
import com.thinh.aistudybuddy.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import com.thinh.aistudybuddy.services.network.RetrofitClient


import androidx.compose.ui.graphics.Shadow

@Composable
fun WelcomeScreen(
    onFinished: () -> Unit
) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    
    var learningMode by remember { mutableStateOf("BALANCED") }

    LaunchedEffect(Unit) {
        // Fetch learning mode dynamically to show matching mascot emotion
        runCatching {
            val resp = RetrofitClient.instance.getUserStats()
            if (resp.isSuccessful) {
                learningMode = resp.body()?.learningMode ?: "BALANCED"
            }
        }
        delay(3000)
        onFinished()
    }

    // Elegant backdrop floating glow animations
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_ambient")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -40f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground),
        contentAlignment = Alignment.Center
    ) {
        // Floating cyber orbs layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Neon teal energy center (Top-Left quadrant)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.15f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.35f + floatOffset),
                    radius = size.width * 0.7f
                )
            )
            // Cosmic purple mystery center (Bottom-Right quadrant)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.18f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.75f, size.height * 0.65f - floatOffset),
                    radius = size.width * 0.8f
                )
            )
        }

        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(1200, easing = EaseOutQuad)) +
                    slideInVertically(
                        animationSpec = tween(1200, easing = EaseOutBack),
                        initialOffsetY = { 150 }
                    )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(36.dp)
                    .fillMaxWidth()
            ) {
                // Interactive AI Mascot
                InteractiveMascot(
                    mode = learningMode,
                    modifier = Modifier.padding(bottom = 36.dp)
                )

                // Neon title with premium linear gradient and high-end glow shadow
                Text(
                    text = "BUDDY APP",
                    style = MaterialTheme.typography.displayMedium.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryNeonTeal, Color(0xFF00E676), Color(0xFF00B0FF))
                        ),
                        shadow = Shadow(
                            color = PrimaryNeonTeal.copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 35f
                        )
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Sleek subtitle with premium silver-teal color and wide tracking
                Text(
                    text = "AI Study",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Tagline with elegant gold glow, comfortable vertical padding
                Text(
                    text = "IGNITE YOUR FOCUS • CONQUER YOUR EXAMS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        shadow = Shadow(
                            color = SecondaryTangerine.copy(alpha = 0.3f),
                            offset = Offset(0f, 0f),
                            blurRadius = 12f
                        )
                    ),
                    color = SecondaryTangerine,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
fun InteractiveMascot(modifier: Modifier = Modifier, mode: String = "BALANCED") {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_anims")
    
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = when (mode) {
            "CASUAL" -> 5f
            "INTENSE" -> -8f
            else -> -15f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mode) {
                    "CASUAL" -> 2000
                    "INTENSE" -> 600
                    else -> 1000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_bounce"
    )

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    Box(
        modifier = modifier
            .size(160.dp)
            .offset(y = bounceY.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val headWidth = size.width * 0.6f
            val headHeight = size.height * 0.5f

            val auraColor = when (mode) {
                "INTENSE" -> Color(0xFF00E676)
                "CASUAL" -> Color(0xFF00B0FF)
                else -> Color(0xFFFFD600)
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor.copy(alpha = 0.3f * auraPulse), Color.Transparent),
                    center = center,
                    radius = size.width * 0.45f
                )
            )

            if (mode == "INTENSE") {
                for (i in 0..5) {
                    val angle = -45f - (i * 18f)
                    val len = size.width * 0.4f * auraPulse
                    val x = center.x + Math.cos(Math.toRadians(angle.toDouble())).toFloat() * len
                    val y = center.y + Math.sin(Math.toRadians(angle.toDouble())).toFloat() * len
                    drawLine(
                        color = Color(0xFF00E676).copy(alpha = 0.4f),
                        start = center,
                        end = Offset(x, y),
                        strokeWidth = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            drawRoundRect(
                color = Color(0xFF1E1E2C),
                topLeft = Offset(center.x - headWidth / 2f, center.y - headHeight / 2f),
                size = androidx.compose.ui.geometry.Size(headWidth, headHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )

            drawRoundRect(
                color = Color.White.copy(alpha = 0.15f),
                topLeft = Offset(center.x - headWidth / 2f, center.y - headHeight / 2f),
                size = androidx.compose.ui.geometry.Size(headWidth, headHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            val screenWidth = headWidth * 0.85f
            val screenHeight = headHeight * 0.75f
            drawRoundRect(
                color = Color(0xFF0C0C14),
                topLeft = Offset(center.x - screenWidth / 2f, center.y - screenHeight / 2f),
                size = androidx.compose.ui.geometry.Size(screenWidth, screenHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
            )

            drawRoundRect(
                color = auraColor.copy(alpha = 0.5f),
                topLeft = Offset(center.x - screenWidth / 2f, center.y - screenHeight / 2f),
                size = androidx.compose.ui.geometry.Size(screenWidth, screenHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )

            val antY = center.y - headHeight / 2f
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(center.x, antY),
                end = Offset(center.x, antY - 20.dp.toPx()),
                strokeWidth = 3.dp.toPx()
            )
            drawCircle(
                color = auraColor,
                center = Offset(center.x, antY - 20.dp.toPx()),
                radius = 6.dp.toPx()
            )

            val eyeOffsetY = 4.dp.toPx()
            val leftEyeX = center.x - screenWidth * 0.25f
            val rightEyeX = center.x + screenWidth * 0.25f
            val eyeY = center.y - eyeOffsetY
            val eyeColor = auraColor

            when (mode) {
                "INTENSE" -> {
                    drawLine(
                        color = eyeColor,
                        start = Offset(leftEyeX - 10.dp.toPx(), eyeY - 6.dp.toPx()),
                        end = Offset(leftEyeX + 10.dp.toPx(), eyeY + 6.dp.toPx()),
                        strokeWidth = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = eyeColor,
                        start = Offset(rightEyeX + 10.dp.toPx(), eyeY - 6.dp.toPx()),
                        end = Offset(rightEyeX - 10.dp.toPx(), eyeY + 6.dp.toPx()),
                        strokeWidth = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
                "CASUAL" -> {
                    drawLine(
                        color = eyeColor.copy(alpha = 0.8f),
                        start = Offset(leftEyeX - 10.dp.toPx(), eyeY),
                        end = Offset(leftEyeX + 10.dp.toPx(), eyeY),
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = eyeColor.copy(alpha = 0.8f),
                        start = Offset(rightEyeX - 10.dp.toPx(), eyeY),
                        end = Offset(rightEyeX + 10.dp.toPx(), eyeY),
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
                 else -> {
                    val leftPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(leftEyeX - 10.dp.toPx(), eyeY + 4.dp.toPx())
                        quadraticTo(leftEyeX, eyeY - 8.dp.toPx(), leftEyeX + 10.dp.toPx(), eyeY + 4.dp.toPx())
                    }
                    drawPath(
                        path = leftPath,
                        color = eyeColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )

                    val rightPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(rightEyeX - 10.dp.toPx(), eyeY + 4.dp.toPx())
                        quadraticTo(rightEyeX, eyeY - 8.dp.toPx(), rightEyeX + 10.dp.toPx(), eyeY + 4.dp.toPx())
                    }
                    drawPath(
                        path = rightPath,
                        color = eyeColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}
