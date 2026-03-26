package com.thinh.aistudybuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Đảm bảo import đúng ChatMessage
import com.thinh.aistudybuddy.data.model.ChatMessage

@Composable
fun ChatBubble(
    message: ChatMessage, // Đổi từ MessageModel thành ChatMessage
    onStartQuiz: () -> Unit
) {
    // Sử dụng message.text thay vì message.content
    val isQuizMessage = message.text.contains("generated a 10-question quiz")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start) {
            Surface(
                color = if (message.isUser) Color(0xFF2C2C2E) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = message.text, // Sử dụng .text
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .padding(if (message.isUser) 12.dp else 0.dp)
                        .widthIn(max = 280.dp)
                )
            }

            if (isQuizMessage) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onStartQuiz,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Quiz", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}