package com.thinh.aistudybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.viewmodel.QuizViewModel

@Composable
fun QuizResultScreen(
    viewModel: QuizViewModel,
    onReviewQuestion: (Int) -> Unit,
    onRetake: () -> Unit, // Thêm lambda xử lý làm lại
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Quiz Results", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Score: ${viewModel.score} / ${viewModel.sampleQuestions.size * 10}", color = Color.Gray, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(48.dp))

        Box(modifier = Modifier.width(300.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.wrapContentHeight()
            ) {
                items(10) { index ->
                    val status = viewModel.getQuestionStatus(index)
                    val boxColor = when (status) {
                        QuizViewModel.QuestionStatus.CORRECT -> Color(0xFF2E7D32) // Xanh lá cho câu đúng
                        QuizViewModel.QuestionStatus.INCORRECT -> Color(0xFFD32F2F) // Đỏ cho câu sai
                        QuizViewModel.QuestionStatus.UNANSWERED -> Color(0xFF2C2C2E)
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(boxColor, shape = RoundedCornerShape(12.dp))
                            .clickable(enabled = index < viewModel.sampleQuestions.size) {
                                onReviewQuestion(index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text((index + 1).toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        // NÚT LÀM LẠI VỚI NEW QUIZ (Thêm mới)
        Button(
            onClick = onRetake,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), // Màu Cyan nổi bật
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Retake with New Quiz", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // NÚT QUAY LẠI TRANG CHỦ
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Back to Home", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}