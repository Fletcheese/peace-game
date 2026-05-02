package com.fletcheese.peace

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

class Enemy(
    val id: Long,
    initialPosition: Offset,
    val radius: Float = 15f,
    private val speed: Float = 4f,
    private val color: Color = Color.Red
) {
    var position by mutableStateOf(initialPosition)

    fun moveTowards(playerPos: Offset, allEnemies: List<Enemy>) {
        val angleToPlayer = atan2(playerPos.y - position.y, playerPos.x - position.x)
        var velocity = Offset(cos(angleToPlayer) * speed, sin(angleToPlayer) * speed)

        var separation = Offset.Zero
        val separationDistance = radius * 2.5f
        for (other in allEnemies) {
            if (other.id != this.id) {
                val dist = position.distanceTo(other.position)
                if (dist < separationDistance && dist > 0.1f) {
                    val pushDirection = (position - other.position)
                    val pushStrength = 1.0f - (dist / separationDistance)
                    separation += (pushDirection / dist) * pushStrength * 2.0f
                }
            }
        }
        
        velocity += separation
        position += velocity
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
            val currentPos = position
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = glowAlpha), Color.Transparent),
                    center = currentPos,
                    radius = radius * 2.5f * glowScale
                ),
                radius = radius * 2.5f * glowScale,
                center = currentPos
            )
            
            drawCircle(color, radius, currentPos)
            drawCircle(Color.Black.copy(alpha = 0.3f), radius * 0.6f, currentPos)
        }
    }
}
