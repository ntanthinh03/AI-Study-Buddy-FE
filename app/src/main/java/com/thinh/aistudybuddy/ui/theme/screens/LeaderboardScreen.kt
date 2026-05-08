package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.data.models.LeaderboardEntry
import com.thinh.aistudybuddy.viewmodel.LeaderboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit,
    viewModel: LeaderboardViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadLeaderboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Top Scholars", color = Color.White, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
        ) {
            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                }
            } else if (viewModel.entries.isNotEmpty()) {
                val top3 = viewModel.entries.take(3)
                val others = viewModel.entries.drop(3)
                
                if (top3.isNotEmpty()) {
                    PodiumView(top3)
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(others) { entry ->
                        LeaderboardItem(entry)
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No ranking data yet", color = Color.Gray)
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
            .height(200.dp)
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        if (top3.size > 1) PodiumColumn(top3[1], height = 120.dp, color = Color(0xFFB0BEC5), rank = 2)
        if (top3.isNotEmpty()) PodiumColumn(top3[0], height = 160.dp, color = Color(0xFFFFD700), rank = 1)
        if (top3.size > 2) PodiumColumn(top3[2], height = 100.dp, color = Color(0xFFCD7F32), rank = 3)
    }
}

@Composable
fun PodiumColumn(entry: LeaderboardEntry, height: androidx.compose.ui.unit.Dp, color: Color, rank: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(90.dp)
    ) {
        Text(entry.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("${entry.xp} XP", color = Color.LightGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(color.copy(alpha = 0.8f)),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                "$rank",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun LeaderboardItem(entry: LeaderboardEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${entry.rank}",
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.width(32.dp)
        )
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF2C2C2E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Lvl ${entry.level}", color = Color(0xFF00E5FF), fontSize = 12.sp)
        }
        
        Text("${entry.xp} XP", color = Color.White, fontWeight = FontWeight.Bold)
    }
}
