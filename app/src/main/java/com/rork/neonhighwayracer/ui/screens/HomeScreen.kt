package com.rork.neonhighwayracer.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.neonhighwayracer.game.CarColor
import com.rork.neonhighwayracer.game.CarModel
import com.rork.neonhighwayracer.game.GameViewModel
import com.rork.neonhighwayracer.game.GraphicsPreset

@Composable
fun HomeScreen(
    onStartGame: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { GameViewModel() }
    val state by viewModel.uiState.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "road_anim")
    val roadOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "road_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        // Animated background road
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizon = h * 0.35f

            // Sky
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF050510), Color(0xFF0D0D2B), Color(0xFF1A1040)),
                    startY = 0f,
                    endY = horizon
                ),
                size = size
            )

            // Road strips
            val stripCount = 40
            for (i in 0 until stripCount) {
                val t = (i.toFloat() + (roadOffset % 10f) / 10f) / stripCount
                if (t <= 0f || t >= 1f) continue
                val tSq = t * t
                val y = horizon + (h - horizon) * tSq * t
                val roadW = 40f + tSq * w * 0.4f
                val centerX = w / 2f

                val path = Path().apply {
                    moveTo(centerX - roadW, y)
                    lineTo(centerX + roadW, y)
                    lineTo(centerX + roadW + 10f, y + 3f)
                    lineTo(centerX - roadW - 10f, y + 3f)
                    close()
                }
                val alpha = 0.1f + t * 0.3f
                drawPath(path, Color(0xFF00FFFF).copy(alpha = alpha))
            }

            // Lane center line
            for (i in 0 until 30) {
                val t = (i.toFloat() + (roadOffset % 6f) / 6f) / 30
                val tSq = t * t
                val y = horizon + (h - horizon) * tSq * t
                val roadW = 40f + tSq * w * 0.4f
                if (i % 2 == 0) {
                    drawCircle(
                        color = Color(0xFF00FFFF).copy(alpha = 0.3f + t * 0.4f),
                        radius = 1.5f,
                        center = Offset(w / 2f, y)
                    )
                }
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = "NEON",
                color = Color(0xFF00FFFF),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 12.sp
            )
            Text(
                text = "HIGHWAY RACER",
                color = Color(0xFFFF00FF),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )

            // Ultra badge
            if (state.graphicsPreset == GraphicsPreset.ULTRA) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFF9800).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        "ULTRA GRAPHICS",
                        color = Color(0xFFFF9800),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // High score
            if (state.highScore > 0) {
                Text(
                    text = "BEST SCORE: ${state.highScore}",
                    color = Color(0xFFFFD700).copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Graphics Preset Selection
            Text(
                text = "GRAPHICS PRESET",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                GraphicsPreset.entries.forEach { preset ->
                    val isSelected = preset == state.graphicsPreset
                    val presetColor = when (preset) {
                        GraphicsPreset.ULTRA -> Color(0xFFFF9800)
                        GraphicsPreset.HIGH -> Color(0xFF4CAF50)
                        GraphicsPreset.MEDIUM -> Color(0xFF2196F3)
                        GraphicsPreset.LOW -> Color(0xFF9E9E9E)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) presetColor.copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .then(
                                if (isSelected) Modifier.border(
                                    1.dp, presetColor, RoundedCornerShape(10.dp)
                                ) else Modifier
                            )
                            .clickable { viewModel.setGraphicsPreset(preset, context) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                preset.label,
                                color = if (isSelected) presetColor else Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "${preset.targetFps}FPS",
                                color = (if (isSelected) presetColor else Color.White).copy(alpha = 0.4f),
                                fontSize = 7.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Car Model Selection
            Text(
                text = "CHOOSE YOUR RIDE",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(CarModel.entries.toList()) { index, model ->
                    val isSelected = model == state.selectedCarModel
                    CarModelChip(
                        model = model,
                        selected = isSelected,
                        onClick = { viewModel.selectCar(model) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Car Color Selection
            Text(
                text = "PAINT COLOR",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(CarColor.ALL) { index, color ->
                    val isSelected = color == state.selectedCarColor
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color.bodyColor)
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    color.neonColor,
                                    CircleShape
                                ) else Modifier
                            )
                            .clickable { viewModel.selectColor(color) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Car Preview
            Canvas(
                modifier = Modifier
                    .size(width = 120.dp, height = 80.dp)
            ) {
                val cw = size.width
                val ch = size.height
                val cx = cw / 2f
                val cy = ch / 2f
                val bw = 48f
                val bh = 80f
                val scale = minOf(cw / bw, ch / bh) * 0.7f
                val scaledW = bw * scale
                val scaledH = bh * scale

                // Under glow
                drawOval(
                    color = state.selectedCarColor.neonColor.copy(alpha = 0.4f),
                    topLeft = Offset(cx - scaledW * 0.8f, cy + scaledH * 0.3f),
                    size = androidx.compose.ui.geometry.Size(scaledW * 1.6f, scaledH * 0.2f)
                )

                // Car body
                val carPath = getCarPreviewPath(state.selectedCarModel, cx, cy - scaledH * 0.15f, scaledW, scaledH)
                drawPath(carPath, state.selectedCarColor.bodyColor)
                drawPath(
                    carPath,
                    color = state.selectedCarColor.neonColor,
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Windows
                val winPath = getCarWindowPreview(state.selectedCarModel, cx, cy - scaledH * 0.15f, scaledW, scaledH)
                drawPath(winPath, Color(0xFF1A1A2E).copy(alpha = 0.8f))
                drawPath(
                    winPath,
                    color = Color(0xFF4488FF).copy(alpha = 0.3f),
                    style = Stroke(width = 1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Controls hint
            Text(
                text = "TAP LEFT / RIGHT TO STEER",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            Text(
                text = "DODGE TRAFFIC · COLLECT COINS",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 9.sp,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Start Button
            Button(
                onClick = {
                    viewModel.startGame()
                    onStartGame()
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF)
                )
            ) {
                Text(
                    text = "START RACE",
                    color = Color(0xFF050510),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CarModelChip(
    model: CarModel,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) Color(0xFF00E5FF).copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f)
            )
            .then(
                if (selected) Modifier.border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = model.displayName.uppercase(),
            color = if (selected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 1.sp
        )
    }
}

// Simplified car path for preview
private fun getCarPreviewPath(model: CarModel, x: Float, y: Float, w: Float, h: Float): Path {
    val hw = w / 2f
    return when (model) {
        CarModel.SPORTS, CarModel.SUPERCAR, CarModel.RACER -> Path().apply {
            moveTo(x - hw * 0.85f, y + h * 0.55f)
            lineTo(x - hw * 0.7f, y + h * 0.1f)
            lineTo(x - hw * 0.3f, y - h * 0.05f)
            lineTo(x + hw * 0.3f, y - h * 0.05f)
            lineTo(x + hw * 0.7f, y + h * 0.1f)
            lineTo(x + hw * 0.85f, y + h * 0.65f)
            lineTo(x + hw * 0.8f, y + h * 0.9f)
            lineTo(x - hw * 0.8f, y + h * 0.9f)
            close()
        }
        CarModel.TRUCK, CarModel.PICKUP -> Path().apply {
            moveTo(x - hw * 1.05f, y + h * 0.45f)
            lineTo(x - hw * 1f, y + h * 0.08f)
            lineTo(x - hw * 0.2f, y + h * 0.05f)
            lineTo(x + hw * 0.4f, y + h * 0.05f)
            lineTo(x + hw * 0.95f, y + h * 0.12f)
            lineTo(x + hw * 1f, y + h * 0.55f)
            lineTo(x + hw * 1f, y + h * 0.9f)
            lineTo(x - hw * 1f, y + h * 0.9f)
            close()
        }
        CarModel.LIMO -> Path().apply {
            moveTo(x - hw * 1.1f, y + h * 0.55f)
            lineTo(x - hw * 1.05f, y + h * 0.12f)
            lineTo(x + hw * 0.9f, y + h * 0.1f)
            lineTo(x + hw * 1.05f, y + h * 0.18f)
            lineTo(x + hw * 1.05f, y + h * 0.65f)
            lineTo(x - hw * 1.05f, y + h * 0.65f)
            close()
        }
        else -> Path().apply {
            moveTo(x - hw * 0.95f, y + h * 0.55f)
            lineTo(x - hw * 0.85f, y + h * 0.12f)
            lineTo(x - hw * 0.35f, y + h * 0.03f)
            lineTo(x + hw * 0.35f, y + h * 0.03f)
            lineTo(x + hw * 0.85f, y + h * 0.15f)
            lineTo(x + hw * 0.95f, y + h * 0.65f)
            lineTo(x + hw * 0.9f, y + h * 0.9f)
            lineTo(x - hw * 0.9f, y + h * 0.9f)
            close()
        }
    }
}

private fun getCarWindowPreview(model: CarModel, x: Float, y: Float, w: Float, h: Float): Path {
    val hw = w / 2f
    return Path().apply {
        moveTo(x - hw * 0.35f, y + h * 0.1f)
        lineTo(x - hw * 0.1f, y + h * 0.04f)
        lineTo(x + hw * 0.2f, y + h * 0.04f)
        lineTo(x + hw * 0.35f, y + h * 0.15f)
        lineTo(x + hw * 0.35f, y + h * 0.35f)
        lineTo(x - hw * 0.35f, y + h * 0.35f)
        close()
    }
}
