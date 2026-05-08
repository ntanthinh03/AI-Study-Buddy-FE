package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.viewmodel.DailySessionViewModel

import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySessionScreen(
    onBack: () -> Unit,
    viewModel: DailySessionViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.loadDailySession()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Session", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212),
        content = { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (viewModel.loading && viewModel.session == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF00E5FF)
                    )
                } else if (viewModel.isFinished) {
                    SessionSummary(viewModel, onBack)
                } else if (viewModel.session == null) {
                    EmptySessionState(viewModel)
                } else {
                    val session = viewModel.session!!
                    val totalSteps = session.content.quizQuestions.size + session.content.flashcards.size
                    if (totalSteps == 0) {
                        EmptySessionState(viewModel)
                    } else {
                        ActiveSessionContent(viewModel)
                    }
                }
            }
        }
    )
}

@Composable
fun ActiveSessionContent(viewModel: DailySessionViewModel) {
    val session = viewModel.session!!
    val quizQuestions = session.content.quizQuestions
    val flashcards = session.content.flashcards
    val totalSteps = quizQuestions.size + flashcards.size
    val currentStep = viewModel.currentStep
    val safeTotalSteps = totalSteps.coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / safeTotalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF00E5FF),
            trackColor = Color(0xFF2C2C2E),
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (currentStep < quizQuestions.size) {
            val question = quizQuestions[currentStep]
            QuizStep(question) { isCorrect ->
                viewModel.submitAnswer(isCorrect)
            }
        } else {
            val flashcard = flashcards[currentStep - quizQuestions.size]
            FlashcardStep(flashcard) {
                viewModel.submitAnswer(true)
            }
        }
    }
}

@Composable
fun QuizStep(question: com.thinh.aistudybuddy.data.models.BackendQuizQuestion, onAnswer: (Boolean) -> Unit) {
    Column {
        Text(
            text = question.question,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        val options: List<Pair<String, String>> = listOf(
            "A" to (question.options["A"] ?: ""),
            "B" to (question.options["B"] ?: ""),
            "C" to (question.options["C"] ?: ""),
            "D" to (question.options["D"] ?: "")
        )

        options.forEach { item ->
            val key = item.first
            val text = item.second
            Button(
                onClick = { onAnswer(key == question.correctAnswer) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "$key. $text", color = Color.LightGray, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun FlashcardStep(flashcard: com.thinh.aistudybuddy.data.models.Flashcard, onDone: () -> Unit) {
    var revealed by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Flashcard Review", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(
                    text = if (revealed) flashcard.back else flashcard.front,
                    color = Color.White,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (!revealed) {
            Button(
                onClick = { revealed = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Show Answer", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Got it!", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SessionSummary(viewModel: DailySessionViewModel, onBack: () -> Unit) {
    val stats = viewModel.stats
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Whatshot,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(100.dp)
        )
        
        Text(
            text = "${stats?.currentStreak ?: 0} Day Streak!",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "You earned ${viewModel.correctCount * 10 + 20} XP today",
            color = Color.LightGray,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
        ) {
            Text("Back to Home", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptySessionState(viewModel: DailySessionViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "No questions ready for today!",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Upload a document or generate a batch to start your daily study routine.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

