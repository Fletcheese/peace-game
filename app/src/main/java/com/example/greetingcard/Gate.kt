package com.example.greetingcard.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import kotlin.math.*
import kotlin.random.Random

class Gate(
    val id: Long,
    private val anchorStart: Offset,
    private val anchorEnd: Offset,
    private val endRadius: Float = 12f
) {
    // Movement state
    var currentStart by mutableStateOf(anchorStart)
    var currentEnd by mutableStateOf(anchorEnd)
    private var time = Random.nextFloat() * 100f

    fun update() {
        time += 0.01f // Slow speed
        // Vaguely random drifting pattern using Sine/Cosine
        val driftX = sin(time) * 30f
        val driftY = cos(time * 0.8f) * 30f
        val drift = Offset(driftX, driftY)

        currentStart = anchorStart + drift
        currentEnd = anchorEnd + drift
    }

    @Composable
    fun Draw() {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gateColor = Color(0xFFFFA500) // Orange

            // 1. Draw White Outline (slightly thicker)
            drawLine(
                Color.White, currentStart, currentEnd,
                strokeWidth = 10f, cap = StrokeCap.Round, alpha = 0.6f
            )
            drawCircle(Color.White, endRadius + 4f, currentStart, alpha = 0.6f)
            drawCircle(Color.White, endRadius + 4f, currentEnd, alpha = 0.6f)

            // 2. Draw Orange Gate
            drawLine(gateColor, currentStart, currentEnd, strokeWidth = 5f, cap = StrokeCap.Round)
            drawCircle(gateColor, endRadius, currentStart)
            drawCircle(gateColor, endRadius, currentEnd)
        }
    }

    fun checkLethalCollision(playerPos: Offset, playerRadius: Float): Boolean {
        val d1 = sqrt((playerPos.x - currentStart.x).pow(2) + (playerPos.y - currentStart.y).pow(2))
        val d2 = sqrt((playerPos.x - currentEnd.x).pow(2) + (playerPos.y - currentEnd.y).pow(2))
        return d1 < playerRadius + endRadius || d2 < playerRadius + endRadius
    }

    fun checkForActivation(playerPos: Offset, playerRadius: Float): Boolean {
        // Simple distance check to the line (approximate)
        return didLineCrossCircle(currentStart, currentEnd, playerPos, playerRadius)
    }
}
// --- Gate Constants ---
private const val GATE_LENGTH = 250f

/**
 * Creates a gate with a fixed length at a random position and orientation.
 */
fun createRandomGate(screenSize: IntSize, padding: Float): Gate {
    // 1. Pick a random starting point
    val start = createRandomOffset(screenSize, padding)

    // 2. Pick a random angle
    val angle = Random.nextFloat() * 2 * PI.toFloat()

    // 3. Calculate the end point based on length
    val endX = start.x + cos(angle) * GATE_LENGTH
    val endY = start.y + sin(angle) * GATE_LENGTH

    val end = Offset(endX, endY).let {
        Offset(
            it.x.coerceIn(padding, screenSize.width - padding),
            it.y.coerceIn(padding, screenSize.height - padding)
        )
    }

    // Return the new Gate using the anchor coordinates for drifting
    return Gate(
        id = System.currentTimeMillis(),
        anchorStart = start,
        anchorEnd = end
    )
}

/**
 * Shared helper to generate random coordinates within screen bounds
 */
fun createRandomOffset(screenSize: IntSize, padding: Float): Offset {
    if (screenSize.width == 0 || screenSize.height == 0) return Offset.Zero
    return Offset(
        x = Random.nextFloat() * (screenSize.width - 2 * padding) + padding,
        y = Random.nextFloat() * (screenSize.height - 2 * padding) + padding
    )
}