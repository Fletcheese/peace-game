package com.fletcheese.peace

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random

// --- Game Constants ---
private const val WORLD_SCALE = 1.5f 
private const val SPAWN_CORNER_PADDING = 100f
private const val SPAWN_SPREAD_RADIUS = 60f
private const val MIN_SPAWN_DISTANCE_FROM_PLAYER = 500f
private const val SHARD_PICKUP_RADIUS = 45f

fun Offset.distanceTo(other: Offset) = sqrt((this.x - other.x).pow(2) + (this.y - other.y).pow(2))

@Composable
fun InteractiveCircleScreen(onNavigateHome: () -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scoreManager = remember { ScoreManager(context) }
    val modManager = remember { ModManager(context) }
    val activeProfile = remember { modManager.getActiveProfile() }
    
    val isJoystickEnabled = remember { scoreManager.isJoystickEnabled() }
    val shipSpeedMultiplier = remember { scoreManager.getShipSpeed() }
    val player = remember { GamePlayer(Offset.Zero) }

    // --- Game State ---
    var isPaused by remember { mutableStateOf(false) }
    var enemies by remember { mutableStateOf<List<Enemy>>(emptyList()) }
    var gates by remember { mutableStateOf<List<Gate>>(emptyList()) }
    var upcomingEnemies by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var upcomingEnemyCorner by remember { mutableStateOf<Offset?>(null) }
    var upcomingGates by remember { mutableStateOf<List<Gate>>(emptyList()) }
    val explosions = remember { mutableStateListOf<Explosion>() }
    var shards by remember { mutableStateOf<List<Shard>>(emptyList()) }
    var score by remember { mutableIntStateOf(0) }
    var multiplier by remember { mutableIntStateOf(0) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var enemyTimeRemaining by remember { mutableLongStateOf(0L) }
    var currentEnemySpawnInterval by remember { mutableLongStateOf(3000L) }
    var gateTimeRemaining by remember { mutableLongStateOf(0L) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var hasBeenCentered by remember { mutableStateOf(false) }
    var gameOverMessage by remember { mutableStateOf("Game Over") }

    val cameraOffset by remember {
        derivedStateOf {
            if (screenSize == IntSize.Zero) Offset.Zero else {
                val rawX = player.position.x - screenSize.width / 2f
                val rawY = player.position.y - screenSize.height / 2f
                val worldWidth = screenSize.width * WORLD_SCALE
                val worldHeight = screenSize.height * WORLD_SCALE
                Offset(
                    rawX.coerceIn(0f, (worldWidth - screenSize.width).coerceAtLeast(0f)),
                    rawY.coerceIn(0f, (worldHeight - screenSize.height).coerceAtLeast(0f))
                )
            }
        }
    }

    var touchAnchor by remember { mutableStateOf<Offset?>(null) }
    var fingerPosition by remember { mutableStateOf<Offset?>(null) }
    var playerVelocity by remember { mutableStateOf(Offset.Zero) }

    val difficultyFactor by remember(multiplier) {
        derivedStateOf { (1f + (multiplier / 150f)).coerceAtMost(4f) }
    }

    val resetGame = {
        enemies = emptyList()
        gates = emptyList()
        upcomingEnemies = emptyList()
        upcomingEnemyCorner = null
        upcomingGates = emptyList()
        explosions.clear()
        shards = emptyList()
        score = 0
        multiplier = 0
        playerVelocity = Offset(Random.nextFloat() * 2f - 1f, Random.nextFloat() * 2f - 1f) * 2f
        val worldWidth = screenSize.width * WORLD_SCALE
        val worldHeight = screenSize.height * WORLD_SCALE
        player.snapTo(Offset(worldWidth / 2f, worldHeight / 2f))
        showGameOverDialog = false
        isPaused = false
    }

    LaunchedEffect(hasBeenCentered) {
        if (!hasBeenCentered) return@LaunchedEffect
        var enemyTime = 3000L
        var gateTime = 7000L 
        
        while (isActive) {
            withFrameNanos {
                if (showGameOverDialog || isPaused || screenSize == IntSize.Zero) return@withFrameNanos
                
                try {
                    val target = fingerPosition
                    val anchor = touchAnchor
                    if (target != null) {
                        val acceleration = if (isJoystickEnabled && anchor != null) {
                            val vector = target - anchor
                            val dist = vector.getDistance().coerceAtLeast(1f)
                            (vector / dist) * (dist * 0.015f * shipSpeedMultiplier * activeProfile.playerAcceleration / 0.0035f).coerceAtMost(activeProfile.playerMaxSpeed / 4f)
                        } else {
                            val targetWorld = target + cameraOffset
                            val direction = targetWorld - player.position
                            val dist = direction.getDistance().coerceAtLeast(1f)
                            val accelMagnitude = activeProfile.playerAcceleration * (1f + (100f / dist)) * shipSpeedMultiplier
                            direction * accelMagnitude
                        }
                        playerVelocity += acceleration
                        playerVelocity *= 0.9f
                    } else {
                        playerVelocity *= 0.95f
                        if (playerVelocity.getDistance() < 0.1f) playerVelocity = Offset.Zero
                    }
                    
                    if (playerVelocity != Offset.Zero) {
                        val worldWidth = screenSize.width * WORLD_SCALE
                        val worldHeight = screenSize.height * WORLD_SCALE
                        val newPos = (player.position + playerVelocity).let {
                            Offset(it.x.coerceIn(0f, worldWidth), it.y.coerceIn(0f, worldHeight))
                        }
                        player.updateRotation(playerVelocity)
                        player.position = newPos
                    }

                    enemyTime -= 16; gateTime -= 16
                    enemyTimeRemaining = enemyTime; gateTimeRemaining = gateTime
                    
                    val currentEnemyInterval = (3000L / (difficultyFactor * activeProfile.enemySpawnRateMod)).toLong().coerceAtLeast(800L)
                    val currentGateInterval = (7000L / (difficultyFactor * 0.8f * activeProfile.gateSpawnRateMod)).toLong().coerceAtLeast(2000L) 

                    if (upcomingEnemies.isEmpty()) {
                        val worldWidth = screenSize.width * WORLD_SCALE
                        val worldHeight = screenSize.height * WORLD_SCALE
                        val corners = listOf(
                            Offset(SPAWN_CORNER_PADDING, SPAWN_CORNER_PADDING),
                            Offset(worldWidth - SPAWN_CORNER_PADDING, SPAWN_CORNER_PADDING),
                            Offset(SPAWN_CORNER_PADDING, worldHeight - SPAWN_CORNER_PADDING),
                            Offset(worldWidth - SPAWN_CORNER_PADDING, worldHeight - SPAWN_CORNER_PADDING)
                        ).filter { it.distanceTo(player.position) > MIN_SPAWN_DISTANCE_FROM_PLAYER }
                        
                        if (corners.isNotEmpty()) {
                            val chosenCorner = corners.random()
                            upcomingEnemyCorner = chosenCorner
                            currentEnemySpawnInterval = currentEnemyInterval
                            upcomingEnemies = listOf(List(activeProfile.enemySpawnCount) { i ->
                                val angle = (i * (360f / activeProfile.enemySpawnCount)) * (Math.PI.toFloat() / 180f)
                                Offset(chosenCorner.x + cos(angle) * SPAWN_SPREAD_RADIUS, chosenCorner.y + sin(angle) * SPAWN_SPREAD_RADIUS)
                            })
                        }
                    }

                    if (enemyTime <= 0) {
                        if (upcomingEnemies.isNotEmpty()) {
                            enemies = enemies + upcomingEnemies.first().mapIndexed { i, pos -> 
                                Enemy(System.nanoTime() + i, pos, activeProfile.enemySize, activeProfile.enemyMaxSpeed, Color(activeProfile.enemyColor)) 
                            }
                            upcomingEnemies = emptyList(); upcomingEnemyCorner = null
                        }
                        enemyTime = currentEnemyInterval
                    }

                    if (gateTime <= 1000L && upcomingGates.isEmpty()) {
                        val worldSize = IntSize((screenSize.width * WORLD_SCALE).toInt(), (screenSize.height * WORLD_SCALE).toInt())
                        val tempGate = createRandomGate(worldSize, 100f, activeProfile.gateLength, activeProfile.gateEndZoneSize)
                        if (tempGate.currentStart.distanceTo(player.position) > MIN_SPAWN_DISTANCE_FROM_PLAYER) {
                            upcomingGates = listOf(tempGate)
                        }
                    }

                    if (gateTime <= 0) {
                        if (upcomingGates.isNotEmpty()) {
                            gates = gates + upcomingGates.first(); upcomingGates = emptyList()
                        }
                        gateTime = currentGateInterval
                    }

                    val playerPos = player.position
                    gates.forEach { it.update(activeProfile.gateAcceleration, activeProfile.gateMaxSpeed) }
                    enemies.forEach { it.moveTowards(playerPos, enemies) }
                    shards.forEach { it.update(playerPos) }
                    shards = shards.filter { !it.isExpired }

                    if (enemies.any { it.checkCollision(playerPos, activeProfile.playerSize) }) {
                        gameOverMessage = "Ran into an enemy!"; scoreManager.saveScore(score, multiplier)
                        showGameOverDialog = true; playerVelocity = Offset.Zero
                    }

                    val collected = shards.filter { it.position.distanceTo(playerPos) < SHARD_PICKUP_RADIUS }
                    if (collected.isNotEmpty()) { multiplier += collected.size; shards = shards - collected.toSet() }

                    val gatesToRemove = mutableListOf<Gate>()
                    for (gate in gates) {
                        if (gate.checkLethalCollision(playerPos, activeProfile.playerSize)) {
                            gameOverMessage = "Hit the end of a gate!"; scoreManager.saveScore(score, multiplier)
                            showGameOverDialog = true; playerVelocity = Offset.Zero
                        }
                        if (gate.checkForActivation(playerPos, activeProfile.playerSize)) {
                            explosions.addAll(listOf(
                                Explosion(System.nanoTime(), gate.currentStart, Color.White, initialRadius = activeProfile.gateEndZoneSize / 2f, targetRadius = activeProfile.gateExplosionRadius),
                                Explosion(System.nanoTime() + 1, gate.currentEnd, Color.White, initialRadius = activeProfile.gateEndZoneSize / 2f, targetRadius = activeProfile.gateExplosionRadius)
                            ))
                            val dying = enemies.filter { e -> e.position.distanceTo(gate.currentStart) < activeProfile.gateExplosionRadius || e.position.distanceTo(gate.currentEnd) < activeProfile.gateExplosionRadius }
                            dying.forEach { e -> 
                                explosions.add(Explosion(System.nanoTime() + e.id, e.position, Color.Red, 5f, 100f))
                                shards = shards + Shard(System.nanoTime() + e.id, e.position)
                            }
                            score += ((dying.size * 100) + 500) * (1 + multiplier)
                            enemies = enemies.filter { it !in dying }; gatesToRemove.add(gate)
                        }
                    }
                    gates = gates - gatesToRemove.toSet()
                } catch (e: Exception) {
                    Log.e("GameLoop", "Error in loop", e)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .onSizeChanged { size ->
                if (!hasBeenCentered && size != IntSize.Zero) {
                    screenSize = size
                    val worldWidth = size.width * WORLD_SCALE
                    val worldHeight = size.height * WORLD_SCALE
                    player.snapTo(Offset(worldWidth / 2f, worldHeight / 2f))
                    playerVelocity = Offset(Random.nextFloat() * 2f - 1f, Random.nextFloat() * 2f - 1f) * 2f
                    hasBeenCentered = true
                }
            }
            .pointerInput(Unit) {
                if (showGameOverDialog || isPaused) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchAnchor = down.position; fingerPosition = down.position
                    do {
                        val event = awaitPointerEvent()
                        fingerPosition = event.changes.first().position
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    fingerPosition = null; touchAnchor = null
                }
            }
    ) {
        SpaceBackground(cameraOffset = cameraOffset)

        Box(modifier = Modifier.fillMaxSize().graphicsLayer {
            translationX = -cameraOffset.x
            translationY = -cameraOffset.y
        }) {
            val flickerAlpha by rememberInfiniteTransition(label="").animateFloat(0f, 0.4f, infiniteRepeatable(tween(250), RepeatMode.Reverse), label="")
            
            player.Draw(activeProfile.playerSize) 
            enemies.forEach { it.Draw() }
            gates.forEach { it.Draw(Color(activeProfile.gateColor)) }
            shards.forEach { it.Draw() }
            explosions.toList().forEach { explosion -> key(explosion.id) { explosion.Animate { explosions.remove(it) } } }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            upcomingEnemyCorner?.let { corner ->
                val glowIntensity = (1f - (enemyTimeRemaining.toFloat() / currentEnemySpawnInterval)).coerceIn(0f, 1f)
                val screenPadding = 50f
                val clampedX = (corner.x - cameraOffset.x).coerceIn(screenPadding, size.width - screenPadding)
                val clampedY = (corner.y - cameraOffset.y).coerceIn(screenPadding, size.height - screenPadding)
                val indicatorPos = Offset(clampedX, clampedY)
                drawCircle(Brush.radialGradient(listOf(Color(activeProfile.enemyColor).copy(alpha = glowIntensity * 0.5f), Color.Transparent), center = indicatorPos, radius = (400f * glowIntensity).coerceAtLeast(1f)), radius = (400f * glowIntensity).coerceAtLeast(1f), center = indicatorPos)
            }
        }

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

        if (showGameOverDialog) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)) // 25% Dim
                    .clickable { /* Block input */ },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF151525).copy(alpha = 0.4f)) // Highly transparent card
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("You Lose", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(gameOverMessage, color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Final Score: $score (x$multiplier)", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Green)
                    Spacer(modifier = Modifier.height(32.dp))
                    Row {
                        TextButton(onClick = { resetGame() }) { Text("Play Again", color = Color.Cyan, fontSize = 18.sp) }
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(onClick = { onNavigateHome() }) { Text("Home", color = Color.Cyan, fontSize = 18.sp) }
                    }
                }
            }
        }
    }
}
