package com.thinh.aistudybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.ui.components.QuizOptionItem
import com.thinh.aistudybuddy.viewmodel.QuizViewModel

@Composable
fun QuizScreen(
    onCloseClick: () -> Unit,
    viewModel: QuizViewModel = viewModel()
) {
    var showResultScreen by remember { mutableStateOf(false) }
    var isReviewMode by remember { mutableStateOf(false) }
    var showUnansweredDialog by remember { mutableStateOf(false) }
    var unansweredText by remember { mutableStateOf("") }

    if (showResultScreen && !isReviewMode) {
        QuizResultScreen(
            viewModel = viewModel,
            onReviewQuestion = { index ->
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
            title = { Text("Finish Quiz?") },
            text = { Text("There are unanswered quiz questions: $unansweredText. Do you want to submit anyway?") },
            confirmButton = {
                TextButton(onClick = {
                    showUnansweredDialog = false
                    showResultScreen = true
                }) { Text("Confirm", color = Color(0xFF1976D2)) }
            },
            dismissButton = {
                TextButton(onClick = { showUnansweredDialog = false }) { Text("Cancel", color = Color.Gray) }
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
            .background(Color(0xFF121212))
            .padding(24.dp)
    ) {
        IconButton(onClick = if (isReviewMode) { { isReviewMode = false } } else onCloseClick) {
            Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(32.dp))
        }

        LinearProgressIndicator(
            progress = { if (viewModel.questions.isNotEmpty()) (currentIdx + 1).toFloat() / viewModel.questions.size else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(8.dp),
            color = Color(0xFF1976D2),
            trackColor = Color(0xFF2C2C2E)
        )

        if (viewModel.questions.isNotEmpty()) {
            Text(
                text = viewModel.currentQuestion.question,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            viewModel.currentQuestion.options.forEachIndexed { index, option ->
                QuizOptionItem(
                    label = option,
                    isSelected = viewModel.userAnswers[currentIdx] == index,
                    isCorrect = index == viewModel.currentQuestion.correctAnswerIndex,
                    showResult = isSubmitted,
                    onClick = { viewModel.selectOption(index) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isSubmitted) {
                Text(
                    text = "Explanation: ${viewModel.currentQuestion.explanation}",
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isReviewMode) {
            Button(
                onClick = { isReviewMode = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Results", fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentIdx > 0) {
                    Button(
                        onClick = { viewModel.previousQuestion() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Previous", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        when {
                            hasSelected && !isSubmitted -> viewModel.submitAnswer()
                            currentIdx == viewModel.questions.size - 1 -> {
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
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val text = when {
                        hasSelected && !isSubmitted -> "Submit"
                        currentIdx == viewModel.questions.size - 1 -> "Finish"
                        else -> "Next"
                    }
                    Text(text, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}