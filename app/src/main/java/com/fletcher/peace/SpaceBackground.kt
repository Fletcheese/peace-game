package com.fletcher.peace

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
fun SpaceBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "spaceTransition")
    
    // The "camera" rotation/sliding offset
    val offsetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    // Generate static stars and dust once
    val starField = remember { generateStarField(200) }
    val dustField = remember { generateDustField(50) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val totalWidth = width * 3 // Wide image to slide across
        
        val currentOffset = offsetProgress * totalWidth

        // Draw background color
        drawRect(Color(0xFF050510))

        // Draw Stars and Dust with wrapping logic
        drawSpaceElements(starField, currentOffset, totalWidth, width, height)
        drawSpaceElements(dustField, currentOffset, totalWidth, width, height, isDust = true)

        // Draw a planet in the distance
        drawPlanet(currentOffset, totalWidth, width, height)
    }
}

private fun DrawScope.drawSpaceElements(
    elements: List<SpaceElement>,
    offset: Float,
    totalWidth: Float,
    screenWidth: Float,
    screenHeight: Float,
    isDust: Boolean = false
) {
    elements.forEach { element ->
        // Parallax effect: deeper elements move slower
        val parallaxOffset = (offset * element.parallax) % totalWidth
        var x = (element.x * totalWidth - parallaxOffset)
        
        // Wrap around
        if (x < -element.size) x += totalWidth
        if (x > totalWidth) x -= totalWidth

        // Only draw if on screen
        if (x > -element.size && x < screenWidth + element.size) {
            if (isDust) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(element.color.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(x, element.y * screenHeight),
                        radius = element.size * 5
                    ),
                    radius = element.size * 5,
                    center = Offset(x, element.y * screenHeight)
                )
            } else {
                drawCircle(
                    color = element.color,
                    radius = element.size,
                    center = Offset(x, element.y * screenHeight)
                )
            }
        }
    }
}

private fun DrawScope.drawPlanet(offset: Float, totalWidth: Float, screenWidth: Float, screenHeight: Float) {
    val planetParallax = 0.3f
    val planetXRatio = 0.7f
    val planetYRatio = 0.3f
    val planetRadius = 150f
    
    val pOffset = (offset * planetParallax) % totalWidth
    var x = (planetXRatio * totalWidth - pOffset)
    
    if (x < -planetRadius * 2) x += totalWidth
    if (x > totalWidth) x -= totalWidth
    
    if (x > -planetRadius * 2 && x < screenWidth + planetRadius * 2) {
        // Simple planet with shadow
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF4B0082), Color(0xFF000033)),
                start = Offset(x - planetRadius, screenHeight * planetYRatio - planetRadius),
                end = Offset(x + planetRadius, screenHeight * planetYRatio + planetRadius)
            ),
            radius = planetRadius,
            center = Offset(x, screenHeight * planetYRatio)
        )
        // Atmosphere glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8A2BE2).copy(alpha = 0.3f), Color.Transparent),
                center = Offset(x, screenHeight * planetYRatio),
                radius = planetRadius * 1.2f
            ),
            radius = planetRadius * 1.2f,
            center = Offset(x, screenHeight * planetYRatio)
        )
    }
}

data class SpaceElement(
    val x: Float, // 0 to 1 ratio of totalWidth
    val y: Float, // 0 to 1 ratio of screenHeight
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
            parallax = Random.nextFloat() * 0.5f + 0.5f
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
            parallax = Random.nextFloat() * 0.3f + 0.2f
        )
    }
}
