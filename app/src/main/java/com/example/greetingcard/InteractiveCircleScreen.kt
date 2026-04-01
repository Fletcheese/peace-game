package com.example.greetingcard.ui.games

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.tooling.data.position

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// --- Game Constants ---
private const val ENEMY_SPAWN_INTERVAL_MS = 3000L
private const val GATE_SPAWN_INTERVAL_MS = 7000L
private const val POINTS_PER_ENEMY = 100
private const val POINTS_PER_GATE = 500
private const val MOVEMENT_STOP_DELAY_MS = 300L
private const val GATE_END_RADIUS = 12f

// --- Spawning Constants ---
private const val SPAWN_CLUSTER_COUNT = 3
private const val SPAWN_CORNER_PADDING = 150f // Distance from the actual screen edge
private const val SPAWN_SPREAD_RADIUS = 60f   // How far apart the 3 enemies are from each other

@Composable
fun InteractiveCircleScreen(onNavigateHome: () -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    val player = remember { GamePlayer(Offset.Zero, coroutineScope) }

    // --- Game State ---
    var enemies by remember { mutableStateOf<List<Enemy>>(emptyList()) }
    var gates by remember { mutableStateOf<List<Gate>>(emptyList()) }
    var explosions by remember { mutableStateOf<List<Explosion>>(emptyList()) }
    var score by remember { mutableStateOf(0) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var enemySpawnCountdown by remember { mutableStateOf(1.0f) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var hasBeenCentered by remember { mutableStateOf(false) }
    var gameOverMessage by remember { mutableStateOf("You collided with an object.") }

    // --- Physics State ---
    var fingerPosition by remember { mutableStateOf<Offset?>(null) }
    var playerVelocity by remember { mutableStateOf(Offset.Zero) }

    // Helper to reset the entire game state
    val resetGame = {
        enemies = emptyList()
        gates = emptyList()
        explosions = emptyList()
        score = 0
        playerVelocity = Offset.Zero
        val center = Offset(screenSize.width / 2f, screenSize.height / 2f)
        coroutineScope.launch { player.snapTo(center) }
        enemySpawnCountdown = 1.0f
        showGameOverDialog = false
    }

    // THE PHYSICS LOOP
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos {
                val target = fingerPosition
                if (target != null && !showGameOverDialog) {
                    val direction = target - player.position.value
                    val acceleration = direction * 0.0016f
                    playerVelocity += acceleration
                    playerVelocity *= 0.94f
                } else {
                    playerVelocity *= 0.95f
                    if (playerVelocity.getDistance() < 0.1f) playerVelocity = Offset.Zero
                }

                if (playerVelocity != Offset.Zero) {
                    val newPos = player.position.value + playerVelocity
                    launch { player.position.snapTo(newPos) }
                }
            }
        }
    }

    // Enemy/Gate Spawning
    LaunchedEffect(hasBeenCentered) {
        if (hasBeenCentered) {
            var enemyTime = ENEMY_SPAWN_INTERVAL_MS
            var gateTime = GATE_SPAWN_INTERVAL_MS
            while (isActive) {
                delay(16)
                if (!showGameOverDialog) {
                    enemyTime -= 16
                    gateTime -= 16
                    enemySpawnCountdown = enemyTime / ENEMY_SPAWN_INTERVAL_MS.toFloat()

                    if (enemyTime <= 0) {
                        // 1. Define the 4 corners
                        val corners = listOf(
                            Offset(SPAWN_CORNER_PADDING, SPAWN_CORNER_PADDING), // Top Left
                            Offset(screenSize.width - SPAWN_CORNER_PADDING, SPAWN_CORNER_PADDING), // Top Right
                            Offset(SPAWN_CORNER_PADDING, screenSize.height - SPAWN_CORNER_PADDING), // Bottom Left
                            Offset(screenSize.width - SPAWN_CORNER_PADDING, screenSize.height - SPAWN_CORNER_PADDING) // Bottom Right
                        )

                        // 2. Randomly select one corner
                        val chosenCorner = corners.random()

                        // 3. Create 3 enemies in a triangle formation around that corner
                        val newCluster = mutableListOf<Enemy>()
                        for (i in 0 until SPAWN_CLUSTER_COUNT) {
                            // Angle them at 0, 120, and 240 degrees for equal spread
                            val angle = (i * 120f) * (Math.PI.toFloat() / 180f)
                            val spawnPos = Offset(
                                x = chosenCorner.x + cos(angle) * SPAWN_SPREAD_RADIUS,
                                y = chosenCorner.y + sin(angle) * SPAWN_SPREAD_RADIUS
                            )
                            newCluster.add(Enemy(System.currentTimeMillis() + i, spawnPos))
                        }

                        enemies = enemies + newCluster
                        enemyTime = ENEMY_SPAWN_INTERVAL_MS
                    }

                    if (gateTime <= 0) {
                        gates = gates + createRandomGate(screenSize, 100f)
                        gateTime = GATE_SPAWN_INTERVAL_MS
                    }
                }
            }
        }
    }

    // ... inside InteractiveCircleScreen.kt ...

// Update the Gate Movement logic
    LaunchedEffect(hasBeenCentered) {
        if (hasBeenCentered) {
            while (isActive) {
                delay(16)
                if (!showGameOverDialog) {
                    gates.forEach { it.update() } // Move gates
                    enemies.forEach { it.moveTowards(player.position.value) }
                }
            }
        }
    }

    // Collision Detection
    LaunchedEffect(player.position.value, enemies, gates) {
        if (!hasBeenCentered || showGameOverDialog) return@LaunchedEffect
        val playerPos = player.position.value

        val collidingEnemy = enemies.firstOrNull { it.checkCollision(playerPos, PLAYER_CIRCLE_RADIUS) }
        if (collidingEnemy != null) {
            gameOverMessage = "You ran into an enemy!"
            showGameOverDialog = true
            playerVelocity = Offset.Zero
            return@LaunchedEffect
        }

        val gatesToRemove = mutableListOf<Gate>()
        for (gate in gates) {
            if (gate.checkLethalCollision(playerPos, PLAYER_CIRCLE_RADIUS)) {
                gameOverMessage = "You hit the end of a gate!"
                showGameOverDialog = true
                playerVelocity = Offset.Zero
                return@LaunchedEffect
            }

            // Update Gate Activation inside the Collision LaunchedEffect
            if (gate.checkForActivation(playerPos, PLAYER_CIRCLE_RADIUS)) {
                // 1. White Gate Explosions
                val newExplosions = mutableListOf<Explosion>(
                    Explosion(System.currentTimeMillis(), gate.currentStart, Color.White, initialRadius = GATE_END_RADIUS),
                    Explosion(System.currentTimeMillis() + 1, gate.currentEnd, Color.White, initialRadius = GATE_END_RADIUS)
                )

                val explosionRadiusSquared = 120f.pow(2)

                // 2. Identify dying enemies and create Red Particles for them
                enemies.forEach { enemy ->
                    val d1 = (enemy.position.x - gate.currentStart.x).pow(2) + (enemy.position.y - gate.currentStart.y).pow(2)
                    val d2 = (enemy.position.x - gate.currentEnd.x).pow(2) + (enemy.position.y - gate.currentEnd.y).pow(2)

                    if (d1 <= explosionRadiusSquared || d2 <= explosionRadiusSquared) {
                        newExplosions.add(
                            Explosion(
                                id = System.nanoTime() + enemy.id,
                                center = enemy.position,
                                color = Color.Red,
                                initialRadius = 5f,
                                targetRadius = 100f
                            )
                        )
                    }
                }

                explosions = explosions + newExplosions
                // ... filter enemies and update score logic remains the same ...
            }
        }
        gates = gates - gatesToRemove.toSet()
    }

    // UI Composition
    Box(
        modifier = Modifier.fillMaxSize()
            .onSizeChanged { size ->
                if (!hasBeenCentered) {
                    screenSize = size
                    coroutineScope.launch { player.snapTo(Offset(size.width / 2f, size.height / 2f)) }
                    hasBeenCentered = true
                }
            }
            .background(Color.Black) // Sets background to Black
            .pointerInput(Unit) {
                if (showGameOverDialog) return@pointerInput
                forEachGesture {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fingerPosition = down.position
                        do {
                            val event = awaitPointerEvent()
                            fingerPosition = event.changes.first().position
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        fingerPosition = null
                    }
                }
            }
    ) {
        player.Draw()
        enemies.forEach { it.Draw() }
        gates.forEach { it.Draw() }
        explosions.forEach { it.Animate { ex -> explosions = explosions - ex } }

        Text("Score: $score", modifier = Modifier.align(Alignment.TopStart).padding(16.dp), color = Color.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        LinearProgressIndicator(
            progress = { enemySpawnCountdown },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(12.dp).align(Alignment.TopCenter),
            color = Color.White, trackColor = Color.Gray, strokeCap = StrokeCap.Round
        )
    }

    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("You Lose", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
            text = { Text(gameOverMessage) },
            confirmButton = {
                TextButton(onClick = { resetGame() }) { Text("Play Again") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGameOverDialog = false
                    onNavigateHome()
                }) { Text("Home") }
            }
        )
    }
}