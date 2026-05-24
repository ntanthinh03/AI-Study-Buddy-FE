package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.viewmodel.AnalyticsViewModel
import com.thinh.aistudybuddy.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadDashboard()
            kotlinx.coroutines.delay(5000)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                viewModel.loadDashboard()
            }
    ) {
        // Space Ambient glows
        val infiniteTransition = rememberInfiniteTransition(label = "analytics_ambient")
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
                            Text("Diagnostics & Analytics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Neural Performance telemetry", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (viewModel.isLoading && viewModel.stats == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryNeonTeal)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Text(
                            "Neural Overview",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Typography.headlineMedium.fontFamily,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatsCard(
                                title = "Quizzes Finished",
                                value = "${viewModel.stats?.totalQuizzes ?: 0}",
                                icon = Icons.Default.Quiz,
                                color = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            StatsCard(
                                title = "Cards Analyzed",
                                value = "${viewModel.stats?.totalFlashcards ?: 0}",
                                icon = Icons.Default.ViewCarousel,
                                color = PrimaryNeonTeal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        val quizzesToday = viewModel.stats?.recentActivities?.filter { 
                            it.type == "QUIZ" && isToday(it.createdAt)
                        }?.size ?: 0
                        
                        val learningMode = viewModel.gamificationStats?.learningMode ?: "BALANCED"
                        val target = when (learningMode) {
                            "CASUAL" -> 1
                            "INTENSE" -> 5
                            else -> 3
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CosmicStudyRingsCard(
                                quizzesToday = quizzesToday,
                                target = target,
                                mode = learningMode
                            )
                            AccuracyCard(accuracy = viewModel.stats?.accuracy ?: 0)
                        }
                    }

                    item {
                        Text(
                            "Weekly Knowledge Progress",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        WeeklyChart(data = viewModel.chartData)
                    }

                    item {
                        Text(
                            "Recent Synapses",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (viewModel.stats?.recentActivities.isNullOrEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassCard(shape = RoundedCornerShape(12.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No active memory traces found.", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                            }
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
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape)
                    .border(0.5.dp, color.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(
                value, 
                color = Color.White, 
                fontSize = 26.sp, 
                fontWeight = FontWeight.Bold,
                fontFamily = Typography.headlineMedium.fontFamily
            )
            Text(
                title, 
                color = Color.White.copy(alpha = 0.5f), 
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AccuracyCard(accuracy: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
            .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.2f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Overall Retentive Accuracy", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$accuracy%", 
                    color = Color.White, 
                    fontSize = 34.sp, 
                    fontWeight = FontWeight.Bold,
                    fontFamily = Typography.headlineLarge.fontFamily
                )
            }
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { accuracy / 100f },
                    modifier = Modifier.size(68.dp),
                    color = PrimaryNeonTeal,
                    strokeWidth = 6.dp,
                    trackColor = Color.White.copy(alpha = 0.05f)
                )
                Icon(Icons.Default.Insights, null, tint = PrimaryNeonTeal, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun WeeklyChart(data: List<com.thinh.aistudybuddy.data.models.ChartDataPoint>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Insufficient telemetry data", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                }
            } else {
                val maxCount = (data.maxOfOrNull { point -> point.count } ?: 1).coerceAtLeast(1)
                data.forEach { point ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        val barHeight = (point.count.toFloat() / maxCount.toFloat()) * 110
                        Text(
                            text = "${point.count}",
                            color = if (point.count > 0) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.25f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(PrimaryNeonTeal, TertiaryCosmicIndigo)
                                    )
                                )
                                .border(0.5.dp, PrimaryNeonTeal.copy(alpha = 0.3f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = point.date.substring(5),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: com.thinh.aistudybuddy.data.models.StudyActivity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = SurfaceContainerHigh.copy(alpha = 0.3f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (activity.type) {
                "QUIZ" -> Icons.Default.Quiz
                "FLASHCARD" -> Icons.Default.ViewCarousel
                else -> Icons.Default.Description
            }
            val tint = when (activity.type) {
                "QUIZ" -> EmeraldSuccess
                "FLASHCARD" -> PrimaryNeonTeal
                else -> Color.White.copy(alpha = 0.4f)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.1f))
                    .border(0.5.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (activity.type == "QUIZ") "Quiz Practice Session" else "Flashcard Repetition",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${activity.correctAnswers}/${activity.totalQuestions} Synapses Retained",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = formatIsoDate(activity.createdAt),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatIsoDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Recent"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = parser.parse(isoString)
        val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        isoString.take(10)
    }
}

private fun isToday(dateStr: String?): Boolean {
    if (dateStr.isNullOrBlank()) return false
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = format.format(Date())
        dateStr.startsWith(todayStr)
    } catch (e: Exception) {
        false
    }
}

@Composable
fun CosmicStudyRingsCard(quizzesToday: Int, target: Int, mode: String) {
    val progress = if (target > 0) quizzesToday.toFloat() / target else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "ring_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
            .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.2f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "COSMIC STUDY RINGS",
                    color = PrimaryNeonTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$quizzesToday / $target Quizzes Done",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Mode: $mode (${(progress * 100).toInt()}% of daily target)",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.05f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx())
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(PrimaryNeonTeal, TertiaryCosmicIndigo, PrimaryNeonTeal)),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 8.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.WorkspacePremium, null, tint = PrimaryNeonTeal, modifier = Modifier.size(20.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
