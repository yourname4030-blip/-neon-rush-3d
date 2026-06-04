package com.rork.neonhighwayracer.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.neonhighwayracer.game.GraphicsPreset
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AssetDownloadScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadSpeed by remember { mutableStateOf("0 MB/s") }
    var currentFile by remember { mutableStateOf("Initializing...") }
    var isComplete by remember { mutableStateOf(false) }
    var totalDownloaded by remember { mutableFloatStateOf(0f) }
    val totalSize = 250f // MB

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Simulate download
    LaunchedEffect(Unit) {
        val files = listOf(
            "ultra_texture_pack.8k" to 80f,
            "pbr_materials.dat" to 45f,
            "car_models_hq.3d" to 35f,
            "volumetric_clouds.dat" to 25f,
            "weather_effects.pak" to 20f,
            "ssr_shaders.glsl" to 15f,
            "bloom_pipeline.dat" to 12f,
            "shadow_maps.dat" to 10f,
            "audio_engine.dat" to 5f,
            "finalizing.dat" to 3f
        )

        for ((file, size) in files) {
            currentFile = file
            val chunkCount = 20
            val chunkSize = size / chunkCount
            for (i in 0 until chunkCount) {
                delay((30 + i * 2).toLong())
                val chunkProgress = (i + 1).toFloat() / chunkCount
                val fileProgress = chunkProgress * (size / totalSize)
                totalDownloaded += chunkSize
                progress = (totalDownloaded / totalSize).coerceAtMost(1f)
                val mbps = (chunkSize * chunkCount * 2f).roundToInt()
                downloadSpeed = "$mbps MB/s"
            }
        }

        isComplete = true
        currentFile = "Ultra assets installed successfully"
        downloadSpeed = "250 MB"
        GraphicsPreset.setAssetsDownloaded(context, true)
        GraphicsPreset.markFirstLaunchDone(context)
        delay(1500)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                if (isComplete) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                contentDescription = null,
                tint = if (isComplete) Color(0xFF4CAF50) else Color(0xFFFF9800).copy(alpha = glowAlpha),
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isComplete) "ASSETS INSTALLED" else "DOWNLOADING ULTRA ASSETS",
                color = if (isComplete) Color(0xFF4CAF50) else Color(0xFFFF9800),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "250 MB Ultra HD Asset Pack",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isComplete) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    trackColor = Color.Transparent
                )

                // Glow effect
                if (!isComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFFF9800).copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * totalSize).roundToInt()} MB / ${totalSize.roundToInt()} MB",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = downloadSpeed,
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Current file being downloaded
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (isComplete) Icons.Default.CheckCircle else Icons.Default.Downloading,
                        contentDescription = null,
                        tint = if (isComplete) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = currentFile,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Asset details
            if (!isComplete) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AssetDetailChip("8K", "Textures")
                    AssetDetailChip("PBR", "Materials")
                    AssetDetailChip("SSR", "Shaders")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AssetDetailChip("4K", "Render")
                    AssetDetailChip("60", "FPS")
                    AssetDetailChip("ULTRA", "Ready")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (isComplete) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        "CONTINUE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp
                    )
                }
            } else {
                Text(
                    text = "Please wait while assets are being downloaded...",
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AssetDetailChip(label: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color(0xFFFF9800),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 8.sp,
            letterSpacing = 1.sp
        )
    }
}
