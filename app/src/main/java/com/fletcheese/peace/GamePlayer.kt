package com.fletcheese.peace

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

class GamePlayer(initialPosition: Offset) {
    var position by mutableStateOf(initialPosition)
    var rotation by mutableFloatStateOf(0f)

    fun snapTo(offset: Offset) {
        position = offset
    }

    fun updateRotation(velocity: Offset) {
        if (velocity.getDistance() > 0.1f) {
            val targetRotation = Math.toDegrees(
                kotlin.math.atan2(velocity.y.toDouble(), velocity.x.toDouble())
            ).toFloat() + 90f 
            rotation = targetRotation
        }
    }

    @Composable
    fun Draw(size: Float = 37.5f) {
        val infiniteTransition = rememberInfiniteTransition(label = "playerGlow")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = position
            val radius = size
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Cyan.copy(alpha = 0.4f),
                        Color.Magenta.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 3f * pulseScale
                ),
                radius = radius * 3f * pulseScale,
                center = center
            )

            rotate(rotation, center) {
                val rainbowBrush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red, Color.Magenta, Color.Blue, 
                        Color.Cyan, Color.Green, Color.Yellow, Color.Red
                    ),
                    center = center
                )

                drawCircle(
                    brush = rainbowBrush,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 6f)
                )

                drawLine(
                    brush = rainbowBrush,
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y + radius),
                    strokeWidth = 6f
                )

                val cos45 = 0.707f
                val sin45 = 0.707f

                drawLine(
                    brush = rainbowBrush,
                    start = center,
                    end = Offset(center.x - radius * cos45, center.y + radius * sin45),
                    strokeWidth = 6f
                )
                drawLine(
                    brush = rainbowBrush,
                    start = center,
                    end = Offset(center.x + radius * cos45, center.y + radius * sin45),
                    strokeWidth = 6f
                )
            }
        }
    }
}
