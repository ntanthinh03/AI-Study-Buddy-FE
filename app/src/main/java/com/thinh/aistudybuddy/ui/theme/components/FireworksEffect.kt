package com.thinh.aistudybuddy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FireworksEffect() {
    val particles = remember {
        List(100) {
            Particle(
                angle = Random.nextFloat() * 2 * Math.PI.toFloat(),
                speed = Random.nextFloat() * 15f + 5f,
                color = Color(
                    red = Random.nextFloat(),
                    green = Random.nextFloat(),
                    blue = Random.nextFloat(),
                    alpha = 1f
                )
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fireworks")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = durationBasedTween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 3)
        particles.forEach { particle ->
            val x = center.x + cos(particle.angle) * particle.speed * progress * 500f
            val y = center.y + sin(particle.angle) * particle.speed * progress * 500f

            drawCircle(
                color = particle.color.copy(alpha = 1f - progress),
                radius = 8f * (1f - progress),
                center = Offset(x, y)
            )
        }
    }
}

private fun durationBasedTween(duration: Int): TweenSpec<Float> = tween(
    durationMillis = duration,
    easing = LinearOutSlowInEasing
)

data class Particle(val angle: Float, val speed: Float, val color: Color)