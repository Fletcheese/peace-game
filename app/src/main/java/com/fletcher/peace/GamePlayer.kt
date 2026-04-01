package com.fletcher.peace

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.CoroutineScope

// --- Player Constants ---
const val PLAYER_CIRCLE_RADIUS = 25f

// --- Player Class ---
class GamePlayer(
    initialPosition: Offset,
    private val coroutineScope: CoroutineScope
) {
    val position = Animatable(initialPosition, Offset.VectorConverter)
    var rotation by mutableFloatStateOf(0f)

    /**
     * Instantly moves the player to a new position.
     */
    suspend fun snapTo(offset: Offset) {
        position.snapTo(offset)
    }

    fun updateRotation(velocity: Offset) {
        if (velocity.getDistance() > 0.1f) {
            val targetRotation = Math.toDegrees(
                kotlin.math.atan2(velocity.y.toDouble(), velocity.x.toDouble())
            ).toFloat() + 90f 
            rotation = targetRotation
        }
    }

    /**
     * Composable function for the Player to draw itself as a rotating rainbow peace sign.
     */
    @Composable
    fun Draw() {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = position.value
            val radius = PLAYER_CIRCLE_RADIUS
            
            rotate(rotation, center) {
                // Rainbow gradient for the peace sign
                val rainbowBrush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red, Color.Magenta, Color.Blue, 
                        Color.Cyan, Color.Green, Color.Yellow, Color.Red
                    ),
                    center = center
                )

                // 1. Draw the outer circle
                drawCircle(
                    brush = rainbowBrush,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 4f)
                )

                // 2. Draw the vertical line
                drawLine(
                    brush = rainbowBrush,
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y + radius),
                    strokeWidth = 4f
                )

                // 3. Draw the angled lines
                val cos45 = 0.707f
                val sin45 = 0.707f

                drawLine(
                    brush = rainbowBrush,
                    start = center,
                    end = Offset(center.x - radius * cos45, center.y + radius * sin45),
                    strokeWidth = 4f
                )
                drawLine(
                    brush = rainbowBrush,
                    start = center,
                    end = Offset(center.x + radius * cos45, center.y + radius * sin45),
                    strokeWidth = 4f
                )
            }
        }
    }
}
