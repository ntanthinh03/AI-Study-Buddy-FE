package com.thinh.aistudybuddy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuizOptionItem(
    label: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            showResult && isCorrect -> Color(0xFF2E7D32).copy(alpha = 0.2f)
            showResult && isSelected && !isCorrect -> Color(0xFFD32F2F).copy(alpha = 0.2f)
            isSelected -> Color(0xFF37474F)
            else -> Color(0xFF1E1E1E)
        }, label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            showResult && isCorrect -> Color(0xFF4CAF50)
            showResult && isSelected && !isCorrect -> Color(0xFFF44336)
            isSelected -> Color(0xFF1976D2)
            else -> Color(0xFF333333)
        }, label = "borderColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, shape = RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(enabled = !showResult, onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            if (showResult) {
                if (isCorrect) Icon(Icons.Default.Check, null, tint = Color.Green)
                else if (isSelected) Icon(Icons.Default.Close, null, tint = Color.Red)
            }
        }
    }
}