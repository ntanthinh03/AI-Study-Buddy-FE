package com.thinh.aistudybuddy.ui.pages

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.ui.theme.DeepSpaceBackground
import com.thinh.aistudybuddy.ui.theme.PrimaryNeonTeal
import com.thinh.aistudybuddy.ui.theme.SecondaryTangerine
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VersusCountdownScreen(
    matchId: String,
    onFinished: (String) -> Unit
) {
    val context = LocalContext.current
    var count by remember { mutableStateOf(3) }
    var showStartText by remember { mutableStateOf(false) }


    fun triggerVibration(durationMs: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    LaunchedEffect(Unit) {

        triggerVibration(70L)
        delay(1000)
        

        count = 2
        triggerVibration(70L)
        delay(1000)
        

        count = 1
        triggerVibration(70L)
        delay(1000)
        

        showStartText = true
        triggerVibration(400L)
        delay(800)
        
        onFinished(matchId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = if (showStartText) "BATTLE!" else count.toString(),
            transitionSpec = {
                (fadeIn(animationSpec = tween(200, easing = EaseInQuad)) + scaleIn(initialScale = 0.3f, animationSpec = tween(300, easing = EaseOutBack))) togetherWith
                        fadeOut(animationSpec = tween(200, easing = EaseOutQuad)) + scaleOut(targetScale = 1.8f, animationSpec = tween(200))
            },
            label = "countdown_animation"
        ) { targetCount ->
            Text(
                text = targetCount,
                color = if (showStartText) SecondaryTangerine else PrimaryNeonTeal,
                fontSize = if (showStartText) 72.sp else 120.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
