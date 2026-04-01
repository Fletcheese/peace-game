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
private const val POINTS_PER_ENEMY = 100
private const val POINTS_PER_GATE = 500
private const val GATE_END_RADIUS = 12f
private const val SPAWN_CLUSTER_COUNT = 3
private const val SPAWN_CORNER_PADDING = 150f
private const val SPAWN_SPREAD_RADIUS = 60f
private const val MIN_SPAWN_DISTANCE_FROM_PLAYER = 300f
private const val SHARD_MAGNET_RADIUS = 125f
private const val SHARD_PICKUP_RADIUS = 40f

@Composable
fun InteractiveCircleScreen(onNavigateHome: () -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scoreManager = remember { ScoreManager(context) }
    val player = remember { GamePlayer(Offset.Zero, coroutineScope) }

    // --- Game State ---
    var isPaused by remember { mutableStateOf(false) }
    var enemies by remember { mutableStateOf<List<Enemy>>(emptyList()) }
    var gates by remember { mutableStateOf<List<Gate>>(emptyList()) }
    var explosions by remember { mutableStateOf<List<Explosion>>(emptyList()) }
    var shards by remember { mutableStateOf<List<Shard>>(emptyList()) }
    var score by remember { mutableStateOf(0) }
    var multiplier by remember { mutableStateOf(0) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var enemySpawnCountdown by remember { mutableStateOf(1.0f) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var hasBeenCentered by remember { mutableStateOf(false) }
    var gameOverMessage by remember { mutableStateOf("Game Over") }

    // --- Physics State ---
    var fingerPosition by remember { mutableStateOf<Offset?>(null) }
    var playerVelocity by remember { mutableStateOf(Offset.Zero) }

    // Difficulty Factor scales from 1.0 to 3.0 based on score
    val difficultyFactor by remember(score) {
        derivedStateOf { (1f + (score / 15000f)).coerceAtMost(3f) }
    }

    val resetGame = {
        enemies = emptyList()
        gates = emptyList()
        explosions = emptyList()
        shards = emptyList()
        score = 0
        multiplier = 0
        playerVelocity = Offset.Zero
        val center = Offset(screenSize.width / 2f, screenSize.height / 2f)
        coroutineScope.launch { player.snapTo(center) }
        enemySpawnCountdown = 1.0f
        showGameOverDialog = false
        isPaused = false
    }

    // THE PHYSICS LOOP
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos {
                if (showGameOverDialog || isPaused) return@withFrameNanos

                val target = fingerPosition
                if (target != null) {
                    val direction = target - player.position.value
                    val dist = direction.getDistance().coerceAtLeast(1f)
                    val accelMagnitude = 0.0035f * (1f + (100f / dist))
                    val acceleration = direction * accelMagnitude
                    playerVelocity += acceleration
                    // Lowered top speed / increased friction to 0.9
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

    // Spawning Logic
    LaunchedEffect(hasBeenCentered) {
        if (hasBeenCentered) {
            var enemyTime = 1000L 
            var gateTime = 2000L
            while (isActive) {
                delay(16)
                if (!showGameOverDialog && !isPaused) {
                    enemyTime -= 16
                    gateTime -= 16
                    
                    val currentEnemyInterval = (INITIAL_ENEMY_SPAWN_INTERVAL_MS / difficultyFactor).toLong().coerceAtLeast(MIN_ENEMY_SPAWN_INTERVAL_MS)
                    val currentGateInterval = (INITIAL_GATE_SPAWN_INTERVAL_MS / difficultyFactor).toLong().coerceAtLeast(MIN_GATE_SPAWN_INTERVAL_MS)

                    enemySpawnCountdown = (enemyTime.toFloat() / currentEnemyInterval.toFloat()).coerceIn(0f, 1f)

                    if (enemyTime <= 0) {
                        val playerPos = player.position.value
                        val predictedPos = playerPos + playerVelocity * 60f // Predicted pos in ~1s
                        
                        val corners = listOf(
                            Offset(SPAWN_CORNER_PADDING, SPAWN_CORNER_PADDING),
                            Offset(screenSize.width - SPAWN_CORNER_PADDING, SPAWN_CORNER_PADDING),
                            Offset(SPAWN_CORNER_PADDING, screenSize.height - SPAWN_CORNER_PADDING),
                            Offset(screenSize.width - SPAWN_CORNER_PADDING, screenSize.height - SPAWN_CORNER_PADDING)
                        ).filter { corner ->
                            val distToPlayer = sqrt((corner.x - playerPos.x).pow(2) + (corner.y - playerPos.y).pow(2))
                            val distToPredicted = sqrt((corner.x - predictedPos.x).pow(2) + (corner.y - predictedPos.y).pow(2))
                            distToPlayer > MIN_SPAWN_DISTANCE_FROM_PLAYER && distToPredicted > MIN_SPAWN_DISTANCE_FROM_PLAYER
                        }

                        if (corners.isNotEmpty()) {
                            val chosenCorner = corners.random()
                            val newCluster = mutableListOf<Enemy>()
                            for (i in 0 until SPAWN_CLUSTER_COUNT) {
                                val angle = (i * 120f) * (Math.PI.toFloat() / 180f)
                                val spawnPos = Offset(
                                    x = chosenCorner.x + cos(angle) * SPAWN_SPREAD_RADIUS,
                                    y = chosenCorner.y + sin(angle) * SPAWN_SPREAD_RADIUS
                                )
                                newCluster.add(Enemy(System.nanoTime() + i, spawnPos))
                            }
                            enemies = enemies + newCluster
                            enemyTime = currentEnemyInterval
                        }
                    }

                    if (gateTime <= 0) {
                        val playerPos = player.position.value
                        val predictedPos = playerPos + playerVelocity * 60f
                        
                        var newGate: Gate? = null
                        var attempts = 0
                        while (newGate == null && attempts < 5) {
                            val tempGate = createRandomGate(screenSize, 100f)
                            
                            val dStartPlayer = sqrt((tempGate.currentStart.x - playerPos.x).pow(2) + (tempGate.currentStart.y - playerPos.y).pow(2))
                            val dStartPredicted = sqrt((tempGate.currentStart.x - predictedPos.x).pow(2) + (tempGate.currentStart.y - predictedPos.y).pow(2))
                            
                            val dEndPlayer = sqrt((tempGate.currentEnd.x - playerPos.x).pow(2) + (tempGate.currentEnd.y - playerPos.y).pow(2))
                            val dEndPredicted = sqrt((tempGate.currentEnd.x - predictedPos.x).pow(2) + (tempGate.currentEnd.y - predictedPos.y).pow(2))
                            
                            if (dStartPlayer > MIN_SPAWN_DISTANCE_FROM_PLAYER && dStartPredicted > MIN_SPAWN_DISTANCE_FROM_PLAYER &&
                                dEndPlayer > MIN_SPAWN_DISTANCE_FROM_PLAYER && dEndPredicted > MIN_SPAWN_DISTANCE_FROM_PLAYER) {
                                newGate = tempGate
                            }
                            attempts++
                        }
                        
                        if (newGate != null) {
                            gates = gates + newGate
                            gateTime = currentGateInterval
                        }
                    }
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

        // Shard Collection
        val collectedShards = shards.filter { 
            (it.position.x - playerPos.x).pow(2) + (it.position.y - playerPos.y).pow(2) < SHARD_PICKUP_RADIUS.pow(2) 
        }
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

                // Apply Multiplier to score
                val totalPoints = ((dyingEnemies.size * POINTS_PER_ENEMY) + POINTS_PER_GATE) * (1 + multiplier)
                score += totalPoints
                
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
                    fingerPosition = down.position
                    do {
                        val event = awaitPointerEvent()
                        fingerPosition = event.changes.first().position
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    fingerPosition = null
                }
            }
    ) {
        SpaceBackground()

        player.Draw()
        enemies.forEach { it.Draw() }
        gates.forEach { it.Draw() }
        shards.forEach { it.Draw() }
        
        explosions.forEach { explosion ->
            key(explosion.id) {
                explosion.Animate { finishedExplosion ->
                    explosions = explosions.filter { it.id != finishedExplosion.id }
                }
            }
        }

        // Score display
        Text(
            text = "Score: $score", 
            modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 16.dp),
            color = Color.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold
        )
        
        // Multiplier display
        Text(
            text = "x$multiplier", 
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp),
            color = Color.Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold
        )

        // Adaptive Timer Bar & Pause Control
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { enemySpawnCountdown },
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.4f else 0.8f)
                    .height(4.dp)
                    .alpha(0.6f),
                color = Color.White, trackColor = Color.Gray, strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "||", // Pause icon
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { isPaused = true }
                    .padding(8.dp)
            )
        }

        // Pause Overlay
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isPaused = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶", // Play symbol
                        color = Color.White,
                        fontSize = 40.sp,
                        modifier = Modifier.padding(start = 4.dp) // Optical centering
                    )
                }
            }
        }
    }

    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("You Lose", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
            text = { 
                Column {
                    Text(gameOverMessage)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Final Score: $score (x$multiplier)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Green)
                }
            },
            confirmButton = { TextButton(onClick = { resetGame() }) { Text("Play Again") } },
            dismissButton = { TextButton(onClick = { onNavigateHome() }) { Text("Home") } }
        )
    }
}

