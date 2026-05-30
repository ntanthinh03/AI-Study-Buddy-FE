package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.*
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.StudyPlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanScreen(
    onBack: () -> Unit,
    onLearnClick: (Lesson) -> Unit,
    onMindMapClick: (String, String) -> Unit,
    studyViewModel: StudyPlanViewModel
) {
    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "roadmap_ambient")
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

        LaunchedEffect(Unit) {
            studyViewModel.ensureAllLessonsEnriched()
        }

        val isReady = studyViewModel.isAllLessonsReady

        if (!isReady) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Column {
                                Text(studyViewModel.activePlan.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Personalized Roadmap", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .glassCard(shape = RoundedCornerShape(24.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.6f))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val infiniteRotation = rememberInfiniteTransition("spinner_rotation")
                        val rotationAngle by infiniteRotation.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "spin"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .cyberBorder(shape = CircleShape, borderWidth = 2.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryNeonTeal,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "PREPARING STUDY PLAN",
                            color = PrimaryNeonTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Buddy is crafting customized theory & high-fidelity quizzes for your roadmap. Please wait...",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val readyCount = studyViewModel.readyLessonsCount
                        val totalCount = studyViewModel.totalLessonsCount
                        val progressFraction = if (totalCount > 0) readyCount.toFloat() / totalCount else 0f
                        
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryNeonTeal,
                            trackColor = SurfaceContainerLowest
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Prepared: $readyCount of $totalCount modules (${(progressFraction * 100).toInt()}%)",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            studyViewModel.activePlan.lessons.forEach { lesson ->
                                val prepared = studyViewModel.isLessonReady(lesson.id)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (prepared) Icons.Default.CheckCircle else Icons.Default.Pending,
                                        contentDescription = null,
                                        tint = if (prepared) EmeraldSuccess else PrimaryNeonTeal.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lesson.title,
                                        color = if (prepared) Color.White else Color.White.copy(alpha = 0.4f),
                                        fontSize = 13.sp,
                                        fontWeight = if (prepared) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Column {
                                Text(studyViewModel.activePlan.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Personalized Roadmap", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
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
                                    Icon(Icons.Default.AccountTree, "Mind Map", tint = PrimaryNeonTeal)
                                }
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
                ) {
                    if (studyViewModel.loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = PrimaryNeonTeal,
                            trackColor = SurfaceContainerLowest
                        )
                    }

                    studyViewModel.errorMessage?.let { message ->
                        Surface(
                            color = RoseWarning.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = message,
                                color = RoseWarning,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        
                        item {
                            val completedCount = studyViewModel.activePlan.lessons.count { it.status == ModuleStatus.COMPLETED }
                            val totalCount = studyViewModel.activePlan.lessons.size
                            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                                    .glassCard(shape = RoundedCornerShape(24.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.6f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Overall Progress", color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("$completedCount of $totalCount modules completed", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        }
                                        Text("${(progress * 100).toInt()}%", color = PrimaryNeonTeal, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = PrimaryNeonTeal,
                                        trackColor = SurfaceContainerLowest
                                    )

                                    Spacer(Modifier.height(24.dp))
                                    
                                    Text("Learning Intensity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (intensity in StudyIntensity.values()) {
                                            val isSelected = studyViewModel.activePlan.intensity == intensity
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .then(
                                                        if (isSelected) Modifier.cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.dp)
                                                        else Modifier
                                                    )
                                                    .glassCard(
                                                        shape = RoundedCornerShape(12.dp),
                                                        backgroundColor = if (isSelected) PrimaryNeonTeal.copy(alpha = 0.15f) else SurfaceContainerHigh.copy(alpha = 0.4f),
                                                        borderColor = Color.Transparent
                                                    )
                                                    .clickable { studyViewModel.updateIntensity(intensity) }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(intensity.icon, fontSize = 20.sp)
                                                    Text(
                                                        intensity.label,
                                                        color = if (isSelected) PrimaryNeonTeal else Color.White.copy(alpha = 0.4f),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = PrimaryNeonTeal.copy(alpha = 0.05f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, null, tint = PrimaryNeonTeal, modifier = Modifier.size(16.dp))
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

                        itemsIndexed(studyViewModel.activePlan.lessons) { index, lesson ->
                            val isLast = index == studyViewModel.activePlan.lessons.size - 1
                            val isLocked = lesson.status == ModuleStatus.LOCKED
                            val isCompleted = lesson.status == ModuleStatus.COMPLETED
                            val isInProgress = lesson.status == ModuleStatus.IN_PROGRESS

                            val statusColor = when {
                                isCompleted -> EmeraldSuccess
                                isInProgress -> PrimaryNeonTeal
                                else -> Color.White.copy(alpha = 0.2f)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                if (isInProgress) PrimaryNeonTeal.copy(alpha = 0.2f) else Color.Transparent,
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
                                                    if (isCompleted) EmeraldSuccess else Color.White.copy(alpha = 0.08f)
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (isInProgress) Modifier.cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.dp)
                                            else Modifier
                                        )
                                        .glassCard(
                                            shape = RoundedCornerShape(20.dp),
                                            backgroundColor = if (isInProgress) SurfaceCardContainer.copy(alpha = 0.8f) else Color.Transparent,
                                            borderColor = Color.Transparent
                                        )
                                        .clickable(enabled = !isLocked) { onLearnClick(lesson) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = lesson.title,
                                                color = if (isLocked) Color.White.copy(alpha = 0.4f) else Color.White,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (lesson.userScore != null) {
                                                Text(
                                                    "${lesson.userScore}/10",
                                                    color = EmeraldSuccess,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .background(EmeraldSuccess.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        
                                        Text(
                                            text = lesson.description,
                                            color = Color.White.copy(alpha = 0.6f),
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
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
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
    }
}