package com.example.greetingcard.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.launch

// --- Explosion Constants ---
const val GATE_EXPLOSION_RADIUS = 350f

class Explosion(
    val id: Long,
    val center: Offset,
    val color: Color = Color.White, // Added color parameter
    private val initialRadius: Float,
    private val targetRadius: Float = GATE_EXPLOSION_RADIUS
) {
    private val animatedRadius = Animatable(initialRadius)
    private val animatedAlpha = Animatable(1f)

    @Composable
    fun Animate(onAnimationFinished: (Explosion) -> Unit) {
        LaunchedEffect(Unit) {
            launch {
                animatedRadius.animateTo(
                    targetValue = targetRadius,
                    // Slightly faster animation for more "pop"
                    animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing)
                )
            }
            launch {
                animatedAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                )
            }
            onAnimationFinished(this@Explosion)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Inner Flash (Filled Circle)
            drawCircle(
                color = color,
                radius = animatedRadius.value * 0.5f,
                center = center,
                alpha = animatedAlpha.value * 0.5f
            )
            // Outer Shockwave (Stroke)
            drawCircle(
                color = color,
                radius = animatedRadius.value,
                center = center,
                style = Stroke(width = 12f),
                alpha = animatedAlpha.value
            )
        }
    }
}