package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.*
import com.thinh.aistudybuddy.viewmodel.StudyPlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanScreen(
    onBack: () -> Unit,
    onLearnClick: (Lesson) -> Unit,
    onMindMapClick: (String, String) -> Unit,
    studyViewModel: StudyPlanViewModel
) {
    val primaryCyan = Color(0xFF00E5FF)
    val successGreen = Color(0xFF4CAF50)
    val darkBg = Color(0xFF0F0F0F)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(studyViewModel.activePlan.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Personalized Roadmap", color = Color.Gray, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    val firstDocId = studyViewModel.activePlan.lessons.firstOrNull { it.documentId.isNotBlank() }?.documentId
                    if (firstDocId != null) {
                        IconButton(onClick = { onMindMapClick(firstDocId, studyViewModel.activePlan.title) }) {
                            Icon(Icons.Default.AccountTree, "Mind Map", tint = primaryCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        },
        containerColor = darkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (studyViewModel.loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = primaryCyan,
                    trackColor = Color(0xFF1E1E1E)
                )
            }

            studyViewModel.errorMessage?.let { message ->
                Surface(
                    color = Color(0xFFFF6B6B).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = message,
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header Progress
                item {
                    val completedCount = studyViewModel.activePlan.lessons.count { it.status == ModuleStatus.COMPLETED }
                    val totalCount = studyViewModel.activePlan.lessons.size
                    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                    Surface(
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF262626))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Overall Progress", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("$completedCount of $totalCount modules completed", color = Color.Gray, fontSize = 12.sp)
                                }
                                Text("${(progress * 100).toInt()}%", color = primaryCyan, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = primaryCyan,
                                trackColor = Color(0xFF2C2C2E)
                            )

                            Spacer(Modifier.height(24.dp))
                            
                            // Intensity Selector
                            Text("Learning Intensity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (intensity in StudyIntensity.values()) {
                                    val isSelected = studyViewModel.activePlan.intensity == intensity
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { studyViewModel.updateIntensity(intensity) },
                                        color = if (isSelected) primaryCyan.copy(alpha = 0.15f) else Color(0xFF2C2C2E),
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (isSelected) BorderStroke(1.dp, primaryCyan) else null
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(intensity.icon, fontSize = 20.sp)
                                            Text(
                                                intensity.label,
                                                color = if (isSelected) primaryCyan else Color.Gray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            // AI Motivational Message
                            Surface(
                                color = primaryCyan.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = primaryCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = when(studyViewModel.activePlan.intensity) {
                                            StudyIntensity.CHILL -> "Taking it slow? Smart choice for deep understanding. Buddy has adjusted your timeline to keep things stress-free while you stay on track!"
                                            StudyIntensity.SMART -> "The optimal path! You're balancing speed and mastery perfectly. Buddy will help you maintain this steady momentum."
                                            StudyIntensity.HARDCORE -> "Maximum power engaged! You're crushing modules at lightning speed. Buddy is ready with high-intensity material to fuel your expertise!"
                                            else -> "Ready to start your learning journey? Choose an intensity that fits your schedule!"
                                        },
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Timeline Items
                itemsIndexed(studyViewModel.activePlan.lessons) { index, lesson ->
                    val isLast = index == studyViewModel.activePlan.lessons.size - 1
                    val isLocked = lesson.status == ModuleStatus.LOCKED
                    val isCompleted = lesson.status == ModuleStatus.COMPLETED
                    val isInProgress = lesson.status == ModuleStatus.IN_PROGRESS

                    val statusColor = when {
                        isCompleted -> successGreen
                        isInProgress -> primaryCyan
                        else -> Color(0xFF333333)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Timeline Connector
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        if (isInProgress) primaryCyan.copy(alpha = 0.2f) else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isCompleted -> Icons.Default.CheckCircle
                                        isLocked -> Icons.Default.Lock
                                        else -> Icons.Default.RadioButtonChecked
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(if (isInProgress) 24.dp else 20.dp)
                                )
                            }
                            
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(100.dp)
                                        .background(
                                            if (isCompleted) successGreen else Color(0xFF262626)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Lesson Content Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isLocked) { onLearnClick(lesson) },
                            color = if (isInProgress) Color(0xFF1A1A1A) else Color.Transparent,
                            shape = RoundedCornerShape(20.dp),
                            border = if (isInProgress) BorderStroke(1.dp, primaryCyan.copy(alpha = 0.5f)) else null
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = lesson.title,
                                        color = if (isLocked) Color.Gray else Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (lesson.userScore != null) {
                                        Text(
                                            "${lesson.userScore}/10",
                                            color = successGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.background(successGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = lesson.description,
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isInProgress) {
                                    Button(
                                        onClick = { onLearnClick(lesson) },
                                        modifier = Modifier.padding(top = 12.dp).height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryCyan),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}