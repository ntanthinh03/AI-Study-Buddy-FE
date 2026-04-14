package com.thinh.aistudybuddy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.model.Lesson
import com.thinh.aistudybuddy.data.model.ModuleStatus
import com.thinh.aistudybuddy.viewmodel.StudyPlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanScreen(
    onBack: () -> Unit,
    onLearnClick: (Lesson) -> Unit,
    studyViewModel: StudyPlanViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(studyViewModel.activePlan.title, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        ) {
            if (studyViewModel.loading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF2C2C2E)
                )
            }

            studyViewModel.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp)
            ) {
                item {
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = studyViewModel.activePlan.overview.ifBlank { "Structured study roadmap loaded from backend JSON." },
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(color = Color(0xFF00E5FF).copy(alpha = 0.15f), shape = RoundedCornerShape(999.dp)) {
                                    Text(
                                        text = "${studyViewModel.activePlan.lessons.size} modules",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(color = Color(0xFF4CAF50).copy(alpha = 0.15f), shape = RoundedCornerShape(999.dp)) {
                                    Text(
                                        text = "${studyViewModel.activePlan.estimatedTotalMinutes} min",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                items(studyViewModel.activePlan.lessons, key = { it.id }) { lesson ->
                    var isExpanded by remember { mutableStateOf(false) }
                    val isLocked = lesson.status == ModuleStatus.LOCKED
                    val statusColor = when (lesson.status) {
                        ModuleStatus.COMPLETED -> Color(0xFF4CAF50)
                        ModuleStatus.IN_PROGRESS -> Color(0xFF00E5FF)
                        ModuleStatus.LOCKED -> Color.Gray
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = when {
                                        lesson.isCompleted || lesson.status == ModuleStatus.COMPLETED -> Icons.Default.CheckCircle
                                        isLocked -> Icons.Default.Lock
                                        else -> Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = lesson.title,
                                    color = if (lesson.isCompleted) Color.LightGray else Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                if (lesson.userScore != null) {
                                    Surface(
                                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${lesson.userScore}/10",
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(999.dp)) {
                                Text(
                                    text = when (lesson.status) {
                                        ModuleStatus.COMPLETED -> "Completed"
                                        ModuleStatus.IN_PROGRESS -> "In progress"
                                        ModuleStatus.LOCKED -> "Locked"
                                    },
                                    color = statusColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            Text(
                                text = lesson.description,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 10.dp)
                            )

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    Button(
                                        onClick = { onLearnClick(lesson) },
                                        enabled = !isLocked,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            if (isLocked) "Locked" else "Learn",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}