package com.fletcheese.peace

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fletcheese.peace.ui.theme.GreetingCardTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreetingCardTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "greeting",
                    ) {
                        composable("greeting") {
                            LaunchedEffect(Unit) { setImmersiveMode(false) }
                            HomeScreen(
                                navController = navController,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        composable("circleScreen") {
                            LaunchedEffect(Unit) { setImmersiveMode(true) }
                            InteractiveCircleScreen(onNavigateHome = {
                                setImmersiveMode(false)
                                navController.popBackStack("greeting", inclusive = false)
                            })
                        }
                    }
                }
            }
        }
    }

    private fun setImmersiveMode(enable: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = if (enable) {
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
        
        if (enable) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val scoreManager = remember { ScoreManager(context) }
    val modManager = remember { ModManager(context) }
    val highScores = remember { scoreManager.getHighScores() }
    val lastGame = remember { scoreManager.getLastScore() }
    val uriHandler = LocalUriHandler.current
    
    var showModDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { navController.navigate("circleScreen") }
    ) {
        SpaceBackground()

        IconButton(
            onClick = { showInfoDialog = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("ⓘ", color = Color.White, fontSize = 32.sp)
        }

        IconButton(
            onClick = { showModDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("⚙", color = Color.White, fontSize = 32.sp)
        }

        val labelStyle = TextStyle(
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        val valueStyle = TextStyle(
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        if (lastGame != null) {
            val lastGameModifier = if (isLandscape) {
                Modifier.align(Alignment.CenterStart).padding(start = 48.dp)
            } else {
                Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
            }
            Column(
                modifier = lastGameModifier,
                horizontalAlignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Text(text = "LAST GAME", style = labelStyle)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${lastGame.score} (x${lastGame.multiplier})",
                    style = valueStyle
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Text(
                text = "we come in...",
                color = Color.Gray.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            )

            val rainbowBrush = Brush.sweepGradient(
                colors = listOf(
                    Color.Red, Color.Magenta, Color.Blue,
                    Color.Cyan, Color.Green, Color.Yellow, Color.Red
                )
            )

            Text(
                text = "PEACE",
                style = TextStyle(
                    brush = rainbowBrush,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Cyan.copy(alpha = alpha * 0.4f), Color.Transparent),
                            center = center,
                            radius = size.width
                        ),
                        radius = size.width,
                        center = center
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "tap to play",
                color = Color.White.copy(alpha = alpha),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
        }

        if (highScores.isNotEmpty()) {
            val highScoresModifier = if (isLandscape) {
                Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
            } else {
                Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
            }
            Column(
                modifier = highScoresModifier,
                horizontalAlignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Text(text = "HIGH SCORES", style = labelStyle)
                Spacer(modifier = Modifier.height(8.dp))
                highScores.forEachIndexed { index, gameScore ->
                    Text(
                        text = "${index + 1}. ${gameScore.score} (x${gameScore.multiplier})",
                        style = valueStyle
                    )
                }
            }
        }
    }

    if (showModDialog) {
        ModDialog(
            modManager = modManager,
            onDismiss = { showModDialog = false }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = Color(0xFF151525),
            text = {
                Text(
                    text = "Enjoying Peace? Click here to find more by Fletcheese",
                    color = Color.Cyan,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/fletcheese") }
                        .padding(16.dp)
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("No thanks", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModDialog(
    modManager: ModManager,
    onDismiss: () -> Unit
) {
    val profiles = remember { mutableStateListOf<ModProfile>().apply { addAll(modManager.getProfiles()) } }
    var selectedProfileId by remember { mutableStateOf(modManager.getSelectedProfileId()) }
    
    val currentProfile by remember(selectedProfileId) {
        derivedStateOf { profiles.find { it.id == selectedProfileId } ?: profiles[0] }
    }
    
    val context = LocalContext.current

    val updateProfile: (ModProfile) -> Unit = { updated ->
        val index = profiles.indexOfFirst { it.id == selectedProfileId }
        if (index != -1) {
            profiles[index] = updated
            modManager.saveProfiles(profiles.filter { it.isEditable })
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151525),
        title = { Text("Mod Profiles", color = Color.White) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = currentProfile.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Profile") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.label) },
                                    onClick = {
                                        selectedProfileId = profile.id
                                        modManager.setSelectedProfileId(profile.id)
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Create Custom", color = Color.Cyan) },
                                onClick = {
                                    val new = modManager.createCustomProfile()
                                    profiles.add(new)
                                    selectedProfileId = new.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (currentProfile.isEditable) {
                    item {
                        var localLabel by remember(currentProfile.id) { mutableStateOf(currentProfile.label) }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = localLabel,
                            onValueChange = { localLabel = it },
                            modifier = Modifier.fillMaxWidth().onFocusChanged { 
                                if (!it.isFocused) {
                                    updateProfile(currentProfile.copy(label = localLabel))
                                }
                            },
                            label = { Text("Profile Name") }
                        )
                    }
                }

                item {
                    Text(
                        text = "These settings have had limited testing so things may break, but have fun with it!",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    SectionHeader("Enemy")
                    ColorSelector("Enemy Color", currentProfile.enemyColor, currentProfile.isEditable) { colorInt ->
                        updateProfile(currentProfile.copy(enemyColor = colorInt))
                    }
                    ModSlider("Max Speed", currentProfile.enemyMaxSpeed, 0.5f, 2.0f, ModManager.DefaultValues.ENEMY_SPEED, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(enemyMaxSpeed = it))
                    }
                    ModSlider("Acceleration", currentProfile.enemyAcceleration, 0.5f, 2.0f, ModManager.DefaultValues.ENEMY_ACCEL, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(enemyAcceleration = it))
                    }
                    ModSlider("Size", currentProfile.enemySize, 0.5f, 2.0f, ModManager.DefaultValues.ENEMY_SIZE, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(enemySize = it))
                    }
                    ModSlider("Spawn Rate", currentProfile.enemySpawnRateMod, 0.5f, 2.0f, 1.0f, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(enemySpawnRateMod = it))
                    }
                    ModSlider("Cluster Count", currentProfile.enemySpawnCount.toFloat(), 1f, 10f, ModManager.DefaultValues.ENEMY_COUNT, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(enemySpawnCount = it.toInt()))
                    }

                    SectionHeader("Player")
                    ModSlider("Max Speed", currentProfile.playerMaxSpeed, 0.5f, 2.0f, ModManager.DefaultValues.PLAYER_SPEED, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(playerMaxSpeed = it))
                    }
                    ModSlider("Acceleration", currentProfile.playerAcceleration, 0.5f, 2.0f, ModManager.DefaultValues.PLAYER_ACCEL, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(playerAcceleration = it))
                    }
                    ModSlider("Size", currentProfile.playerSize, 0.5f, 2.0f, ModManager.DefaultValues.PLAYER_SIZE, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(playerSize = it))
                    }

                    SectionHeader("Gate")
                    ColorSelector("Gate Color", currentProfile.gateColor, currentProfile.isEditable) { colorInt ->
                        updateProfile(currentProfile.copy(gateColor = colorInt))
                    }
                    ModSlider("Max Speed", currentProfile.gateMaxSpeed, 0.5f, 2.0f, ModManager.DefaultValues.GATE_SPEED, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(gateMaxSpeed = it))
                    }
                    ModSlider("Acceleration", currentProfile.gateAcceleration, 0.5f, 2.0f, ModManager.DefaultValues.GATE_ACCEL, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(gateAcceleration = it))
                    }
                    ModSlider("Length", currentProfile.gateLength, 0.5f, 2.0f, ModManager.DefaultValues.GATE_LENGTH, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(gateLength = it))
                    }
                    ModSlider("End Zone Size", currentProfile.gateEndZoneSize, 0.5f, 2.0f, ModManager.DefaultValues.GATE_END_ZONE, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(gateEndZoneSize = it))
                    }
                    ModSlider("Explosion Radius", currentProfile.gateExplosionRadius, 0.5f, 2.0f, ModManager.DefaultValues.GATE_EXPLOSION, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(gateExplosionRadius = it))
                    }
                    ModSlider("Spawn Rate", currentProfile.gateSpawnRateMod, 0.5f, 2.0f, 1.0f, currentProfile.isEditable) { 
                        updateProfile(currentProfile.copy(gateSpawnRateMod = it))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = {
                            val jsonString = Json.encodeToString(currentProfile)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Peace Mod", jsonString))
                            Toast.makeText(context, "Profile copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) { Text("Export", color = Color.Cyan) }
                        
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val data = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                            if (data != null) {
                                try {
                                    val imported = Json.decodeFromString<ModProfile>(data)
                                    val existingIndex = profiles.indexOfFirst { it.id == imported.id }
                                    if (existingIndex != -1) {
                                        val toUpdate = imported.copy(isEditable = true)
                                        profiles[existingIndex] = toUpdate
                                        modManager.setSelectedProfileId(toUpdate.id)
                                        selectedProfileId = toUpdate.id
                                        Toast.makeText(context, "${toUpdate.label} updated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val toAdd = imported.copy(id = UUID.randomUUID().toString(), isEditable = true)
                                        profiles.add(toAdd)
                                        modManager.setSelectedProfileId(toAdd.id)
                                        selectedProfileId = toAdd.id
                                        Toast.makeText(context, "${toAdd.label} imported", Toast.LENGTH_SHORT).show()
                                    }
                                    modManager.saveProfiles(profiles.filter { it.isEditable })
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid profile data", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) { Text("Import", color = Color.Cyan) }
                        
                        if (currentProfile.isEditable) {
                            TextButton(onClick = {
                                modManager.deleteProfile(currentProfile.id)
                                profiles.removeIf { it.id == currentProfile.id }
                                selectedProfileId = "standard"
                            }) { Text("Delete", color = Color.Red) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.Cyan) }
        }
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Cyan,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ModSlider(label: String, currentValue: Float, minRel: Float, maxRel: Float, defaultValue: Float, enabled: Boolean, onValueChange: (Float) -> Unit) {
    var relValue by remember(currentValue, defaultValue) { mutableStateOf(currentValue / defaultValue) }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        val displayVal = if (label == "Cluster Count") currentValue.toInt().toString() else "${"%.2f".format(relValue)}x"
        Text(text = "$label: $displayVal", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Slider(
            value = relValue,
            onValueChange = { 
                relValue = it
                onValueChange(it * defaultValue)
            },
            valueRange = minRel..maxRel,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.Cyan,
                activeTrackColor = Color.Cyan,
                disabledThumbColor = Color.Gray,
                disabledActiveTrackColor = Color.Gray
            )
        )
    }
}

@Composable
fun ColorSelector(label: String, selectedColorArgb: Int, enabled: Boolean, onColorChange: (Int) -> Unit) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.Yellow, Color.White, Color(0xFFFFA500))
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            colors.forEach { c ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (selectedColorArgb == c.toArgb()) 2.dp else 0.dp, Color.White, CircleShape)
                        .clickable(enabled = enabled) { onColorChange(c.toArgb()) }
                )
            }
        }
    }
}
