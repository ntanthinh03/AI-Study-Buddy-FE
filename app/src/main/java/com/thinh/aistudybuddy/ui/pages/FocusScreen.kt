package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.viewmodel.FocusViewModel
import com.thinh.aistudybuddy.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onBackClick: () -> Unit,
    viewModel: FocusViewModel = viewModel()
) {
    val totalTime = 25 * 60 * 1000L
    val progress = viewModel.timeRemainingMs.toFloat() / totalTime.toFloat()
    
    val minutes = (viewModel.timeRemainingMs / 1000) / 60
    val seconds = (viewModel.timeRemainingMs / 1000) % 60

    if (viewModel.showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text("Session Complete!", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Great focus! You've earned ${viewModel.earnedXp} XP.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Awesome", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceCardContainer,
            modifier = Modifier.border(1.dp, PrimaryNeonTeal.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "focus_ambient")
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
                            Text("Focus Chamber", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Cognitive Flow Isolation", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
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
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(310.dp)
                        .glassCard(shape = CircleShape, backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                        .cyberBorder(shape = CircleShape, borderWidth = 1.5.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.3f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.3f))
                        .padding(15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {

                        drawArc(
                            color = Color.White.copy(alpha = 0.05f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )

                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(PrimaryNeonTeal, TertiaryCosmicIndigo, PrimaryNeonTeal),
                                center = Offset(size.width / 2f, size.height / 2f)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                            color = Color.White,
                            fontSize = 62.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = Typography.headlineLarge.fontFamily
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (viewModel.isRunning && !viewModel.isPaused) "ISOLATION ACTIVE" else if (viewModel.isPaused) "FLOW PAUSED" else "SYSTEM READY",
                            color = if (viewModel.isRunning && !viewModel.isPaused) PrimaryNeonTeal else Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(56.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.isRunning || viewModel.isPaused) {
                        FilledIconButton(
                            onClick = { viewModel.stopTimer() },
                            modifier = Modifier
                                .size(64.dp)
                                .glassCard(shape = CircleShape, backgroundColor = SurfaceContainerHigh.copy(alpha = 0.8f))
                                .cyberBorder(shape = CircleShape, borderWidth = 1.dp, startColor = RoseWarning.copy(alpha = 0.4f), endColor = RoseWarning.copy(alpha = 0.1f)),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Transparent)
                        ) {
                            Icon(Icons.Default.Stop, null, tint = RoseWarning, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                    }

                    FilledIconButton(
                        onClick = {
                            if (viewModel.isRunning && !viewModel.isPaused) {
                                viewModel.pauseTimer()
                            } else {
                                viewModel.startTimer()
                            }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .glassCard(
                                shape = CircleShape, 
                                backgroundColor = if (viewModel.isRunning && !viewModel.isPaused) SecondaryTangerine.copy(alpha = 0.9f) else PrimaryNeonTeal.copy(alpha = 0.9f)
                            )
                            .cyberBorder(shape = CircleShape, borderWidth = 2.dp, startColor = Color.White.copy(alpha = 0.4f), endColor = Color.White.copy(alpha = 0.1f)),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Transparent)
                    ) {
                        Icon(
                            if (viewModel.isRunning && !viewModel.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}
