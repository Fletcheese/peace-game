package com.fletcheese.peace

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*
import kotlin.random.Random

// Magnetism constants
private const val SHARD_MAGNET_RADIUS = 125f
private const val SHARD_LIFETIME_MS = 8000L

class Shard(val id: Long, val anchorPosition: Offset) {
    var position by mutableStateOf(anchorPosition)
    private var velocity = Offset.Zero
    private var isMagnetized by mutableStateOf(false)
    private var time = Random.nextFloat() * 100f
    private val rotationOffset = Random.nextFloat() * 360f
    
    private var age = 0L
    var isExpired by mutableStateOf(false)

    fun update(playerPos: Offset) {
        if (isExpired) return
        
        age += 16 // Assuming ~60fps update
        if (age >= SHARD_LIFETIME_MS) {
            isExpired = true
            return
        }

        time += 0.02f
        val distToPlayer = sqrt((position.x - playerPos.x).pow(2) + (position.y - playerPos.y).pow(2))
        
        if (distToPlayer < SHARD_MAGNET_RADIUS) {
            isMagnetized = true
        }

        if (isMagnetized) {
            val direction = playerPos - position
            val distance = direction.getDistance().coerceAtLeast(1f)
            val acceleration = (direction / distance) * 0.9f
            velocity += acceleration
            velocity *= 0.94f
            position += velocity
        } else {
            val driftX = sin(time) * 15f
            val driftY = cos(time * 0.7f) * 15f
            position = anchorPosition + Offset(driftX, driftY)
        }
    }

    @Composable
    fun Draw() {
        if (isExpired) return
        
        val infiniteTransition = rememberInfiniteTransition(label = "shardShine")
        val shineAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shine"
        )
        
        // Blink faster when about to despawn
        val timeRemaining = SHARD_LIFETIME_MS - age
        val flickerAlpha = if (timeRemaining < 2000L) {
             val flicker = (timeRemaining / 200).toInt() % 2 == 0
             if (flicker) 1.0f else 0.3f
        } else 1.0f

        Canvas(modifier = Modifier.fillMaxSize()) {
            rotate(rotationOffset + time * 10f, position) {
                // Main shard body
                drawRect(
                    color = Color.Gray.copy(alpha = shineAlpha * 0.8f * flickerAlpha),
                    topLeft = Offset(position.x - 6f, position.y - 12f),
                    size = Size(12f, 24f)
                )
                // Reflective "shine" layer
                drawRect(
                    color = Color.White.copy(alpha = shineAlpha * 0.4f * flickerAlpha),
                    topLeft = Offset(position.x - 4f, position.y - 10f),
                    size = Size(4f, 20f)
                )
            }
        }
    }
}
