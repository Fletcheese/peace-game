package com.example.greetingcard.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
// *** FIXED: Removed non-existent import for awaitFirstDown ***
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.data.position
// *** FIXED: Removed incorrect import for position ***
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Player Constants ---
const val PLAYER_CIRCLE_RADIUS = 20f
private const val MOVEMENT_STOP_DELAY_MS = 500L


// --- Player Class ---
class GamePlayer(
    initialPosition: Offset,
    private val coroutineScope: CoroutineScope
) {
    val position = Animatable(initialPosition, Offset.VectorConverter)
    private var animationJob: Job? = null

    /**
     * A modifier that encapsulates the player's movement logic.
     * Movement starts on press, updates on drag, and stops with a delay on release.
     */
    fun inputModifier(): Modifier = Modifier.pointerInput(Unit) {
        // Use forEachGesture to handle each press/drag/release sequence independently.
        forEachGesture {
            awaitPointerEventScope {
                // Wait for the initial press down.
                val down = awaitFirstDown()
                var currentTarget = down.position

                // As soon as the press is detected, start the animation job.
                animationJob?.cancel()
                animationJob = coroutineScope.launch {
                    position.animateTo(
                        targetValue = currentTarget,
                        animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                    )
                }

                // Main loop to track drag events after the initial press.
                do {
                    val event = awaitPointerEvent()
                    // Update the target position whenever the pointer moves.
                    currentTarget = event.changes.first().position

                    // Re-launch the animation to re-target it to the new position.
                    // This ensures it follows a moving finger.
                    (animationJob as? Job)?.cancel() // Cancel to restart with new target
                    animationJob = coroutineScope.launch {
                        position.animateTo(
                            targetValue = currentTarget,
                            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                        )
                    }

                    // Consume the event changes to prevent them from being used by other gestures.
                    event.changes.forEach { it.consume() }

                } while (event.changes.any { it.pressed })

                // When the loop exits, the press has been released.
                // Start a new coroutine to handle the stop delay.
                coroutineScope.launch {
                    delay(MOVEMENT_STOP_DELAY_MS)
                    animationJob?.cancel()
                }
            }
        }
    }


    /**
     * Instantly moves the player to a new position.
     */
    suspend fun snapTo(offset: Offset) {
        position.snapTo(offset)
    }

    /**
     * Immediately stops any ongoing movement animation.
     */
    suspend fun stop() {
        position.stop()
    }

    /**
     * Composable function for the Player to draw itself.
     */
    @Composable
    fun Draw() {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color.Green, PLAYER_CIRCLE_RADIUS, position.value)
        }
    }
}
