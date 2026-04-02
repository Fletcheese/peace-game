package com.fletcher.peace

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

// --- Game Constants ---
private const val INITIAL_ENEMY_SPAWN_INTERVAL_MS = 3000L
private const val INITIAL_GATE_SPAWN_INTERVAL_MS = 7000L
private const val MIN_ENEMY_SPAWN_INTERVAL_MS = 800L
private const val MIN_GATE_SPAWN_INTERVAL_MS = 2000L
private const val WARNING_DURATION_MS = 1000L
private const val POINTS_PER_ENEMY = 100
private const val POINTS_PER_GATE = 500
private const val GATE_END_RADIUS = 12f
private const val SPAWN_CLUSTER_COUNT = 3
private const val SPAWN_CORNER_PADDING = 150f
private const val SPAWN_SPREAD_RADIUS = 60f
private const val MIN_SPAWN_DISTANCE_FROM_PLAYER = 300f
private const val SHARD_PICKUP_RADIUS = 40f

fun Offset.distanceTo(other: Offset) = sqrt((this.x - other.x).pow(2) + (this.y - other.y).pow(2))

@Composable
fun InteractiveCircleScreen(onNavigateHome: () -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scoreManager = remember { ScoreManager(context) }
    val isJoystickEnabled = remember { scoreManager.isJoystickEnabled() }
    val shipSpeedMultiplier = remember { scoreManager.getShipSpeed() }
    val player = remember { GamePlayer(Offset.Zero, coroutineScope) }

    // --- Game State ---
    var isPaused by remember { mutableStateOf(false) }
    var enemies by remember { mutableStateOf<List<Enemy>>(emptyList()) }
    var gates by remember { mutableStateOf<List<Gate>>(emptyList()) }
    var upcomingEnemies by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var upcomingEnemyCorner by remember { mutableStateOf<Offset?>(null) }
    var upcomingGates by remember { mutableStateOf<List<Gate>>(emptyList()) }
    var explosions by remember { mutableStateOf<List<Explosion>>(emptyList()) }
    var shards by remember { mutableStateOf<List<Shard>>(emptyList()) }
    var score by remember { mutableStateOf(0) }
    var multiplier by remember { mutableStateOf(0) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var enemyTimeRemaining by remember { mutableLongStateOf(0L) }
    var currentEnemySpawnInterval by remember { mutableLongStateOf(INITIAL_ENEMY_SPAWN_INTERVAL_MS) }
    var gateTimeRemaining by remember { mutableLongStateOf(0L) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var hasBeenCentered by remember { mutableStateOf(false) }
    var gameOverMessage by remember { mutableStateOf("Game Over") }

    // --- Physics State ---
    var touchAnchor by remember { mutableStateOf<Offset?>(null) }
    var fingerPosition by remember { mutableStateOf<Offset?>(null) }
    var playerVelocity by remember { mutableStateOf(Offset.Zero) }

    val difficultyFactor by remember(score) {
        derivedStateOf { (1f + (score / 15000f)).coerceAtMost(3f) }
    }

    val resetGame = {
        enemies = emptyList()
        gates = emptyList()
        upcomingEnemies = emptyList()
        upcomingEnemyCorner = null
        upcomingGates = emptyList()
        explosions = emptyList()
        shards = emptyList()
        score = 0
        multiplier = 0
        playerVelocity = Offset.Zero
        val center = Offset(screenSize.width / 2f, screenSize.height / 2f)
        coroutineScope.launch { player.snapTo(center) }
        showGameOverDialog = false
        isPaused = false
    }

    // THE PHYSICS LOOP
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos {
                if (showGameOverDialog || isPaused) return@withFrameNanos
                val target = fingerPosition
                val anchor = touchAnchor
                if (target != null) {
                    val acceleration = if (isJoystickEnabled && anchor != null) {
                        val vector = target - anchor
                        val dist = vector.getDistance().coerceAtLeast(1f)
                        (vector / dist) * (dist * 0.015f * shipSpeedMultiplier).coerceAtMost(3.0f)
                    } else {
                        val direction = target - player.position.value
                        val dist = direction.getDistance().coerceAtLeast(1f)
                        val accelMagnitude = 0.0035f * (1f + (100f / dist)) * shipSpeedMultiplier
                        direction * accelMagnitude
                    }
                    playerVelocity += acceleration
                    playerVelocity *= 0.9f
                } else {
                    playerVelocity *= 0.95f
                    if (playerVelocity.getDistance() < 0.1f) playerVelocity = Offset.Zero
                }
                if (playerVelocity != Offset.Zero) {
                    val newPos = player.position.value + playerVelocity
                    coroutineScope.launch { 
                        player.updateRotation(playerVelocity)
                        player.position.snapTo(newPos) 
                    }
                }
            }
        }
    }

    // Spawning Logic with Dynamic Warnings
    LaunchedEffect(hasBeenCentered) {
        if (hasBeenCentered) {
            var enemyTime = INITIAL_ENEMY_SPAWN_INTERVAL_MS
            var gateTime = INITIAL_GATE_SPAWN_INTERVAL_MS
            while (isActive) {
                delay(16)
                if (!showGameOverDialog && !isPaused) {
                    enemyTime -= 16
                    gateTime -= 16
                    enemyTimeRemaining = enemyTime
                    gateTimeRemaining = gateTime
                    
                    val currentEnemyInterval = (INITIAL_ENEMY_SPAWN_INTERVAL_MS / difficultyFactor).toLong().coerceAtLeast(MIN_ENEMY_SPAWN_INTERVAL_MS)
                    val currentGateInterval = (INITIAL_GATE_SPAWN_INTERVAL_MS / difficultyFactor).toLong().coerceAtLeast(MIN_GATE_SPAWN_INTERVAL_MS)

                    // Enemy Spawning & Glow Logic
                    if (upcomingEnemies.isEmpty()) {
                        val playerPos = player.position.value
                        val predictedPos = playerPos + playerVelocity * 60f
                        val corners = listOf(
                            Offset(SPAWN_CORNER_PADDING, SPAWN_CORNER_PADDING),
                            Offset(screenSize.width - SPAWN_CORNER_PADDING, SPAWN_CORNER_PADDING),
                            Offset(SPAWN_CORNER_PADDING, screenSize.height - SPAWN_CORNER_PADDING),
                            Offset(screenSize.width - SPAWN_CORNER_PADDING, screenSize.height - SPAWN_CORNER_PADDING)
                        ).filter { corner ->
                            corner.distanceTo(playerPos) > MIN_SPAWN_DISTANCE_FROM_PLAYER && 
                            corner.distanceTo(predictedPos) > MIN_SPAWN_DISTANCE_FROM_PLAYER
                        }
                        if (corners.isNotEmpty()) {
                            val chosenCorner = corners.random()
                            upcomingEnemyCorner = chosenCorner
                            currentEnemySpawnInterval = currentEnemyInterval
                            upcomingEnemies = listOf(List(SPAWN_CLUSTER_COUNT) { i ->
                                val angle = (i * 120f) * (Math.PI.toFloat() / 180f)
                                Offset(chosenCorner.x + cos(angle) * SPAWN_SPREAD_RADIUS, chosenCorner.y + sin(angle) * SPAWN_SPREAD_RADIUS)
                            })
                        }
                    }

                    if (enemyTime <= 0) {
                        if (upcomingEnemies.isNotEmpty()) {
                            val cluster = upcomingEnemies.first()
                            enemies = enemies + cluster.mapIndexed { i, pos -> Enemy(System.nanoTime() + i, pos) }
                            upcomingEnemies = emptyList()
                            upcomingEnemyCorner = null
                        }
                        enemyTime = currentEnemyInterval
                    }

                    // Gate Spawning Logic
                    if (gateTime <= WARNING_DURATION_MS && gateTime > 0 && upcomingGates.isEmpty()) {
                        val playerPos = player.position.value
                        val predictedPos = playerPos + playerVelocity * 60f
                        var attempts = 0
                        while (attempts < 5) {
                            val tempGate = createRandomGate(screenSize, 100f)
                            if (tempGate.currentStart.distanceTo(playerPos) > MIN_SPAWN_DISTANCE_FROM_PLAYER &&
                                tempGate.currentStart.distanceTo(predictedPos) > MIN_SPAWN_DISTANCE_FROM_PLAYER &&
                                tempGate.currentEnd.distanceTo(playerPos) > MIN_SPAWN_DISTANCE_FROM_PLAYER &&
                                tempGate.currentEnd.distanceTo(predictedPos) > MIN_SPAWN_DISTANCE_FROM_PLAYER) {
                                upcomingGates = listOf(tempGate)
                                break
                            }
                            attempts++
                        }
                    }

                    if (gateTime <= 0) {
                        if (upcomingGates.isNotEmpty()) {
                            gates = gates + upcomingGates.first()
                            upcomingGates = emptyList()
                        }
                        gateTime = currentGateInterval
                    }
                }
            }
        }
    }

    // Entity Update Loop
    LaunchedEffect(hasBeenCentered) {
        if (hasBeenCentered) {
            while (isActive) {
                delay(16)
                if (!showGameOverDialog && !isPaused) {
                    gates.forEach { it.update() }
                    enemies.forEach { it.moveTowards(player.position.value) }
                    shards.forEach { it.update(player.position.value) }
                }
            }
        }
    }

    // Collision Detection
    LaunchedEffect(player.position.value, enemies, gates, shards) {
        if (!hasBeenCentered || showGameOverDialog || isPaused) return@LaunchedEffect
        val playerPos = player.position.value
        if (enemies.any { it.checkCollision(playerPos, PLAYER_CIRCLE_RADIUS) }) {
            gameOverMessage = "Ran into an enemy!"
            scoreManager.saveScore(score, multiplier)
            showGameOverDialog = true
            playerVelocity = Offset.Zero
            return@LaunchedEffect
        }
        val collectedShards = shards.filter { (it.position.x - playerPos.x).pow(2) + (it.position.y - playerPos.y).pow(2) < SHARD_PICKUP_RADIUS.pow(2) }
        if (collectedShards.isNotEmpty()) {
            multiplier += collectedShards.size
            shards = shards - collectedShards.toSet()
        }
        val gatesToRemove = mutableListOf<Gate>()
        for (gate in gates) {
            if (gate.checkLethalCollision(playerPos, PLAYER_CIRCLE_RADIUS)) {
                gameOverMessage = "Hit the end of a gate!"
                scoreManager.saveScore(score, multiplier)
                showGameOverDialog = true
                playerVelocity = Offset.Zero
                return@LaunchedEffect
            }
            if (gate.checkForActivation(playerPos, PLAYER_CIRCLE_RADIUS)) {
                val gateExplosions = listOf(
                    Explosion(System.nanoTime(), gate.currentStart, Color.White, initialRadius = GATE_END_RADIUS),
                    Explosion(System.nanoTime() + 1, gate.currentEnd, Color.White, initialRadius = GATE_END_RADIUS)
                )
                val explosionRadiusSquared = GATE_EXPLOSION_RADIUS.pow(2)
                val enemyParticles = mutableListOf<Explosion>()
                val newShards = mutableListOf<Shard>()
                val dyingEnemies = mutableSetOf<Enemy>()
                enemies.forEach { enemy ->
                    val d1 = (enemy.position.x - gate.currentStart.x).pow(2) + (enemy.position.y - gate.currentStart.y).pow(2)
                    val d2 = (enemy.position.x - gate.currentEnd.x).pow(2) + (enemy.position.y - gate.currentEnd.y).pow(2)
                    if (d1 <= explosionRadiusSquared || d2 <= explosionRadiusSquared) {
                        dyingEnemies.add(enemy)
                        enemyParticles.add(Explosion(System.nanoTime() + enemy.id, enemy.position, Color.Red, 5f, 100f))
                        newShards.add(Shard(System.nanoTime() + enemy.id, enemy.position))
                    }
                }
                explosions = explosions + gateExplosions + enemyParticles
                shards = shards + newShards
                score += ((dyingEnemies.size * POINTS_PER_ENEMY) + POINTS_PER_GATE) * (1 + multiplier)
                enemies = enemies.filter { it !in dyingEnemies }
                gatesToRemove.add(gate)
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
            .pointerInput(Unit) {
                if (showGameOverDialog || isPaused) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchAnchor = down.position
                    fingerPosition = down.position
                    do {
                        val event = awaitPointerEvent()
                        fingerPosition = event.changes.first().position
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    fingerPosition = null
                    touchAnchor = null
                }
            }
    ) {
        SpaceBackground()

        // 1. Spawning Warnings
        val infiniteTransition = rememberInfiniteTransition(label = "warning")
        val flickerAlpha by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 0.4f,
            animationSpec = infiniteRepeatable(animation = tween(250, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "alpha"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Corner Glow for upcoming enemies (Timer starts as soon as previous wave spawns)
            if (upcomingEnemyCorner != null) {
                val glowIntensity = (1f - (enemyTimeRemaining.toFloat() / currentEnemySpawnInterval)).coerceIn(0f, 1f)
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Red.copy(alpha = glowIntensity * 0.5f), Color.Transparent),
                        center = upcomingEnemyCorner!!,
                        radius = 400f * glowIntensity
                    ),
                    radius = 400f * glowIntensity,
                    center = upcomingEnemyCorner!!
                )
            }

            // Flickering Previews (1s before spawn)
            if (enemyTimeRemaining <= WARNING_DURATION_MS) {
                upcomingEnemies.flatten().forEach { pos ->
                    drawCircle(Color.Red, radius = 15f, center = pos, alpha = flickerAlpha)
                }
            }
            if (gateTimeRemaining <= WARNING_DURATION_MS) {
                upcomingGates.forEach { gate ->
                    drawLine(
                        color = Color(0xFFFFA500).copy(alpha = flickerAlpha),
                        start = gate.currentStart, end = gate.currentEnd,
                        strokeWidth = 30f, cap = StrokeCap.Round
                    )
                }
            }
        }

        // 2. Active Entities
        player.Draw()
        enemies.forEach { it.Draw() }
        gates.forEach { it.Draw() }
        shards.forEach { it.Draw() }
        explosions.forEach { explosion -> key(explosion.id) { explosion.Animate { ex -> explosions = explosions.filter { it.id != ex.id } } } }

        // UI Overlays
        Text(text = "Score: $score", modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 16.dp), color = Color.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = "x$multiplier", modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp), color = Color.Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = "||", color = Color.White.copy(alpha = 0.4f), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp).clickable { isPaused = true }.padding(8.dp))

        if (isPaused) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { isPaused = false }, contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Text(text = "▶", color = Color.White, fontSize = 40.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }

    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("You Lose", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
            text = { Column { Text(gameOverMessage); Spacer(modifier = Modifier.height(8.dp)); Text("Final Score: $score (x$multiplier)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Green) } },
            confirmButton = { TextButton(onClick = { resetGame() }) { Text("Play Again") } },
            dismissButton = { TextButton(onClick = { onNavigateHome() }) { Text("Home") } }
        )
    }
}
