package com.rork.neonhighwayracer.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.neonhighwayracer.game.GameRenderer
import com.rork.neonhighwayracer.game.GameState
import com.rork.neonhighwayracer.game.GameViewModel
import com.rork.neonhighwayracer.game.GraphicsPreset
import com.rork.neonhighwayracer.game.LANE_COUNT

@Composable
fun GameScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { GameViewModel() }
    val state by viewModel.uiState.collectAsState()
    var screenSize by remember { mutableStateOf(IntSize(1, 1)) }
    var lastFrameTime by remember { mutableFloatStateOf(0f) }
    var steerDirection by remember { mutableFloatStateOf(0f) }
    var showOverheatWarning by remember {
        mutableStateOf(!GraphicsPreset.isOverheatWarningShown(context))
    }

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    // Overheat warning dialog
    if (showOverheatWarning) {
        AlertDialog(
            onDismissRequest = {
                GraphicsPreset.markOverheatWarningShown(context)
                showOverheatWarning = false
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "ULTRA GRAPHICS MODE",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800),
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        "You are running ULTRA graphics preset.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠ Warning: Ultra mode may overheat your device during extended gameplay.",
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Features enabled:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        "• 8K Texture Resolution\n• Screen Space Reflections\n• Real-time Shadows\n• PBR Materials\n• Bloom + Motion Blur\n• Volumetric Clouds\n• Dynamic Weather\n• 4K Render | 60 FPS",
                        color = Color(0xFF4FC3F7),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        GraphicsPreset.markOverheatWarningShown(context)
                        showOverheatWarning = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text("I UNDERSTAND", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setGraphicsPreset(GraphicsPreset.HIGH, context)
                        GraphicsPreset.markOverheatWarningShown(context)
                        showOverheatWarning = false
                    }
                ) {
                    Text("Switch to HIGH", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF12122A),
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
            .onSizeChanged { screenSize = it }
    ) {
        // Game Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (state.gameState == GameState.GAME_OVER) {
                            viewModel.restartGame()
                        } else if (state.gameState == GameState.PLAYING) {
                            if (offset.x < size.width / 2f) {
                                viewModel.steerLeft()
                            } else {
                                viewModel.steerRight()
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { /* handled by tap */ },
                        onHorizontalDrag = { _, dragAmount ->
                            if (state.gameState == GameState.PLAYING) {
                                if (dragAmount > 10f) viewModel.steerRight()
                                else if (dragAmount < -10f) viewModel.steerLeft()
                            }
                        }
                    )
                }
        ) {
            val w = screenSize.width.toFloat()
            val h = screenSize.height.toFloat()
            GameRenderer.draw(this, state, w, h)
        }

        // Steer buttons (left/right zones)
        if (state.gameState == GameState.PLAYING) {
            // Left zone
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxSize(0.35f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.steerLeft()
                                tryAwaitRelease()
                            }
                        )
                    }
            )
            // Right zone
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxSize(0.35f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.steerRight()
                                tryAwaitRelease()
                            }
                        )
                    }
            )
        }

        // HUD - Top bar
        if (state.gameState == GameState.PLAYING || state.gameState == GameState.CRASHING) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Score
                    HudBox(
                        label = "SCORE",
                        value = state.score.toString(),
                        color = Color(0xFF00FFFF)
                    )

                    // Speed
                    HudBox(
                        label = "SPEED",
                        value = "${(state.speed * 30).toInt()} km/h",
                        color = Color(0xFFFF00FF)
                    )

                    // Coins
                    HudBox(
                        label = "COINS",
                        value = state.coinCount.toString(),
                        color = Color(0xFFFFD700)
                    )

                    // Ultra badge
                    if (state.graphicsPreset == GraphicsPreset.ULTRA) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFF9800).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "ULTRA",
                                color = Color(0xFFFF9800),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Weather indicator (ULTRA only)
                if (state.graphicsPreset == GraphicsPreset.ULTRA ||
                    state.graphicsPreset.dynamicWeather) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.weatherState.current.label.uppercase(),
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        // Game Over overlay
        if (state.gameState == GameState.GAME_OVER) {
            GameOverOverlay(
                score = state.score,
                coins = state.coinCount,
                highScore = state.highScore,
                onRestart = { viewModel.restartGame() },
                onMenu = {
                    viewModel.returnToMenu()
                    onBack()
                }
            )
        }

        // Game loop
        LaunchedEffect(state.gameState) {
            var lastTime = 0f
            while (state.gameState == GameState.PLAYING || state.gameState == GameState.CRASHING) {
                withFrameMillis { frameTimeMs ->
                    val now = frameTimeMs / 1000f
                    if (lastTime > 0f) {
                        val dt = (now - lastTime).coerceAtMost(0.05f)
                        viewModel.tick(dt)
                    }
                    lastTime = now
                }
            }
        }
    }
}

@Composable
private fun HudBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    coins: Int,
    highScore: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC050510)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CRASHED!",
                color = Color(0xFFFF5252),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                StatItem("SCORE", score.toString(), Color(0xFF00FFFF))
                StatItem("COINS", coins.toString(), Color(0xFFFFD700))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "BEST: $highScore",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRestart,
                modifier = Modifier
                    .width(180.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF)
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF050510)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "RESTART",
                    color = Color(0xFF050510),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onMenu,
                modifier = Modifier
                    .width(180.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    "MENU",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = value,
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
