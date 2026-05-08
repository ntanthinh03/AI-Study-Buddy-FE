package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Analytics", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00E5FF))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Learning Overview",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsCard(
                            title = "Quizzes",
                            value = "${viewModel.stats?.totalQuizzes ?: 0}",
                            icon = Icons.Default.Quiz,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        StatsCard(
                            title = "Flashcards",
                            value = "${viewModel.stats?.totalFlashcards ?: 0}",
                            icon = Icons.Default.ViewCarousel,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    AccuracyCard(accuracy = viewModel.stats?.accuracy ?: 0)
                }

                item {
                    Text(
                        "Knowledge Progress (Weekly)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    WeeklyChart(data = viewModel.chartData)
                }

                item {
                    Text(
                        "Recent Activities",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (viewModel.stats?.recentActivities.isNullOrEmpty()) {
                    item {
                        Text("No recent activities found.", color = Color.Gray)
                    }
                } else {
                    items(viewModel.stats?.recentActivities ?: emptyList()) { activity ->
                        ActivityItem(activity = activity)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun AccuracyCard(accuracy: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Overall Accuracy", color = Color.Gray, fontSize = 14.sp)
                Text("$accuracy%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { accuracy / 100f },
                    modifier = Modifier.size(64.dp),
                    color = Color(0xFF00E5FF),
                    strokeWidth = 6.dp,
                    trackColor = Color(0xFF2C2C2E)
                )
                Icon(Icons.Default.Insights, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun WeeklyChart(data: List<com.thinh.aistudybuddy.data.models.ChartDataPoint>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Insufficient data", color = Color.Gray)
                }
            } else {
                val maxCount = (data.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
                data.forEach { point ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        val barHeight = (point.count.toFloat() / maxCount.toFloat()) * 120
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF00B8D4))
                                    )
                                )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = point.date.substring(5),
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: com.thinh.aistudybuddy.data.models.StudyActivity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (activity.type) {
                "QUIZ" -> Icons.Default.Quiz
                "FLASHCARD" -> Icons.Default.ViewCarousel
                else -> Icons.Default.Description
            }
            val tint = when (activity.type) {
                "QUIZ" -> Color(0xFF4CAF50)
                "FLASHCARD" -> Color(0xFF2196F3)
                else -> Color.Gray
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (activity.type == "QUIZ") "Quiz Practice" else "Flashcard Review",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${activity.correctAnswers}/${activity.totalQuestions} correct",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            Text(
                text = formatIsoDate(activity.createdAt),
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatIsoDate(isoString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = parser.parse(isoString)
        val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        isoString.take(10)
    }
}
