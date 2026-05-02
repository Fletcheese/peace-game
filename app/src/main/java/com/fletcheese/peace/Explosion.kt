package com.fletcheese.peace

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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

// --- Explosion Constants ---
const val GATE_EXPLOSION_RADIUS = 350f

class Explosion(
    val id: Long,
    val center: Offset,
    val color: Color = Color.White,
    private val initialRadius: Float,
    private val targetRadius: Float = GATE_EXPLOSION_RADIUS
) {
    private val animatedRadius = Animatable(initialRadius)
    private val animatedAlpha = Animatable(1f)

    @Composable
    fun Animate(onAnimationFinished: (Explosion) -> Unit) {
        LaunchedEffect(Unit) {
            val rAnim = launch {
                animatedRadius.animateTo(
                    targetValue = targetRadius,
                    animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing)
                )
            }
            val aAnim = launch {
                animatedAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                )
            }
            // Wait for both animations to finish
            joinAll(rAnim, aAnim)
            onAnimationFinished(this@Explosion)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Shadowing fix: use this@Explosion.center to avoid DrawScope.center
            val explosionCenter = this@Explosion.center
            
            drawCircle(
                color = color,
                radius = animatedRadius.value * 0.5f,
                center = explosionCenter,
                alpha = animatedAlpha.value * 0.5f
            )
            drawCircle(
                color = color,
                radius = animatedRadius.value,
                center = explosionCenter,
                style = Stroke(width = 12f),
                alpha = animatedAlpha.value
            )
        }
    }
}
