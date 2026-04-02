package com.fletcher.peace

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// --- Enemy Constants ---
private const val ENEMY_CIRCLE_RADIUS = 15f
private const val ENEMY_SPEED = 4f

// --- Enemy Class ---
class Enemy(
    val id: Long,
    var position: Offset,
    private val radius: Float = ENEMY_CIRCLE_RADIUS,
    private val speed: Float = ENEMY_SPEED
) {

    fun moveTowards(playerPos: Offset) {
        val angle = atan2(playerPos.y - position.y, playerPos.x - position.x)
        position = Offset(
            x = position.x + cos(angle) * speed,
            y = position.y + sin(angle) * speed
        )
    }

    fun checkCollision(playerPos: Offset, playerRadius: Float): Boolean {
        val distanceSquared = (playerPos.x - position.x).pow(2) + (playerPos.y - position.y).pow(2)
        return distanceSquared < (playerRadius + radius).pow(2)
    }

    @Composable
    fun Draw() {
        val infiniteTransition = rememberInfiniteTransition(label = "enemyGlow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle animated glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Red.copy(alpha = glowAlpha), Color.Transparent),
                    center = position,
                    radius = radius * 2.5f * glowScale
                ),
                radius = radius * 2.5f * glowScale,
                center = position
            )
            
            // Core enemy body
            drawCircle(Color.Red, radius, position)
            // Add a small inner detail to make it more "sprite-like"
            drawCircle(Color.Black.copy(alpha = 0.3f), radius * 0.6f, position)
        }
    }
}
