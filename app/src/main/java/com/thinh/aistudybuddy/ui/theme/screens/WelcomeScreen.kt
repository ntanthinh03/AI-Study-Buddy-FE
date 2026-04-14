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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.R
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onFinished: () -> Unit
) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    LaunchedEffect(Unit) {
        delay(3500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(1500, easing = LinearOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = tween(1500, easing = LinearOutSlowInEasing),
                        initialOffsetY = { 200 }
                    )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.buddy_logo_png),
                    contentDescription = "Buddy Logo",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 32.dp)
                )

                Text(
                    text = "Welcome to AI Study Buddy",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Ignite your focus. Empower your exams.",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }
        }
    }
}