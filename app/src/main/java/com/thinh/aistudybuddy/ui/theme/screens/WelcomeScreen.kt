package com.thinh.aistudybuddy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Import này để dùng màu sắc cố định
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.R // Thay bằng package chuẩn của bạn
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onFinished: () -> Unit // Lambda để chuyển màn hình khi hoàn tất
) {
    // Trạng thái hoạt họa ban đầu
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    // Tự động chuyển màn hình sau một khoảng thời gian
    LaunchedEffect(Unit) {
        delay(3500) // Đợi 3.5 giây
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Ép buộc nền Đen cố định
        contentAlignment = Alignment.Center
    ) {
        // Container chính cho hoạt họa
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(1500, easing = LinearOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = tween(1500, easing = LinearOutSlowInEasing),
                        initialOffsetY = { 200 } // Sửa lỗi initialOffset thành initialOffsetY
                    )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Logo ở giữa màn hình
                Image(
                    painter = painterResource(id = R.drawable.buddy_logo_png), // Thay bằng ID logo của bạn
                    contentDescription = "Buddy Logo",
                    modifier = Modifier
                        .size(200.dp) // Kích thước logo
                        .padding(bottom = 32.dp)
                )

                // Câu khẩu hiệu cho học sinh, sinh viên
                Text(
                    text = "Welcome to AI Study Buddy",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White, // Ép buộc màu chữ trắng cố định
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Ignite your focus. Empower your exams.", // Câu nói hay hay
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f), // Ép buộc màu trắng mờ cố định
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }
        }
    }
}