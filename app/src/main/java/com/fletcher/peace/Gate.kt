package com.fletcher.peace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

class Gate(
    val id: Long,
    private val anchorStart: Offset,
    private val anchorEnd: Offset,
    private val baseRadius: Float = 24f
) {
    var currentStart by mutableStateOf(anchorStart)
    var currentEnd by mutableStateOf(anchorEnd)
    var isLethal by mutableStateOf(false)
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
        LaunchedEffect(Unit) {
            delay(500)
            isLethal = true
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val gateColor = if (isLethal) Color(0xFFFFA500) else Color(0xFFFFA500).copy(alpha = 0.5f)

            // 1. Fuzzy Blur Energy Effect
            for (i in 1..7) {
                drawLine(
                    color = gateColor.copy(alpha = 0.1f),
                    start = currentStart,
                    end = currentEnd,
                    strokeWidth = 15f + (i * 12f),
                    cap = StrokeCap.Round
                )
            }

            // 2. Draw Bell-shaped ends
            drawBell(currentStart, currentEnd, gateColor, isLethal)
            drawBell(currentEnd, currentStart, gateColor, isLethal)
        }
    }

    private fun DrawScope.drawBell(pos: Offset, pointingTowards: Offset, color: Color, lethal: Boolean) {
        val angle = atan2(pos.y - pointingTowards.y, pos.x - pointingTowards.x)
        val radius = baseRadius
        
        val path = Path().apply {
            val mouthAngle1 = angle + PI.toFloat() / 3.5f
            val mouthAngle2 = angle - PI.toFloat() / 3.5f
            val mouthWidth = radius * 1.4f
            moveTo(pos.x + cos(mouthAngle1) * mouthWidth, pos.y + sin(mouthAngle1) * mouthWidth)
            val topPos = pos - Offset(cos(angle) * radius * 0.5f, sin(angle) * radius * 0.5f)
            quadraticTo(pos.x - cos(angle) * radius * 0.2f, pos.y - sin(angle) * radius * 0.2f, topPos.x, topPos.y)
            quadraticTo(pos.x - cos(angle) * radius * 0.2f, pos.y - sin(angle) * radius * 0.2f, pos.x + cos(mouthAngle2) * mouthWidth, pos.y + sin(mouthAngle2) * mouthWidth)
            quadraticTo(pos.x + cos(angle) * mouthWidth * 1.1f, pos.y + sin(angle) * mouthWidth * 1.1f, pos.x + cos(mouthAngle1) * mouthWidth, pos.y + sin(mouthAngle1) * mouthWidth)
            close()
        }
        
        if (lethal) {
            for (i in 0 until 8) {
                val spineAngle = angle + (i - 3.5f) * 0.4f
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = pos,
                    end = pos + Offset(cos(spineAngle) * radius * 1.8f, sin(spineAngle) * radius * 1.8f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }

        drawPath(path, Color.White, alpha = if (lethal) 0.5f else 0.2f)
        drawPath(path, color)
    }

    fun checkLethalCollision(playerPos: Offset, playerRadius: Float): Boolean {
        if (!isLethal) return false
        val d1 = sqrt((playerPos.x - currentStart.x).pow(2) + (playerPos.y - currentStart.y).pow(2))
        val d2 = sqrt((playerPos.x - currentEnd.x).pow(2) + (playerPos.y - currentEnd.y).pow(2))
        return d1 < playerRadius + (baseRadius * 1.5f) || d2 < playerRadius + (baseRadius * 1.5f)
    }

    fun checkForActivation(playerPos: Offset, playerRadius: Float): Boolean {
        return didLineCrossCircle(currentStart, currentEnd, playerPos, playerRadius + 20f)
    }
}

private const val GATE_LENGTH = 250f

fun createRandomGate(screenSize: IntSize, padding: Float): Gate {
    val totalPadding = padding + 60f
    val angle = Random.nextFloat() * 2 * PI.toFloat()
    val dx = cos(angle) * GATE_LENGTH
    val dy = sin(angle) * GATE_LENGTH
    val minX = max(totalPadding, totalPadding - dx)
    val maxX = min(screenSize.width - totalPadding, screenSize.width - totalPadding - dx)
    val minY = max(totalPadding, totalPadding - dy)
    val maxY = min(screenSize.height - totalPadding, screenSize.height - totalPadding - dy)
    val startX = if (maxX > minX) Random.nextFloat() * (maxX - minX) + minX else screenSize.width / 2f
    val startY = if (maxY > minY) Random.nextFloat() * (maxY - minY) + minY else screenSize.height / 2f
    val start = Offset(startX, startY)
    val end = Offset(startX + dx, startY + dy)
    return Gate(id = System.currentTimeMillis(), anchorStart = start, anchorEnd = end)
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