class Shard(val id: Long, val anchorPosition: Offset) {
    var position by mutableStateOf(anchorPosition)
    private var velocity = Offset.Zero
    private var isMagnetized by mutableStateOf(false)
    private var time = Random.nextFloat() * 100f
    private val rotationOffset = Random.nextFloat() * 360f

    fun update(playerPos: Offset) {
        time += 0.02f
        val distToPlayer = sqrt((position.x - playerPos.x).pow(2) + (position.y - playerPos.y).pow(2))
        
        if (distToPlayer < SHARD_MAGNET_RADIUS) {
            isMagnetized = true
        }

        if (isMagnetized) {
            val direction = playerPos - position
            val distance = direction.getDistance().coerceAtLeast(1f)
            // Increased magnet acceleration from 0.4f to 0.9f
            val acceleration = (direction / distance) * 0.9f
            velocity += acceleration
            velocity *= 0.94f
            position += velocity
        } else {
            val driftX = sin(time) * 15f
            val driftY = cos(time * 0.7f) * 15f
            position = anchorPosition + Offset(driftX, driftY)
        }
    }

    @Composable
    fun Draw() {
        val infiniteTransition = rememberInfiniteTransition(label = "shardShine")
        val shineAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shine"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            rotate(rotationOffset + time * 10f, position) {
                drawRect(
                    color = Color.Gray.copy(alpha = shineAlpha * 0.8f),
                    topLeft = Offset(position.x - 6f, position.y - 12f),
                    size = Size(12f, 24f)
                )
                drawRect(
                    color = Color.White.copy(alpha = shineAlpha * 0.4f),
                    topLeft = Offset(position.x - 4f, position.y - 10f),
                    size = Size(4f, 20f)
                )
            }
        }
    }
}
