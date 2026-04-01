package com.fletcher.peace

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fletcher.peace.ui.theme.GreetingCardTheme

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
                            HomeScreen(
                                navController = navController,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        composable("circleScreen") {
                            InteractiveCircleScreen(onNavigateHome = {
                                navController.popBackStack("greeting", inclusive = false)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scoreManager = remember { ScoreManager(context) }
    val highScores = remember { scoreManager.getHighScores() }
    val lastGame = remember { scoreManager.getLastScore() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { navController.navigate("circleScreen") }
    ) {
        SpaceBackground()

        // Shared text styles for consistency
        val labelStyle = TextStyle(
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        val scoreValueStyle = TextStyle(
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        // Last Game Score Section
        if (lastGame != null) {
            val lastGameModifier = if (isLandscape) {
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            }
            Column(
                modifier = lastGameModifier,
                horizontalAlignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Text(text = "LAST GAME", style = labelStyle)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${lastGame.score} (x${lastGame.multiplier})",
                    style = scoreValueStyle
                )
            }
        }

        // Glowing "Tap to Start" UI
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

        // High Scores Section
        if (highScores.isNotEmpty()) {
            val highScoresModifier = if (isLandscape) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
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
                        style = scoreValueStyle
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GreetingCardTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            HomeScreen(navController = rememberNavController())
        }
    }
}
