package com.thinh.aistudybuddy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.model.Lesson
import com.thinh.aistudybuddy.data.model.ModuleStatus
import com.thinh.aistudybuddy.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonLearnScreen(
    lesson: Lesson,
    onBack: () -> Unit,
    onStartQuiz: () -> Unit,
    quizViewModel: QuizViewModel
) {
    val isLocked = lesson.status == ModuleStatus.LOCKED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learn", color = Color.White) },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = lesson.title,
                color = Color(0xFF00E5FF),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Color(0xFF2C2C2E), shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = lesson.difficulty,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(color = Color(0xFF2C2C2E), shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = "${lesson.estimatedMinutes} min",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(color = Color(0xFF2C2C2E), shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = when (lesson.status) {
                            ModuleStatus.COMPLETED -> "Completed"
                            ModuleStatus.IN_PROGRESS -> "In progress"
                            ModuleStatus.LOCKED -> "Locked"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = lesson.content.ifBlank { lesson.description },
                color = Color.LightGray,
                fontSize = 16.sp,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(40.dp))

            if (lesson.userScore != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Best Score: ${lesson.userScore}/10",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = {
                    quizViewModel.loadQuestions(
                        newQuestions = lesson.quizQuestions,
                        documentId = lesson.documentId.takeIf { docId -> docId.isNotBlank() },
                        lessonId = lesson.id,
                        title = lesson.title
                    )
                    onStartQuiz()
                },
                enabled = !isLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isLocked) "Locked" else "Practice Quiz", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}