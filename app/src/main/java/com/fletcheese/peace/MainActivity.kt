package com.fletcheese.peace

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
        
        val scoreManager = ScoreManager(this)
        
        // Initialize Sound Systems with saved volumes
        MusicManager.start(this, R.raw.bg_music)
        MusicManager.setVolume(scoreManager.getMusicVolume())
        
        SoundManager.init(this)
        SoundManager.setVolume(scoreManager.getSoundVolume())
        
        // Pre-load sound effects
        SoundManager.load(this, R.raw.gate_explode)
        SoundManager.load(this, R.raw.shard_pickup)

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

    override fun onResume() {
        super.onResume()
        MusicManager.start(this, R.raw.bg_music)
    }

    override fun onPause() {
        super.onPause()
        MusicManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicManager.stop()
        SoundManager.release()
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
    var isTutorialCompleted by remember(showInfoDialog) { mutableStateOf(scoreManager.isTutorialCompleted()) }

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
            scoreManager = scoreManager,
            onDismiss = { showModDialog = false }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = Color(0xFF151525),
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isTutorialCompleted = !isTutorialCompleted
                                scoreManager.setTutorialCompleted(isTutorialCompleted)
                            }
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = isTutorialCompleted,
                            onCheckedChange = {
                                isTutorialCompleted = it
                                scoreManager.setTutorialCompleted(it)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Cyan,
                                uncheckedColor = Color.White.copy(alpha = 0.6f),
                                checkmarkColor = Color.Black
                            )
                        )
                        Column {
                            Text("Tutorial Completed", color = Color.White, fontSize = 16.sp)
                            Text("Untick to repeat it", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val rainbowBrush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Red, Color.Magenta, Color.Blue,
                            Color.Cyan, Color.Green, Color.Yellow, Color.Red
                        )
                    )
                    Text(
                        text = "Enjoying PEACE?",
                        style = TextStyle(
                            brush = rainbowBrush,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri("https://github.com/fletcheese") }
                            .padding(8.dp)
                    ) {
                        Text("🔗 ", fontSize = 18.sp)
                        Text(
                            text = "Find more by\nme on Github",
                            color = Color.Cyan,
                            fontSize = 16.sp,
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModDialog(
    modManager: ModManager,
    scoreManager: ScoreManager,
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
        title = { Text("Settings", color = Color.White) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    SectionHeader("Audio")
                    var musicVol by remember { mutableStateOf(scoreManager.getMusicVolume()) }
                    var soundVol by remember { mutableStateOf(scoreManager.getSoundVolume()) }

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = "Music Volume: ${if (musicVol == 0f) "Muted" else "${(musicVol * 100).toInt()}%"}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Slider(
                            value = musicVol,
                            onValueChange = { 
                                musicVol = it
                                scoreManager.setMusicVolume(it)
                                MusicManager.setVolume(it)
                            },
                            colors = SliderDefaults.colors(thumbColor = Color.Cyan, activeTrackColor = Color.Cyan)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(text = "Sound Effects: ${if (soundVol == 0f) "Muted" else "${(soundVol * 100).toInt()}%"}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Slider(
                            value = soundVol,
                            onValueChange = { 
                                soundVol = it
                                scoreManager.setSoundVolume(it)
                                SoundManager.setVolume(it)
                            },
                            colors = SliderDefaults.colors(thumbColor = Color.Cyan, activeTrackColor = Color.Cyan)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    SectionHeader("Mod Profiles")
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = currentProfile.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active Profile") },
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

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val jsonString = Json.encodeToString(currentProfile)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Peace Mod", jsonString))
                                Toast.makeText(context, "Profile copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Cyan)
                        ) {
                            Text("Export")
                        }

                        OutlinedButton(
                            onClick = {
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
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Cyan)
                        ) {
                            Text("Import")
                        }
                    }
                }

                item {
                    Box {
                        Column {
                            if (currentProfile.isEditable) {
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

                            Text(
                                text = "These settings have had limited testing so things may break, but have fun with it!",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

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

                        if (!currentProfile.isEditable) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        val toast = Toast.makeText(context, "Create custom profile to modify ${currentProfile.label}", Toast.LENGTH_SHORT)
                                        toast.setGravity(Gravity.TOP, 0, 100)
                                        toast.show()
                                    }
                            )
                        }
                    }
                }

                item {
                    if (currentProfile.isEditable) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(onClick = {
                                modManager.deleteProfile(currentProfile.id)
                                profiles.removeIf { it.id == currentProfile.id }
                                selectedProfileId = "standard"
                            }) { Text("Delete Profile", color = Color.Red) }
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
