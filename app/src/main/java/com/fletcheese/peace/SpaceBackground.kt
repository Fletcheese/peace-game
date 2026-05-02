package com.fletcheese.peace

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

@Composable
fun SpaceBackground(modifier: Modifier = Modifier, cameraOffset: Offset = Offset.Zero) {
    val infiniteTransition = rememberInfiniteTransition(label = "spaceTransition")
    
    val autoScrollProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    val starField = remember { generateStarField(200) }
    val dustField = remember { generateDustField(50) }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        
        val width = size.width
        val height = size.height
        val totalWidth = width * 4 
        val totalHeight = height * 4
        
        val autoOffset = autoScrollProgress * totalWidth

        drawRect(Color(0xFF050510))

        // Mix auto-scroll with player movement for parallax
        drawSpaceElements(starField, autoOffset, cameraOffset, totalWidth, totalHeight, width, height)
        drawSpaceElements(dustField, autoOffset, cameraOffset, totalWidth, totalHeight, width, height, isDust = true)

        drawPlanet(autoOffset, cameraOffset, totalWidth, totalHeight, width, height)
    }
}

private fun DrawScope.drawSpaceElements(
    elements: List<SpaceElement>,
    autoOffset: Float,
    cameraOffset: Offset,
    totalWidth: Float,
    totalHeight: Float,
    screenWidth: Float,
    screenHeight: Float,
    isDust: Boolean = false
) {
    if (totalWidth <= 0f) return
    elements.forEach { element ->
        // Parallax: Deeper stars move slower with the camera
        val xShift = (autoOffset * element.parallax + cameraOffset.x * element.parallax) % totalWidth
        val yShift = (cameraOffset.y * element.parallax) % totalHeight
        
        var x = (element.x * totalWidth - xShift)
        var y = (element.y * totalHeight - yShift)
        
        while (x < -element.size * 10) x += totalWidth
        while (x > totalWidth) x -= totalWidth
        while (y < -element.size * 10) y += totalHeight
        while (y > totalHeight) y -= totalHeight

        if (x > -element.size * 10 && x < screenWidth + element.size * 10 &&
            y > -element.size * 10 && y < screenHeight + element.size * 10) {
            if (isDust) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(element.color.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(x, y),
                        radius = (element.size * 5).coerceAtLeast(1f)
                    ),
                    radius = (element.size * 5).coerceAtLeast(1f),
                    center = Offset(x, y)
                )
            } else {
                drawCircle(
                    color = element.color,
                    radius = element.size,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun DrawScope.drawPlanet(autoOffset: Float, cameraOffset: Offset, totalWidth: Float, totalHeight: Float, screenWidth: Float, screenHeight: Float) {
    if (totalWidth <= 0f) return
    val planetParallax = 0.2f
    val planetRadius = 150f
    
    val xShift = (autoOffset * planetParallax + cameraOffset.x * planetParallax) % totalWidth
    val yShift = (cameraOffset.y * planetParallax) % totalHeight
    
    var x = (0.7f * totalWidth - xShift)
    var y = (0.3f * totalHeight - yShift)
    
    while (x < -planetRadius * 2) x += totalWidth
    while (x > totalWidth) x -= totalWidth
    while (y < -planetRadius * 2) y += totalHeight
    while (y > totalHeight) y -= totalHeight
    
    if (x > -planetRadius * 2 && x < screenWidth + planetRadius * 2 &&
        y > -planetRadius * 2 && y < screenHeight + planetRadius * 2) {
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF4B0082), Color(0xFF000033)),
                start = Offset(x - planetRadius, y - planetRadius),
                end = Offset(x + planetRadius, y + planetRadius)
            ),
            radius = planetRadius,
            center = Offset(x, y)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8A2BE2).copy(alpha = 0.3f), Color.Transparent),
                center = Offset(x, y),
                radius = (planetRadius * 1.2f).coerceAtLeast(1f)
            ),
            radius = (planetRadius * 1.2f).coerceAtLeast(1f),
            center = Offset(x, y)
        )
    }
}

data class SpaceElement(
    val x: Float, 
    val y: Float, 
    val size: Float,
    val color: Color,
    val parallax: Float
)

private fun generateStarField(count: Int): List<SpaceElement> {
    return List(count) {
        SpaceElement(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            size = Random.nextFloat() * 2f + 0.5f,
            color = Color.White.copy(alpha = Random.nextFloat() * 0.8f + 0.2f),
            parallax = Random.nextFloat() * 0.4f + 0.1f // Varying depth
        )
    }
}

private fun generateDustField(count: Int): List<SpaceElement> {
    val colors = listOf(Color(0xFF6A5ACD), Color(0xFF483D8B), Color(0xFF8B008B))
    return List(count) {
        SpaceElement(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            size = Random.nextFloat() * 10f + 5f,
            color = colors.random(),
            parallax = Random.nextFloat() * 0.2f + 0.05f
        )
    }
}
