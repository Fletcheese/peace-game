package com.fletcheese.peace

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
    private val baseRadius: Float = 24f,
    private val initialLength: Float = 250f
) {
    private val center = (anchorStart + anchorEnd) / 2f
    private val initialHalfVector = anchorStart - center
    
    var currentStart by mutableStateOf(anchorStart)
    var currentEnd by mutableStateOf(anchorEnd)
    var isLethal by mutableStateOf(false)
    private var time = Random.nextFloat() * 1000f
    private var spawnRamp = 0f 

    fun update(accelMod: Float = 0.01f, maxSpeedMod: Float = 1.0f) {
        spawnRamp = (spawnRamp + 0.0015f).coerceAtMost(1f)
        
        val timeStep = (accelMod + (sin(time * 0.2f) * cos(time * 0.5f) * (accelMod * 0.2f))) * maxSpeedMod
        time += timeStep
        
        val driftX = (sin(time * 0.6f) * 30f + sin(time * 1.3f) * 15f + cos(time * 2.8f) * 8f) * spawnRamp * maxSpeedMod
        val driftY = (cos(time * 0.8f) * 30f + sin(time * 1.7f) * 15f + sin(time * 3.1f) * 8f) * spawnRamp * maxSpeedMod
        val drift = Offset(driftX, driftY)
        
        val rotation = (sin(time * 0.3f) * 1.1f + cos(time * 0.8f) * 0.4f + sin(time * 2.0f) * 0.1f) * spawnRamp
        val cosR = cos(rotation)
        val sinR = sin(rotation)
        
        val rotatedHalf = Offset(
            initialHalfVector.x * cosR - initialHalfVector.y * sinR,
            initialHalfVector.x * sinR + initialHalfVector.y * cosR
        )
        
        currentStart = center + drift + rotatedHalf
        currentEnd = center + drift - rotatedHalf
    }

    @Composable
    fun Draw(color: Color = Color(0xFFFFA500)) {
        LaunchedEffect(Unit) {
            delay(500)
            isLethal = true
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val gateColor = if (isLethal) color else color.copy(alpha = 0.5f)

            for (i in 1..7) {
                drawLine(
                    color = gateColor.copy(alpha = 0.1f),
                    start = currentStart,
                    end = currentEnd,
                    strokeWidth = 15f + (i * 12f),
                    cap = StrokeCap.Round
                )
            }

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
        
        // Define a tighter lethal radius for the half-circle
        val lethalRadius = playerRadius + (baseRadius * 1.2f)
        
        // Check start end end zone
        val toPlayerStart = playerPos - currentStart
        if (toPlayerStart.getDistance() < lethalRadius) {
            // Direction pointing away from the gate from currentStart
            val awayDir = (currentStart - currentEnd).let { if (it.getDistance() < 1f) Offset(1f, 0f) else it / it.getDistance() }
            // Dot product > 0 means the player is on the outer half-circle
            if (toPlayerStart.x * awayDir.x + toPlayerStart.y * awayDir.y > 0) return true
        }

        // Check end end end zone
        val toPlayerEnd = playerPos - currentEnd
        if (toPlayerEnd.getDistance() < lethalRadius) {
            // Direction pointing away from the gate from currentEnd
            val awayDir = (currentEnd - currentStart).let { if (it.getDistance() < 1f) Offset(1f, 0f) else it / it.getDistance() }
            if (toPlayerEnd.x * awayDir.x + toPlayerEnd.y * awayDir.y > 0) return true
        }
        
        return false
    }

    fun checkForActivation(playerPos: Offset, playerRadius: Float): Boolean {
        return didLineCrossCircle(currentStart, currentEnd, playerPos, playerRadius + 20f)
    }
}

fun createRandomGate(screenSize: IntSize, padding: Float, length: Float = 250f, endZoneSize: Float = 24f): Gate {
    val totalPadding = padding + 60f
    val angle = Random.nextFloat() * 2 * PI.toFloat()
    val dx = cos(angle) * length
    val dy = sin(angle) * length
    val minX = max(totalPadding, totalPadding - dx)
    val maxX = min(screenSize.width - totalPadding, screenSize.width - totalPadding - dx)
    val minY = max(totalPadding, totalPadding - dy)
    val maxY = min(screenSize.height - totalPadding, screenSize.height - totalPadding - dy)
    val startX = if (maxX > minX) Random.nextFloat() * (maxX - minX) + minX else screenSize.width / 2f
    val startY = if (maxY > minY) Random.nextFloat() * (maxY - minY) + minY else screenSize.height / 2f
    val start = Offset(startX, startY)
    val end = Offset(startX + dx, startY + dy)
    return Gate(id = System.currentTimeMillis(), anchorStart = start, anchorEnd = end, baseRadius = endZoneSize, initialLength = length)
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
