package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.viewmodel.FocusViewModel
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
            title = { Text("Session Complete!", color = Color.White) },
            text = { Text("Great focus! You've earned ${viewModel.earnedXp} XP.", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("Awesome", color = Color(0xFF00E5FF))
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Timer", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
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
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color(0xFF2C2C2E),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFF00E5FF),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (viewModel.isRunning && !viewModel.isPaused) "Focusing..." else if (viewModel.isPaused) "Paused" else "Ready to focus",
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewModel.isRunning || viewModel.isPaused) {
                    FilledIconButton(
                        onClick = { viewModel.stopTimer() },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2C2C2E))
                    ) {
                        Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(32.dp))
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
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (viewModel.isRunning && !viewModel.isPaused) Color(0xFFFF9800) else Color(0xFF00E5FF)
                    )
                ) {
                    Icon(
                        if (viewModel.isRunning && !viewModel.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}
