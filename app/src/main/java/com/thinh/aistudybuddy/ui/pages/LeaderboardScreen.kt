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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.thinh.aistudybuddy.data.models.LeaderboardEntry
import com.thinh.aistudybuddy.viewmodel.LeaderboardViewModel
import com.thinh.aistudybuddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit,
    viewModel: LeaderboardViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadLeaderboard()
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "leaderboard_ambient")
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
                            Text("Hall of Scholars", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Top AI Brain Sync Rankings", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
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
                    .padding(horizontal = 16.dp)
            ) {
                if (viewModel.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryNeonTeal)
                    }
                } else if (viewModel.entries.isNotEmpty()) {
                    val top3 = viewModel.entries.take(3)
                    val others = viewModel.entries.drop(3)
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (top3.isNotEmpty()) {
                            item {
                                PodiumView(top3)
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        items(others) { entry ->
                            LeaderboardItem(entry)
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No ranking data yet", color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumView(top3: List<LeaderboardEntry>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (top3.size > 1) {
            Box(modifier = Modifier.weight(1f)) {
                PodiumColumn(top3[1], height = 120.dp, color = PrimaryNeonTeal, rank = 2)
            }
        }
        if (top3.isNotEmpty()) {
            Box(modifier = Modifier.weight(1.1f)) {
                PodiumColumn(top3[0], height = 165.dp, color = SecondaryTangerine, rank = 1)
            }
        }
        if (top3.size > 2) {
            Box(modifier = Modifier.weight(1f)) {
                PodiumColumn(top3[2], height = 95.dp, color = TertiaryCosmicIndigo, rank = 3)
            }
        }
    }
}

@Composable
fun PodiumColumn(entry: LeaderboardEntry, height: androidx.compose.ui.unit.Dp, color: Color, rank: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            entry.name, 
            color = Color.White, 
            fontWeight = FontWeight.Bold, 
            maxLines = 1,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${entry.xp} XP", 
            color = color, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .glassCard(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f)
                )
                .cyberBorder(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    borderWidth = 1.5.dp,
                    startColor = color.copy(alpha = 0.8f),
                    endColor = color.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.15f), CircleShape)
                        .cyberBorder(shape = CircleShape, borderWidth = 1.dp, startColor = color, endColor = color.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$rank",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun LeaderboardItem(entry: LeaderboardEntry) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(14.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "${entry.rank}",
                    color = if (entry.rank <= 5) PrimaryNeonTeal else Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = Typography.headlineMedium.fontFamily
                )
            }
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SurfaceContainerHigh.copy(alpha = 0.6f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    entry.name.take(1).uppercase(), 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.name, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(PrimaryNeonTeal.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, PrimaryNeonTeal.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "LVL ${entry.level}", 
                            color = PrimaryNeonTeal, 
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Text(
                "${entry.xp} XP", 
                color = Color.White, 
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
