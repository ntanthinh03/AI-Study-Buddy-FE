@file:Suppress("unused", "UNUSED_VALUE")

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onCloseClick: () -> Unit,
    viewModel: QuizViewModel = viewModel()
) {
    var showResultScreen by remember { mutableStateOf(false) }
    var isReviewMode by remember { mutableStateOf(false) }
    var showUnansweredDialog by remember { mutableStateOf(false) }
    var unansweredText by remember { mutableStateOf("") }
    
    val primaryCyan = Color(0xFF00E5FF)
    val darkBg = Color(0xFF0F0F0F)

    LaunchedEffect(showResultScreen) {
        if (showResultScreen && !isReviewMode) {
            viewModel.onQuizComplete?.invoke(viewModel.score)
            viewModel.submitQuizResultToBackend()
        }
    }

    if (showResultScreen && !isReviewMode) {
        QuizResultScreen(
            viewModel = viewModel,
            onReviewQuestion = { index: Int ->
                viewModel.jumpToQuestion(index)
                isReviewMode = true
            },
            onRetake = {
                viewModel.resetQuiz()
                showResultScreen = false
                isReviewMode = false
            },
            onClose = onCloseClick
        )
        return
    }

    if (showUnansweredDialog) {
        AlertDialog(
            onDismissRequest = { showUnansweredDialog = false },
            title = { Text("Finish Quiz?", fontWeight = FontWeight.Bold) },
            text = { Text("There are unanswered quiz questions: $unansweredText. Do you want to submit anyway?") },
            confirmButton = {
                TextButton(onClick = {
                    showUnansweredDialog = false
                    showResultScreen = true
                }) { Text("SUBMIT", color = primaryCyan, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showUnansweredDialog = false }) { Text("CANCEL", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    val currentIdx = viewModel.currentQuestionIndex
    val isSubmitted = viewModel.submittedQuestions[currentIdx] || isReviewMode
    val hasSelected = viewModel.userAnswers[currentIdx] != -1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .padding(24.dp)
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = if (isReviewMode) { { isReviewMode = false } } else onCloseClick) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("QUESTION ${currentIdx + 1}", color = primaryCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("${viewModel.questions.size} Questions Total", color = Color.Gray, fontSize = 11.sp)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                CircularProgressIndicator(
                    progress = { if (viewModel.questions.isNotEmpty()) (currentIdx + 1).toFloat() / viewModel.questions.size else 0f },
                    color = primaryCyan,
                    trackColor = Color(0xFF1E1E1E),
                    strokeWidth = 4.dp
                )
                Icon(Icons.Default.Schedule, null, tint = primaryCyan.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        if (viewModel.questions.isNotEmpty()) {
            // Question Card
            Surface(
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF262626))
            ) {
                Text(
                    text = viewModel.currentQuestion.question,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(24.dp),
                    lineHeight = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Options List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                itemsIndexed(viewModel.currentQuestion.options) { index, option ->
                    val key = when(index) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" }
                    val isSelected = viewModel.userAnswers[currentIdx] == index
                    val isCorrect = index == viewModel.currentQuestion.correctAnswerIndex
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitted) { viewModel.selectOption(index) },
                        color = when {
                            isSubmitted && isCorrect -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                            isSubmitted && isSelected && !isCorrect -> Color(0xFFD32F2F).copy(alpha = 0.15f)
                            isSelected -> primaryCyan.copy(alpha = 0.15f)
                            else -> Color(0xFF1E1E1E)
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isSubmitted && isCorrect -> Color(0xFF4CAF50)
                                isSubmitted && isSelected && !isCorrect -> Color(0xFFF44336)
                                isSelected -> primaryCyan
                                else -> Color(0xFF2C2C2E)
                            }
                        )
                    ) {
                        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSubmitted && isCorrect -> Color(0xFF4CAF50)
                                            isSubmitted && isSelected && !isCorrect -> Color(0xFFF44336)
                                            isSelected -> primaryCyan
                                            else -> Color(0xFF2C2C2E)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    key,
                                    color = if (isSelected || (isSubmitted && isCorrect)) Color.Black else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(option, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            
                            if (isSubmitted) {
                                if (isCorrect) Icon(Icons.Default.CheckCircle, null, tint = Color.Green)
                                else if (isSelected) Icon(Icons.Default.Close, null, tint = Color.Red)
                            }
                        }
                    }
                }
                
                if (isSubmitted) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Info, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Explanation: ${viewModel.currentQuestion.explanation}",
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Buttons
        if (isReviewMode) {
            Button(
                onClick = { isReviewMode = false },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("BACK TO RESULTS", fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentIdx > 0) {
                    OutlinedButton(
                        onClick = { viewModel.previousQuestion() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("PREVIOUS", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                val isLast = currentIdx == viewModel.questions.size - 1
                Button(
                    onClick = {
                        when {
                            hasSelected && !isSubmitted -> viewModel.submitAnswer()
                            isLast -> {
                                val unanswered = viewModel.getUnansweredQuestions()
                                if (unanswered.isNotEmpty()) {
                                    unansweredText = unanswered.joinToString(", ") { "#$it" }
                                    showUnansweredDialog = true
                                } else {
                                    showResultScreen = true
                                }
                            }
                            else -> viewModel.nextQuestion()
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLast || (hasSelected && !isSubmitted)) primaryCyan else Color(0xFF2C2C2E)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val text = when {
                        hasSelected && !isSubmitted -> "SUBMIT"
                        isLast -> "FINISH"
                        else -> "NEXT"
                    }
                    Text(text, color = if (isLast || (hasSelected && !isSubmitted)) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}