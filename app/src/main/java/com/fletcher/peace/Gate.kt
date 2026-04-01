package com.fletcher.peace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
        time += 0.01f
        val driftX = sin(time) * 30f
        val driftY = cos(time * 0.8f) * 30f
        val drift = Offset(driftX, driftY)

        currentStart = anchorStart + drift
        currentEnd = anchorEnd + drift
    }

    @Composable
    fun Draw() {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gateColor = Color(0xFFFFA500)

            drawLine(
                Color.White, currentStart, currentEnd,
                strokeWidth = 10f, cap = StrokeCap.Round, alpha = 0.6f
            )
            drawCircle(Color.White, endRadius + 4f, currentStart, alpha = 0.6f)
            drawCircle(Color.White, endRadius + 4f, currentEnd, alpha = 0.6f)

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
        return didLineCrossCircle(currentStart, currentEnd, playerPos, playerRadius)
    }
}

private const val GATE_LENGTH = 250f

fun createRandomGate(screenSize: IntSize, padding: Float): Gate {
    val totalPadding = padding + 50f // Extra padding to account for drift
    
    // 1. Pick a random angle first
    val angle = Random.nextFloat() * 2 * PI.toFloat()
    val dx = cos(angle) * GATE_LENGTH
    val dy = sin(angle) * GATE_LENGTH

    // 2. Pick a start point such that the end point is also within bounds
    val minX = max(totalPadding, totalPadding - dx)
    val maxX = min(screenSize.width - totalPadding, screenSize.width - totalPadding - dx)
    val minY = max(totalPadding, totalPadding - dy)
    val maxY = min(screenSize.height - totalPadding, screenSize.height - totalPadding - dy)

    val startX = if (maxX > minX) Random.nextFloat() * (maxX - minX) + minX else screenSize.width / 2f
    val startY = if (maxY > minY) Random.nextFloat() * (maxY - minY) + minY else screenSize.height / 2f
    
    val start = Offset(startX, startY)
    val end = Offset(startX + dx, startY + dy)

    return Gate(
        id = System.currentTimeMillis(),
        anchorStart = start,
        anchorEnd = end
    )
}

private fun didLineCrossCircle(
    lineStart: Offset,
    lineEnd: Offset,
    circleCenter: Offset,
    circleRadius: Float
): Boolean {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y

    val lineLengthSq = dx * dx + dy * dy
    if (lineLengthSq == 0f) return false

    val t = ((circleCenter.x - lineStart.x) * dx + (circleCenter.y - lineStart.y) * dy) / lineLengthSq
    val constrainedT = t.coerceIn(0f, 1f)

    val closestX = lineStart.x + constrainedT * dx
    val closestY = lineStart.y + constrainedT * dy

    val distanceSq = (circleCenter.x - closestX).pow(2) + (circleCenter.y - closestY).pow(2)
    return distanceSq <= circleRadius.pow(2)
}
