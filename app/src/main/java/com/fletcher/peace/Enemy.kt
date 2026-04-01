package com.fletcher.peace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    private val radius: Float = ENEMY_CIRCLE_RADIUS, // Use the constant as a default
    private val speed: Float = ENEMY_SPEED           // Use the constant as a default
) {

    /**
     * Updates the enemy's position to move it towards the player.
     */
    fun moveTowards(playerPos: Offset) {
        val angle = atan2(playerPos.y - position.y, playerPos.x - position.x)
        position = Offset(
            x = position.x + cos(angle) * speed,
            y = position.y + sin(angle) * speed
        )
    }

    /**
     * Checks for a collision between this enemy and the player.
     */
    fun checkCollision(playerPos: Offset, playerRadius: Float): Boolean {
        val distanceSquared = (playerPos.x - position.x).pow(2) + (playerPos.y - position.y).pow(2)
        return distanceSquared < (playerRadius + radius).pow(2)
    }

    /**
     * Composable function for the Enemy to draw itself.
     */
    @Composable
    fun Draw() {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color.Red, radius, position)
        }
    }
}
