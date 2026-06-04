package com.rork.neonhighwayracer.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object GameRenderer {

    private val roadBaseColor = Color(0xFF1A1A2E)
    private val roadLineColor = Color(0xFF00FFFF)
    private val roadEdgeColor = Color(0xFFFF00FF)
    private val laneMarkerColor = Color(0x4488FFFF)
    private val coinColor = Color(0xFFFFD700)
    private val coinGlowColor = Color(0x80FFD700)
    private val crashFlashColor = Color(0x40FF0000)

    private const val HORIZON_RATIO = 0.38f
    private const val FOCAL_LENGTH = 1200f
    private const val CAMERA_HEIGHT = 200f
    private const val ROAD_HALF_WIDTH = 280f

    fun draw(
        drawScope: DrawScope,
        state: GameUiState,
        screenWidth: Float,
        screenHeight: Float
    ) {
        with(drawScope) {
            val horizon = screenHeight * HORIZON_RATIO
            val preset = state.graphicsPreset

            // Sky with volumetric clouds (ULTRA/HIGH)
            drawSky(state, horizon, screenWidth, screenHeight)

            // Weather effects behind road
            if (preset.dynamicWeather || preset == GraphicsPreset.ULTRA) {
                drawWeatherBackground(state, horizon, screenWidth, screenHeight)
            }

            // Road surface with SSR reflections
            drawRoad(state, horizon, screenWidth, screenHeight)

            // SSR road reflections
            if (preset.ssrEnabled) {
                drawSSRReflections(state, horizon, screenWidth, screenHeight)
            }

            // Lane markings
            drawLaneMarkings(state, horizon, screenWidth, screenHeight)

            // Motion blur trails
            if (preset.motionBlurEnabled) {
                drawMotionTrails(state, horizon, screenWidth, screenHeight)
            }

            // Coins with bloom
            drawCoins(state, horizon, screenWidth, screenHeight)

            // Enemy cars with enhanced shadows
            drawEnemyCars(state, horizon, screenWidth, screenHeight)

            // Player car
            if (state.gameState != GameState.CRASHING || state.crashAnimationTime < 0.3f) {
                drawPlayerCar(state, screenWidth, screenHeight)
            }

            // Crash effects
            if (state.gameState == GameState.CRASHING) {
                drawCrashEffects(state, screenWidth, screenHeight)
            }

            // Road edges glow (bloom-enhanced)
            drawRoadEdges(state, horizon, screenWidth, screenHeight)

            // Rain overlay
            if (preset.dynamicWeather || preset == GraphicsPreset.ULTRA) {
                drawRainOverlay(state, horizon, screenWidth, screenHeight)
            }

            // Fog overlay
            if (state.weatherState.fogDensity > 0.1f) {
                drawFogOverlay(state, horizon, screenWidth, screenHeight)
            }

            // Lightning flash
            if (state.weatherState.lightningAlpha > 0.01f) {
                drawLightningFlash(state, screenWidth, screenHeight)
            }
        }
    }

    // ─── SKY WITH VOLUMETRIC CLOUDS ────────────────────────────────────────

    private fun DrawScope.drawSky(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val preset = state.graphicsPreset
        val weather = state.weatherState

        // Base sky gradient — adapts to weather
        val skyColors = when (weather.current) {
            WeatherType.SUNSET -> listOf(
                Color(0xFF1A0533), Color(0xFF3D0C5C),
                Color(0xFF8B2252), Color(0xFFCD5C3C)
            )
            WeatherType.STORM -> listOf(
                Color(0xFF0A0A14), Color(0xFF141428),
                Color(0xFF1E1E3C), Color(0xFF1A1A30)
            )
            WeatherType.FOG -> listOf(
                Color(0xFF0D0D1A), Color(0xFF151528),
                Color(0xFF1C1C38), Color(0xFF1E1E3E)
            )
            WeatherType.CLOUDY -> listOf(
                Color(0xFF080818), Color(0xFF101030),
                Color(0xFF181840), Color(0xFF1C1C44)
            )
            else -> listOf(
                Color(0xFF050510), Color(0xFF0D0D2B),
                Color(0xFF1A1040), Color(0xFF16213E)
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = skyColors,
                startY = 0f,
                endY = horizon
            ),
            size = Size(w, horizon)
        )

        // Stars (clear weather only, fewer in ULTRA with volumetric clouds)
        if (weather.current == WeatherType.CLEAR && !preset.volumetricClouds) {
            val starCount = (20 + preset.shadowQuality * 10)
            for (i in 0 until starCount) {
                val sx = (i * 137.5f + 50f) % w
                val sy = (i * 97.3f + 20f) % (horizon * 0.7f)
                val alpha = 0.3f + 0.7f * abs(sin(i * 2.7f))
                val starSize = 0.8f + (i % 3) * 0.8f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = starSize,
                    center = Offset(sx, sy)
                )
                // ULTRA: star glow
                if (preset == GraphicsPreset.ULTRA && i % 3 == 0) {
                    drawCircle(
                        color = Color(0x44FFFFFF).copy(alpha = alpha * 0.5f),
                        radius = starSize * 3f,
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        // Volumetric clouds
        if (preset.volumetricClouds || weather.cloudCoverage > 0.3f) {
            drawVolumetricClouds(state, horizon, w, h)
        }
    }

    private fun DrawScope.drawVolumetricClouds(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val weather = state.weatherState
        val coverage = if (state.graphicsPreset.volumetricClouds) max(0.6f, weather.cloudCoverage) else weather.cloudCoverage
        val time = state.distanceTraveled * 0.002f
        val cloudColor = when (weather.current) {
            WeatherType.STORM -> Color(0xFF2A2A3E)
            WeatherType.SUNSET -> Color(0xFF3D1A2E)
            WeatherType.FOG -> Color(0xFF3A3A4E)
            else -> Color(0xFF1A1A3C)
        }

        // Multiple cloud layers
        for (layer in 0 until 3) {
            val layerAlpha = (0.08f + layer * 0.06f) * coverage
            val layerSpeed = (1f + layer * 0.6f)
            val cloudCount = 12 - layer * 3
            for (i in 0 until cloudCount) {
                val cx = ((i * w / cloudCount + time * 15f * layerSpeed + layer * 80f) % (w + 200f)) - 100f
                val cy = horizon * (0.15f + layer * 0.13f + sin(i * 1.7f + layer) * 0.05f)
                val cw = w * (0.12f + layer * 0.06f + abs(sin(i * 0.9f + time * 0.3f)) * 0.08f)
                val ch = horizon * (0.08f + layer * 0.03f)

                // Cloud blob: multiple overlapping ovals
                for (blob in 0 until (3 + state.graphicsPreset.shadowQuality)) {
                    val bx = cx + sin(blob * 1.3f) * cw * 0.5f
                    val by = cy + cos(blob * 1.7f) * ch * 0.4f
                    val radius = (cw * 0.3f + ch * 0.3f) * (0.6f + blob * 0.2f)

                    drawCircle(
                        color = cloudColor.copy(alpha = layerAlpha * (1f - blob * 0.15f)),
                        radius = radius,
                        center = Offset(bx, by)
                    )
                }

                // ULTRA: cloud edge highlight
                if (state.graphicsPreset == GraphicsPreset.ULTRA && weather.current == WeatherType.SUNSET) {
                    drawCircle(
                        color = Color(0x30FF8C42),
                        radius = sqrt(cw * cw + ch * ch) * 0.5f,
                        center = Offset(cx + cw * 0.2f, cy - ch * 0.2f)
                    )
                }
            }
        }
    }

    // ─── WEATHER BACKGROUND ────────────────────────────────────────────────

    private fun DrawScope.drawWeatherBackground(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val weather = state.weatherState

        // Storm clouds across sky
        if (weather.current == WeatherType.STORM || weather.current == WeatherType.RAIN) {
            val intensity = weather.rainIntensity
            for (i in 0 until 8) {
                val cy = horizon * (0.2f + i * 0.06f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x00000000),
                            Color(0xFF1A1A2E).copy(alpha = 0.3f * intensity),
                            Color(0xFF2A2A3E).copy(alpha = 0.4f * intensity),
                            Color(0xFF1A1A2E).copy(alpha = 0.3f * intensity),
                            Color(0x00000000)
                        )
                    ),
                    topLeft = Offset(0f, cy),
                    size = Size(w, horizon * 0.08f)
                )
            }
        }
    }

    // ─── ROAD WITH SSR REFLECTIONS ────────────────────────────────────────

    private fun DrawScope.drawRoad(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val preset = state.graphicsPreset
        val weather = state.weatherState
        val stripCount = when {
            preset == GraphicsPreset.ULTRA -> 120
            preset.shadowQuality >= 2 -> 80
            else -> 60
        }

        val isWet = weather.rainIntensity > 0.2f
        val wetness = weather.rainIntensity.coerceIn(0f, 1f)

        for (i in 0 until stripCount) {
            val t = i.toFloat() / stripCount
            val tNext = (i + 1).toFloat() / stripCount

            val yTop = horizon + (h - horizon) * (t * t * t)
            val yBottom = horizon + (h - horizon) * (tNext * tNext * tNext)

            val roadWidthTop = roadWidthAtY(yTop, horizon, w)
            val roadWidthBottom = roadWidthAtY(yBottom, horizon, w)
            val centerX = w / 2f

            val path = Path().apply {
                moveTo(centerX - roadWidthTop, yTop)
                lineTo(centerX + roadWidthTop, yTop)
                lineTo(centerX + roadWidthBottom, yBottom)
                lineTo(centerX - roadWidthBottom, yBottom)
                close()
            }

            val brightness = 0.15f + t * 0.35f
            // Wet road is darker and more reflective
            val wetDark = 1f - wetness * 0.4f
            val roadColor = Color(
                red = brightness * 0.3f * wetDark,
                green = brightness * 0.3f * wetDark,
                blue = brightness * 0.6f * wetDark
            )

            drawPath(path, roadColor)

            // Wet road shimmer
            if (isWet && preset.shadowQuality >= 2) {
                val shimmerAlpha = wetness * t * 0.08f
                drawPath(path, Color(0x44FFFFFF).copy(alpha = shimmerAlpha))
            }

            // Road edge lines with ULTRA bloom
            val edgeAlpha = (0.3f + t * 0.7f) * 0.6f
            val edgeWidth = when {
                preset == GraphicsPreset.ULTRA -> max(1f, 3f * t)
                preset.shadowQuality >= 2 -> max(1f, 2f * t)
                else -> max(1f, 2f * t)
            }

            // ULTRA: edge bloom layer
            if (preset.bloomEnabled) {
                drawLine(
                    color = roadEdgeColor.copy(alpha = edgeAlpha * 0.4f),
                    start = Offset(centerX - roadWidthTop, yTop),
                    end = Offset(centerX - roadWidthBottom, yBottom),
                    strokeWidth = edgeWidth * 3f
                )
                drawLine(
                    color = roadEdgeColor.copy(alpha = edgeAlpha * 0.4f),
                    start = Offset(centerX + roadWidthTop, yTop),
                    end = Offset(centerX + roadWidthBottom, yBottom),
                    strokeWidth = edgeWidth * 3f
                )
            }

            drawLine(
                color = roadEdgeColor.copy(alpha = edgeAlpha),
                start = Offset(centerX - roadWidthTop, yTop),
                end = Offset(centerX - roadWidthBottom, yBottom),
                strokeWidth = edgeWidth
            )
            drawLine(
                color = roadEdgeColor.copy(alpha = edgeAlpha),
                start = Offset(centerX + roadWidthTop, yTop),
                end = Offset(centerX + roadWidthBottom, yBottom),
                strokeWidth = edgeWidth
            )
        }
    }

    // ─── SSR (SCREEN SPACE REFLECTIONS) ──────────────────────────────────

    private fun DrawScope.drawSSRReflections(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val preset = state.graphicsPreset
        if (!preset.ssrEnabled) return

        val weather = state.weatherState
        val reflectivity = if (weather.rainIntensity > 0.2f) {
            0.15f + weather.rainIntensity * 0.25f
        } else {
            0.06f
        }

        // Reflect neon road edges on road surface
        for (i in 0 until 30) {
            val t = i.toFloat() / 30f
            val y = horizon + (h - horizon) * t * t * t
            val roadW = roadWidthAtY(y, horizon, w)
            val centerX = w / 2f

            // Left and right edge reflections
            for (side in listOf(-1, 1)) {
                val x = centerX + side * roadW
                val reflectionY = y + (h - y) * 0.5f
                if (reflectionY < h && reflectionY > horizon) {
                    val alpha = reflectivity * (1f - t) * 0.5f
                    drawCircle(
                        color = roadEdgeColor.copy(alpha = alpha),
                        radius = max(1f, 3f * (1f - t) + 0.5f),
                        center = Offset(x, reflectionY)
                    )
                }
            }
        }

        // Reflect coins as road glow
        for (coin in state.coins) {
            if (coin.collected) continue
            val z = coin.worldZ
            if (z <= 0f) continue
            val scale = FOCAL_LENGTH / (CAMERA_HEIGHT + z)
            val screenY = horizon + CAMERA_HEIGHT * scale
            if (screenY > h || screenY < horizon) continue
            val roadW = roadWidthAtY(screenY, horizon, w)
            val centerX = w / 2f
            val laneOffset = (coin.lane.toFloat() / LANE_COUNT - 0.5f) * 2f * roadW
            val screenX = centerX + laneOffset

            val reflectY = screenY + (h - screenY) * 0.4f
            if (reflectY < h) {
                drawOval(
                    color = coinGlowColor.copy(alpha = 0.07f),
                    topLeft = Offset(screenX - 15f * scale, reflectY - 3f),
                    size = Size(30f * scale, 6f * scale)
                )
            }
        }
    }

    // ─── LANE MARKINGS ────────────────────────────────────────────────────

    private fun DrawScope.drawLaneMarkings(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val preset = state.graphicsPreset
        for (lane in 1 until LANE_COUNT) {
            val stripCount = when {
                preset == GraphicsPreset.ULTRA -> 80
                preset.shadowQuality >= 1 -> 50
                else -> 50
            }
            val scroll = state.roadScroll
            for (i in 0 until stripCount) {
                val t = (i.toFloat() + (scroll % 10f) / 10f) / stripCount
                if (t < 0f || t > 1f) continue

                val tSquared = t * t
                val y = horizon + (h - horizon) * tSquared * t
                val roadW = roadWidthAtY(y, horizon, w)
                val centerX = w / 2f
                val laneOffset = (lane.toFloat() / LANE_COUNT - 0.5f) * 2f * roadW
                val x = centerX + laneOffset

                val dashLength = max(2f, 8f * tSquared)
                val dashAlpha = (0.15f + t * 0.5f) * 0.7f

                if (i % 3 == 0) {
                    val y2 = y + dashLength
                    // ULTRA: bloom under lane markers
                    if (preset.bloomEnabled) {
                        drawLine(
                            color = laneMarkerColor.copy(alpha = dashAlpha * 0.5f),
                            start = Offset(x, y - 1f),
                            end = Offset(x, min(y2, h) + 1f),
                            strokeWidth = max(0.5f, 4f * t)
                        )
                    }
                    drawLine(
                        color = laneMarkerColor.copy(alpha = dashAlpha),
                        start = Offset(x, y),
                        end = Offset(x, min(y2, h)),
                        strokeWidth = max(0.5f, 2f * t)
                    )
                }
            }
        }
    }

    // ─── ROAD EDGES (BLOOM ENHANCED) ─────────────────────────────────────

    private fun DrawScope.drawRoadEdges(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val preset = state.graphicsPreset
        val glowStrips = when {
            preset == GraphicsPreset.ULTRA -> 20
            preset.neonGlowQuality >= 2 -> 10
            else -> 10
        }
        for (side in listOf(-1, 1)) {
            for (i in 0..glowStrips) {
                val t = i.toFloat() / glowStrips
                val tSq = t * t
                val y = horizon + (h - horizon) * tSq * t
                val roadW = roadWidthAtY(y, horizon, w)
                val centerX = w / 2f
                val x = centerX + side * roadW

                val alpha = (0.8f * (1f - t)).coerceIn(0f, 0.8f)

                // ULTRA: double bloom layers
                if (preset.bloomEnabled) {
                    drawCircle(
                        color = roadEdgeColor.copy(alpha = alpha * 0.25f),
                        radius = max(1f, 10f * (1f - t) + 2f),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = roadEdgeColor.copy(alpha = alpha * 0.5f),
                        radius = max(1f, 6f * (1f - t) + 1f),
                        center = Offset(x, y)
                    )
                }

                drawCircle(
                    color = roadEdgeColor.copy(alpha = alpha),
                    radius = max(1f, 4f * (1f - t) + 1f),
                    center = Offset(x, y)
                )
            }
        }

        // ULTRA: cross-street glow connectors
        if (preset == GraphicsPreset.ULTRA) {
            for (i in 0..15) {
                val t = i.toFloat() / 15f
                val tSq = t * t
                val y = horizon + (h - horizon) * tSq * t
                val roadW = roadWidthAtY(y, horizon, w)
                val centerX = w / 2f
                val alpha = (0.15f * (1f - t)).coerceIn(0f, 0.15f)
                drawLine(
                    color = roadEdgeColor.copy(alpha = alpha),
                    start = Offset(centerX - roadW, y),
                    end = Offset(centerX + roadW, y),
                    strokeWidth = 0.5f
                )
            }
        }
    }

    // ─── MOTION BLUR TRAILS ──────────────────────────────────────────────

    private fun DrawScope.drawMotionTrails(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        // Draw ghost trails behind enemy cars
        for (car in state.enemyCars) {
            val z = car.worldZ
            if (z <= 0f) continue
            if (car.worldZ < DESPAWN_Z + 30f) continue

            // Render 3 ghost images behind the car for motion blur effect
            for (trailIdx in 1..3) {
                val trailZ = z + trailIdx * 15f
                val scale = FOCAL_LENGTH / (CAMERA_HEIGHT + trailZ)
                val screenY = horizon + CAMERA_HEIGHT * scale
                if (screenY > h + 20f || screenY < horizon - 20f) continue

                val roadW = roadWidthAtY(screenY, horizon, w)
                val centerX = w / 2f
                val laneOffset = (car.lane.toFloat() / LANE_COUNT - 0.5f) * 2f * roadW
                val screenX = centerX + laneOffset

                val carScale = scale * 0.7f
                val carWidth = 24f * carScale
                val carHeight = 40f * carScale
                if (carWidth < 2f || carHeight < 3f) continue

                val trailAlpha = (0.12f / trailIdx) * (1f - (z / 500f).coerceIn(0f, 1f))

                drawCarSilhouette(
                    x = screenX,
                    y = screenY - carHeight / 2f,
                    width = carWidth,
                    height = carHeight,
                    color = car.color.neonColor.copy(alpha = trailAlpha),
                    alpha = trailAlpha
                )
            }
        }

        // Player car speed trails at high speed
        if (state.speed > BASE_SPEED * 1.5f) {
            val speedFactor = ((state.speed - BASE_SPEED) / (BASE_SPEED * 2f)).coerceIn(0f, 1f)
            val playerLane = state.playerLaneProgress
            val roadW = roadWidthAtY(h * 0.88f, h * HORIZON_RATIO, w)
            val centerX = w / 2f
            val laneOffset = (playerLane / LANE_COUNT - 0.5f) * 2f * roadW
            val screenX = centerX + laneOffset

            for (t in 1..4) {
                val alpha = speedFactor * (0.1f / t)
                val spread = t * 6f * speedFactor
                drawLine(
                    color = state.playerCar.color.neonColor.copy(alpha = alpha),
                    start = Offset(screenX - spread, h * 0.88f),
                    end = Offset(screenX - spread, h * 0.94f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = state.playerCar.color.neonColor.copy(alpha = alpha),
                    start = Offset(screenX + spread, h * 0.88f),
                    end = Offset(screenX + spread, h * 0.94f),
                    strokeWidth = 2f
                )
            }
        }
    }

    // ─── ENEMY CARS ──────────────────────────────────────────────────────

    private fun DrawScope.drawEnemyCars(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val preset = state.graphicsPreset
        for (car in state.enemyCars) {
            val z = car.worldZ
            if (z <= 0f) continue
            if (z > preset.maxDrawDistance) continue

            val scale = FOCAL_LENGTH / (CAMERA_HEIGHT + z)
            val screenY = horizon + CAMERA_HEIGHT * scale
            if (screenY > h + 20f || screenY < horizon - 20f) continue

            val roadW = roadWidthAtY(screenY, horizon, w)
            val centerX = w / 2f
            val laneOffset = (car.lane.toFloat() / LANE_COUNT - 0.5f) * 2f * roadW
            val screenX = centerX + laneOffset

            val carScale = scale * 0.7f
            val carWidth = 24f * carScale
            val carHeight = 40f * carScale
            if (carWidth < 3f || carHeight < 5f) continue

            // Enhanced shadow
            if (preset.shadowQuality >= 1) {
                val shadowAlpha = 0.3f + preset.shadowQuality * 0.15f
                val shadowSpread = 1.0f + preset.shadowQuality * 0.15f
                drawOval(
                    color = Color.Black.copy(alpha = shadowAlpha),
                    topLeft = Offset(
                        screenX - carWidth * 0.5f * shadowSpread,
                        screenY + carHeight * 0.35f
                    ),
                    size = Size(carWidth * 1.0f * shadowSpread, carHeight * 0.1f)
                )
                // ULTRA: second softer shadow
                if (preset == GraphicsPreset.ULTRA) {
                    drawOval(
                        color = Color.Black.copy(alpha = shadowAlpha * 0.5f),
                        topLeft = Offset(
                            screenX - carWidth * 0.7f,
                            screenY + carHeight * 0.3f
                        ),
                        size = Size(carWidth * 1.4f, carHeight * 0.15f)
                    )
                }
            }

            drawCar(
                x = screenX,
                y = screenY - carHeight / 2f,
                width = carWidth,
                height = carHeight,
                bodyColor = car.color.bodyColor,
                neonColor = car.color.neonColor,
                model = car.model,
                isPlayer = false,
                preset = preset
            )
        }
    }

    // ─── COINS WITH BLOOM ────────────────────────────────────────────────

    private fun DrawScope.drawCoins(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val preset = state.graphicsPreset
        for (coin in state.coins) {
            if (coin.collected) continue
            val z = coin.worldZ
            if (z <= 0f || z > preset.maxDrawDistance) continue

            val scale = FOCAL_LENGTH / (CAMERA_HEIGHT + z)
            val screenY = horizon + CAMERA_HEIGHT * scale
            if (screenY > h || screenY < horizon) continue

            val roadW = roadWidthAtY(screenY, horizon, w)
            val centerX = w / 2f
            val laneOffset = (coin.lane.toFloat() / LANE_COUNT - 0.5f) * 2f * roadW
            val screenX = centerX + laneOffset

            val coinSize = max(3f, 8f * scale)
            val bobY = sin(coin.bobOffset + state.distanceTraveled * 0.1f) * coinSize * 0.3f

            // ULTRA: triple-layer bloom
            if (preset.bloomEnabled) {
                drawCircle(
                    color = coinGlowColor.copy(alpha = 0.2f),
                    radius = coinSize * 4f,
                    center = Offset(screenX, screenY + bobY)
                )
                drawCircle(
                    color = coinGlowColor.copy(alpha = 0.4f),
                    radius = coinSize * 2.5f,
                    center = Offset(screenX, screenY + bobY)
                )
            }

            // Standard glow
            drawCircle(
                color = coinGlowColor,
                radius = coinSize * 2f,
                center = Offset(screenX, screenY + bobY)
            )
            drawCircle(
                color = coinColor,
                radius = coinSize,
                center = Offset(screenX, screenY + bobY)
            )
            // Inner highlight
            drawCircle(
                color = Color(0xFFFFF8E1),
                radius = coinSize * 0.5f,
                center = Offset(screenX - coinSize * 0.2f, screenY + bobY - coinSize * 0.2f)
            )

            // ULTRA: coin sparkle
            if (preset == GraphicsPreset.ULTRA && coinSize > 5f) {
                val sparkleAngle = state.distanceTraveled * 0.5f + coin.bobOffset
                for (s in 0 until 4) {
                    val sa = sparkleAngle + s * 1.57f
                    val sx = screenX + cos(sa) * coinSize * 1.3f
                    val sy = screenY + bobY + sin(sa) * coinSize * 1.3f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        radius = coinSize * 0.2f,
                        center = Offset(sx, sy)
                    )
                }
            }
        }
    }

    // ─── PLAYER CAR ──────────────────────────────────────────────────────

    private fun DrawScope.drawPlayerCar(
        state: GameUiState,
        w: Float,
        h: Float
    ) {
        val preset = state.graphicsPreset
        val playerLane = state.playerLaneProgress
        val roadW = roadWidthAtY(h * 0.88f, h * HORIZON_RATIO, w)
        val centerX = w / 2f
        val laneOffset = (playerLane / LANE_COUNT - 0.5f) * 2f * roadW
        val screenX = centerX + laneOffset
        val screenY = h * 0.86f

        val carWidth = 48f
        val carHeight = 82f

        // ULTRA: dual shadow layers
        if (preset.shadowQuality >= 2) {
            drawOval(
                color = Color.Black.copy(alpha = 0.2f),
                topLeft = Offset(screenX - carWidth * 0.8f, screenY - carHeight * 0.1f),
                size = Size(carWidth * 1.6f, carHeight * 0.25f)
            )
        }
        drawOval(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(screenX - carWidth * 0.6f, screenY - carHeight * 0.15f),
            size = Size(carWidth * 1.2f, carHeight * 0.2f)
        )

        // ULTRA: bloom under car
        if (preset.bloomEnabled) {
            drawOval(
                color = state.playerCar.color.neonColor.copy(alpha = 0.15f),
                topLeft = Offset(screenX - carWidth * 1.0f, screenY + carHeight * 0.1f),
                size = Size(carWidth * 2f, carHeight * 0.3f)
            )
        }

        drawCar(
            x = screenX,
            y = screenY - carHeight / 2f,
            width = carWidth,
            height = carHeight,
            bodyColor = state.playerCar.color.bodyColor,
            neonColor = state.playerCar.color.neonColor,
            model = state.playerCar.model,
            isPlayer = true,
            preset = preset
        )

        // Headlight glow on road
        val hlAlpha = if (preset.bloomEnabled) 0.25f else 0.15f
        drawOval(
            color = Color.White.copy(alpha = hlAlpha),
            topLeft = Offset(screenX - carWidth * 0.8f, screenY + carHeight * 0.3f),
            size = Size(carWidth * 1.6f, carHeight * 0.5f)
        )
        // ULTRA: extended headlight beams
        if (preset == GraphicsPreset.ULTRA) {
            drawOval(
                color = Color.White.copy(alpha = 0.06f),
                topLeft = Offset(screenX - carWidth * 1.2f, screenY + carHeight * 0.3f),
                size = Size(carWidth * 2.4f, carHeight * 1.0f)
            )
        }
    }

    // ─── CAR DRAWING ──────────────────────────────────────────────────────

    private fun DrawScope.drawCar(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        bodyColor: Color,
        neonColor: Color,
        model: CarModel,
        isPlayer: Boolean,
        preset: GraphicsPreset
    ) {
        val scale = width / 48f

        // ULTRA: bloom under-car glow (multi-layer)
        if (preset.bloomEnabled) {
            drawOval(
                color = neonColor.copy(alpha = 0.15f),
                topLeft = Offset(x - width * 0.9f, y + height * 0.7f),
                size = Size(width * 1.8f, height * 0.25f)
            )
        }
        drawOval(
            color = neonColor.copy(alpha = 0.3f),
            topLeft = Offset(x - width * 0.7f, y + height * 0.8f),
            size = Size(width * 1.4f, height * 0.15f)
        )

        // Car body (always compute path first)
        val bodyPath = getCarShape(model, x, y, width, height)

        // ULTRA: PBR-like body specular highlight
        if (preset == GraphicsPreset.ULTRA && isPlayer) {
            drawPath(bodyPath, bodyColor)
            // Specular highlight strip
            val highlightPath = getCarHighlight(model, x, y, width, height)
            drawPath(
                highlightPath,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    start = Offset(x - width * 0.3f, y),
                    end = Offset(x + width * 0.3f, y)
                )
            )
        } else {
            drawPath(bodyPath, bodyColor)
        }

        // ULTRA: extra outer neon glow
        if (preset.bloomEnabled) {
            drawPath(
                bodyPath,
                color = neonColor.copy(alpha = 0.4f),
                style = Stroke(
                    width = max(1f, 4f * scale),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Neon outline
        drawPath(
            bodyPath,
            color = neonColor,
            style = Stroke(
                width = max(1f, 2f * scale),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Windows
        val windowPath = getCarWindows(model, x, y, width, height)
        drawPath(
            windowPath,
            color = Color(0xFF1A1A2E).copy(alpha = 0.8f)
        )

        // ULTRA: window reflection
        if (preset == GraphicsPreset.ULTRA && isPlayer) {
            drawPath(
                windowPath,
                color = Color.White.copy(alpha = 0.08f),
                style = Stroke(width = max(0.5f, 1.5f * scale))
            )
        }

        drawPath(
            windowPath,
            color = Color(0xFF4488FF).copy(alpha = 0.3f),
            style = Stroke(width = max(0.5f, 1f * scale))
        )

        // Wheels
        val wheelW = width * 0.22f
        val wheelH = height * 0.12f
        val wheelY1 = y + height * 0.2f
        val wheelY2 = y + height * 0.72f

        for (wy in listOf(wheelY1, wheelY2)) {
            drawOval(color = Color.Black, topLeft = Offset(x - width * 0.42f - wheelW / 2, wy), size = Size(wheelW, wheelH))
            drawOval(color = Color.Black, topLeft = Offset(x + width * 0.42f - wheelW / 2, wy), size = Size(wheelW, wheelH))

            // ULTRA: wheel rim detail
            if (preset == GraphicsPreset.ULTRA && isPlayer) {
                drawOval(
                    color = Color(0xFF333340),
                    topLeft = Offset(x - width * 0.42f - wheelW * 0.35f, wy + wheelH * 0.1f),
                    size = Size(wheelW * 0.7f, wheelH * 0.8f)
                )
                drawOval(
                    color = Color(0xFF333340),
                    topLeft = Offset(x + width * 0.42f - wheelW * 0.35f, wy + wheelH * 0.1f),
                    size = Size(wheelW * 0.7f, wheelH * 0.8f)
                )
            }

            drawOval(
                color = neonColor.copy(alpha = 0.5f),
                topLeft = Offset(x - width * 0.42f - wheelW / 2, wy + wheelH * 0.2f),
                size = Size(wheelW, wheelH * 0.4f)
            )
            drawOval(
                color = neonColor.copy(alpha = 0.5f),
                topLeft = Offset(x + width * 0.42f - wheelW / 2, wy + wheelH * 0.2f),
                size = Size(wheelW, wheelH * 0.4f)
            )
        }

        // Headlights (player or close)
        if (isPlayer) {
            val hlW = width * 0.18f
            val hlH = height * 0.06f
            drawOval(
                color = Color.White,
                topLeft = Offset(x - width * 0.22f - hlW / 2, y + height * 0.85f),
                size = Size(hlW, hlH)
            )
            drawOval(
                color = Color.White,
                topLeft = Offset(x + width * 0.22f - hlW / 2, y + height * 0.85f),
                size = Size(hlW, hlH)
            )
            drawOval(
                color = Color(0x40FFFFFF),
                topLeft = Offset(x - width * 0.35f, y + height * 0.82f),
                size = Size(width * 0.7f, hlH * 2f)
            )

            // ULTRA: lens flares on headlights
            if (preset == GraphicsPreset.ULTRA) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = hlH * 0.8f,
                    center = Offset(x - width * 0.22f, y + height * 0.82f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = hlH * 0.8f,
                    center = Offset(x + width * 0.22f, y + height * 0.82f)
                )
            }
        }
    }

    private fun DrawScope.drawCarSilhouette(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        alpha: Float
    ) {
        // Simplified car for trails
        drawRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(x - width * 0.35f, y + height * 0.1f),
            size = Size(width * 0.7f, height * 0.7f)
        )
    }

    // ─── CRASH EFFECTS ───────────────────────────────────────────────────

    private fun DrawScope.drawCrashEffects(
        state: GameUiState,
        w: Float,
        h: Float
    ) {
        val time = state.crashAnimationTime
        val intensity = (1f - (time / 1.5f).coerceIn(0f, 1f))

        // Screen flash
        if (time < 0.3f) {
            val flashAlpha = (1f - time / 0.3f) * 0.6f
            drawRect(
                color = crashFlashColor.copy(alpha = flashAlpha),
                size = Size(w, h)
            )
        }

        // ULTRA: radial shockwave
        if (state.graphicsPreset == GraphicsPreset.ULTRA && time < 0.4f) {
            val waveRadius = time * w * 1.5f
            val waveAlpha = (1f - time / 0.4f) * 0.4f
            drawCircle(
                color = Color.White.copy(alpha = waveAlpha),
                radius = waveRadius,
                center = Offset(w / 2f, h * 0.86f),
                style = Stroke(width = 3f * (1f - time / 0.4f))
            )
            drawCircle(
                color = state.playerCar.color.neonColor.copy(alpha = waveAlpha * 0.7f),
                radius = waveRadius * 0.7f,
                center = Offset(w / 2f, h * 0.86f),
                style = Stroke(width = 2f * (1f - time / 0.4f))
            )
        }

        // Particles
        for (p in state.particles) {
            val lifeRatio = (p.life / p.maxLife).coerceIn(0f, 1f)
            val px = w / 2f + p.x
            val py = h * 0.86f + p.y
            val alpha = lifeRatio

            // ULTRA: particle glow
            if (state.graphicsPreset.bloomEnabled && p.size > 2f) {
                drawCircle(
                    color = p.color.copy(alpha = alpha * 0.25f),
                    radius = p.size * lifeRatio * 3f,
                    center = Offset(px, py)
                )
            }

            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size * lifeRatio,
                center = Offset(px, py)
            )

            if (p.size > 3f) {
                drawCircle(
                    color = p.color.copy(alpha = alpha * 0.3f),
                    radius = p.size * lifeRatio * 2f,
                    center = Offset(px, py)
                )
            }
        }
    }

    // ─── RAIN OVERLAY ─────────────────────────────────────────────────────

    private fun DrawScope.drawRainOverlay(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val intensity = state.weatherState.rainIntensity
        if (intensity < 0.05f) return

        val preset = state.graphicsPreset
        val dropCount = when {
            preset == GraphicsPreset.ULTRA -> (80 * intensity).toInt()
            preset.shadowQuality >= 2 -> (50 * intensity).toInt()
            else -> (30 * intensity).toInt()
        }

        val time = state.distanceTraveled * 0.1f
        val wind = state.weatherState.windAngle

        for (i in 0 until dropCount) {
            val baseX = (i * 73f + 17f) % w
            val baseY = ((i * 47f + time * 300f * (0.8f + intensity)) % (h + 100f)) - 50f
            if (baseY < horizon || baseY > h) continue

            val dropLen = (8f + intensity * 18f) * (0.6f + (i % 5) * 0.1f)
            val dropAlpha = (0.15f + intensity * 0.35f) * (0.7f + (i % 3) * 0.15f)

            val wx = sin(wind + i * 0.3f) * 2f * intensity
            drawLine(
                color = Color(0xFF8899CC).copy(alpha = dropAlpha),
                start = Offset(baseX, baseY),
                end = Offset(baseX + wx, baseY + dropLen),
                strokeWidth = max(0.3f, intensity * 1.2f)
            )

            // ULTRA: rain sparkle
            if (preset == GraphicsPreset.ULTRA && i % 8 == 0) {
                drawCircle(
                    color = Color.White.copy(alpha = dropAlpha * 0.3f),
                    radius = 0.5f,
                    center = Offset(baseX + wx, baseY + dropLen)
                )
            }
        }
    }

    // ─── FOG OVERLAY ─────────────────────────────────────────────────────

    private fun DrawScope.drawFogOverlay(
        state: GameUiState,
        horizon: Float,
        w: Float,
        h: Float
    ) {
        val density = state.weatherState.fogDensity.coerceIn(0f, 1f)
        val preset = state.graphicsPreset

        // Fog gradient from horizon
        for (i in 0 until 10) {
            val t = i.toFloat() / 10f
            val yStart = horizon + (h - horizon) * t
            val yEnd = horizon + (h - horizon) * (t + 0.1f)
            val alpha = density * (1f - t) * 0.25f

            val fogColor = when (state.weatherState.current) {
                WeatherType.FOG -> Color(0xFF3A3A4E)
                WeatherType.STORM -> Color(0xFF2A2A3C)
                else -> Color(0xFF4A4A5E)
            }

            drawRect(
                color = fogColor.copy(alpha = alpha),
                topLeft = Offset(0f, yStart),
                size = Size(w, yEnd - yStart)
            )
        }

        // ULTRA: fog wisps
        if (preset == GraphicsPreset.ULTRA) {
            val time = state.distanceTraveled * 0.005f
            for (i in 0 until 6) {
                val wx = ((i * w / 5f + sin(time + i) * 30f) % (w + 200f)) - 100f
                val wy = horizon + (h - horizon) * (0.3f + i * 0.08f + cos(time * 0.7f + i) * 0.05f)
                val fw = w * (0.15f + abs(sin(time * 0.5f + i)) * 0.1f)
                val fh = (h - horizon) * 0.08f
                drawOval(
                    color = Color(0xFF5A5A6E).copy(alpha = density * 0.12f),
                    topLeft = Offset(wx, wy),
                    size = Size(fw, fh)
                )
            }
        }
    }

    // ─── LIGHTNING FLASH ─────────────────────────────────────────────────

    private fun DrawScope.drawLightningFlash(
        state: GameUiState,
        w: Float,
        h: Float
    ) {
        val alpha = state.weatherState.lightningAlpha.coerceIn(0f, 1f)
        drawRect(
            color = Color.White.copy(alpha = alpha * 0.6f),
            size = Size(w, h)
        )
    }

    // ─── CAR SHAPES ───────────────────────────────────────────────────────

    private fun getCarHighlight(model: CarModel, x: Float, y: Float, w: Float, h: Float): Path {
        val hw = w / 2f
        return Path().apply {
            // Glossy highlight strip along the roof/side
            moveTo(x - hw * 0.3f, y + h * 0.1f)
            lineTo(x - hw * 0.3f, y + h * 0.5f)
            lineTo(x + hw * 0.3f, y + h * 0.5f)
            lineTo(x + hw * 0.3f, y + h * 0.1f)
            close()
        }
    }

    private fun getCarShape(model: CarModel, x: Float, y: Float, w: Float, h: Float): Path {
        val hw = w / 2f
        return when (model) {
            CarModel.SEDAN -> Path().apply {
                moveTo(x - hw * 1f, y + h * 0.6f)
                lineTo(x - hw * 0.95f, y + h * 0.15f)
                lineTo(x - hw * 0.5f, y + h * 0.02f)
                lineTo(x + hw * 0.5f, y + h * 0.02f)
                lineTo(x + hw * 0.95f, y + h * 0.15f)
                lineTo(x + hw * 1f, y + h * 0.7f)
                lineTo(x + hw * 0.95f, y + h * 0.95f)
                lineTo(x - hw * 0.95f, y + h * 0.95f)
                close()
            }
            CarModel.SPORTS -> Path().apply {
                moveTo(x - hw * 0.9f, y + h * 0.55f)
                lineTo(x - hw * 0.85f, y + h * 0.1f)
                lineTo(x - hw * 0.4f, y - h * 0.05f)
                lineTo(x + hw * 0.4f, y - h * 0.05f)
                lineTo(x + hw * 0.85f, y + h * 0.1f)
                lineTo(x + hw * 0.9f, y + h * 0.65f)
                lineTo(x + hw * 0.85f, y + h * 0.9f)
                lineTo(x - hw * 0.85f, y + h * 0.9f)
                close()
            }
            CarModel.SUV -> Path().apply {
                moveTo(x - hw * 1f, y + h * 0.5f)
                lineTo(x - hw * 0.95f, y + h * 0.05f)
                lineTo(x - hw * 0.4f, y - h * 0.02f)
                lineTo(x + hw * 0.4f, y - h * 0.02f)
                lineTo(x + hw * 0.95f, y + h * 0.05f)
                lineTo(x + hw * 1f, y + h * 0.7f)
                lineTo(x + hw * 0.95f, y + h * 0.95f)
                lineTo(x - hw * 0.95f, y + h * 0.95f)
                close()
            }
            CarModel.TRUCK -> Path().apply {
                moveTo(x - hw * 1.1f, y + h * 0.4f)
                lineTo(x - hw * 1.05f, y + h * 0.05f)
                lineTo(x - hw * 0.2f, y + h * 0.02f)
                lineTo(x + hw * 0.5f, y + h * 0.02f)
                lineTo(x + hw * 0.9f, y + h * 0.08f)
                lineTo(x + hw * 1.0f, y + h * 0.5f)
                lineTo(x + hw * 1.0f, y + h * 0.9f)
                lineTo(x - hw * 1.05f, y + h * 0.9f)
                close()
            }
            CarModel.HATCHBACK -> Path().apply {
                moveTo(x - hw * 0.95f, y + h * 0.6f)
                lineTo(x - hw * 0.9f, y + h * 0.12f)
                lineTo(x - hw * 0.3f, y + h * 0.03f)
                lineTo(x + hw * 0.3f, y + h * 0.03f)
                lineTo(x + hw * 0.85f, y + h * 0.35f)
                lineTo(x + hw * 0.85f, y + h * 0.9f)
                lineTo(x - hw * 0.9f, y + h * 0.9f)
                close()
            }
            CarModel.COUPE -> Path().apply {
                moveTo(x - hw * 0.85f, y + h * 0.5f)
                lineTo(x - hw * 0.8f, y + h * 0.05f)
                lineTo(x - hw * 0.3f, y - h * 0.03f)
                lineTo(x + hw * 0.3f, y - h * 0.03f)
                lineTo(x + hw * 0.8f, y + h * 0.1f)
                lineTo(x + hw * 0.85f, y + h * 0.6f)
                lineTo(x + hw * 0.8f, y + h * 0.85f)
                lineTo(x - hw * 0.8f, y + h * 0.85f)
                close()
            }
            CarModel.CONVERTIBLE -> Path().apply {
                moveTo(x - hw * 0.9f, y + h * 0.55f)
                lineTo(x - hw * 0.85f, y + h * 0.18f)
                lineTo(x - hw * 0.4f, y + h * 0.12f)
                lineTo(x + hw * 0.4f, y + h * 0.12f)
                lineTo(x + hw * 0.85f, y + h * 0.18f)
                lineTo(x + hw * 0.9f, y + h * 0.65f)
                lineTo(x + hw * 0.85f, y + h * 0.9f)
                lineTo(x - hw * 0.85f, y + h * 0.9f)
                close()
            }
            CarModel.PICKUP -> Path().apply {
                moveTo(x - hw * 1.0f, y + h * 0.45f)
                lineTo(x - hw * 0.95f, y + h * 0.05f)
                lineTo(x - hw * 0.3f, y + h * 0.02f)
                lineTo(x + hw * 0.2f, y + h * 0.05f)
                lineTo(x + hw * 0.95f, y + h * 0.35f)
                lineTo(x + hw * 0.95f, y + h * 0.9f)
                lineTo(x - hw * 0.95f, y + h * 0.9f)
                close()
            }
            CarModel.VAN -> Path().apply {
                moveTo(x - hw * 1.0f, y + h * 0.35f)
                lineTo(x - hw * 0.95f, y - h * 0.02f)
                lineTo(x - hw * 0.3f, y - h * 0.05f)
                lineTo(x + hw * 0.5f, y - h * 0.02f)
                lineTo(x + hw * 0.95f, y + h * 0.05f)
                lineTo(x + hw * 1.0f, y + h * 0.55f)
                lineTo(x + hw * 1.0f, y + h * 0.9f)
                lineTo(x - hw * 0.95f, y + h * 0.9f)
                close()
            }
            CarModel.MUSCLE -> Path().apply {
                moveTo(x - hw * 0.85f, y + h * 0.4f)
                lineTo(x - hw * 0.8f, y + h * 0.05f)
                lineTo(x - hw * 0.25f, y - h * 0.02f)
                lineTo(x + hw * 0.4f, y + h * 0.02f)
                lineTo(x + hw * 0.85f, y + h * 0.15f)
                lineTo(x + hw * 0.9f, y + h * 0.5f)
                lineTo(x + hw * 0.8f, y + h * 0.85f)
                lineTo(x - hw * 0.8f, y + h * 0.85f)
                close()
            }
            CarModel.SUPERCAR -> Path().apply {
                moveTo(x - hw * 0.8f, y + h * 0.5f)
                lineTo(x - hw * 0.7f, y + h * 0.05f)
                lineTo(x - hw * 0.3f, y - h * 0.1f)
                lineTo(x + hw * 0.2f, y - h * 0.08f)
                lineTo(x + hw * 0.7f, y + h * 0.05f)
                lineTo(x + hw * 0.8f, y + h * 0.55f)
                lineTo(x + hw * 0.75f, y + h * 0.85f)
                lineTo(x - hw * 0.75f, y + h * 0.85f)
                close()
            }
            CarModel.CLASSIC -> Path().apply {
                moveTo(x - hw * 1.0f, y + h * 0.5f)
                lineTo(x - hw * 0.95f, y + h * 0.08f)
                lineTo(x - hw * 0.4f, y + h * 0.02f)
                lineTo(x + hw * 0.3f, y + h * 0.05f)
                lineTo(x + hw * 0.9f, y + h * 0.2f)
                lineTo(x + hw * 0.95f, y + h * 0.6f)
                lineTo(x + hw * 0.9f, y + h * 0.9f)
                lineTo(x - hw * 0.9f, y + h * 0.9f)
                close()
            }
            CarModel.OFFROAD -> Path().apply {
                moveTo(x - hw * 1.0f, y + h * 0.5f)
                lineTo(x - hw * 0.95f, y + h * 0.05f)
                lineTo(x - hw * 0.35f, y - h * 0.02f)
                lineTo(x + hw * 0.35f, y - h * 0.02f)
                lineTo(x + hw * 0.95f, y + h * 0.08f)
                lineTo(x + hw * 1.0f, y + h * 0.65f)
                lineTo(x + hw * 0.95f, y + h * 0.9f)
                lineTo(x - hw * 0.95f, y + h * 0.9f)
                close()
            }
            CarModel.LIMO -> Path().apply {
                moveTo(x - hw * 1.15f, y + h * 0.55f)
                lineTo(x - hw * 1.1f, y + h * 0.12f)
                lineTo(x - hw * 0.4f, y + h * 0.05f)
                lineTo(x + hw * 0.4f, y + h * 0.05f)
                lineTo(x + hw * 1.1f, y + h * 0.12f)
                lineTo(x + hw * 1.15f, y + h * 0.65f)
                lineTo(x + hw * 1.1f, y + h * 0.9f)
                lineTo(x - hw * 1.1f, y + h * 0.9f)
                close()
            }
            CarModel.RACER -> Path().apply {
                moveTo(x - hw * 0.7f, y + h * 0.45f)
                lineTo(x - hw * 0.55f, y + h * 0.05f)
                lineTo(x - hw * 0.2f, y - h * 0.12f)
                lineTo(x + hw * 0.15f, y - h * 0.1f)
                lineTo(x + hw * 0.55f, y + h * 0.05f)
                lineTo(x + hw * 0.7f, y + h * 0.5f)
                lineTo(x + hw * 0.65f, y + h * 0.8f)
                lineTo(x - hw * 0.65f, y + h * 0.8f)
                close()
            }
        }
    }

    private fun getCarWindows(model: CarModel, x: Float, y: Float, w: Float, h: Float): Path {
        val hw = w / 2f
        return when (model) {
            CarModel.SEDAN -> Path().apply {
                moveTo(x - hw * 0.5f, y + h * 0.18f)
                lineTo(x - hw * 0.2f, y + h * 0.1f)
                lineTo(x + hw * 0.2f, y + h * 0.1f)
                lineTo(x + hw * 0.45f, y + h * 0.2f)
                lineTo(x + hw * 0.45f, y + h * 0.45f)
                lineTo(x - hw * 0.5f, y + h * 0.45f)
                close()
            }
            CarModel.SPORTS -> Path().apply {
                moveTo(x - hw * 0.4f, y + h * 0.08f)
                lineTo(x - hw * 0.15f, y + h * 0.02f)
                lineTo(x + hw * 0.15f, y + h * 0.02f)
                lineTo(x + hw * 0.4f, y + h * 0.12f)
                lineTo(x + hw * 0.4f, y + h * 0.35f)
                lineTo(x - hw * 0.4f, y + h * 0.35f)
                close()
            }
            CarModel.SUV -> Path().apply {
                moveTo(x - hw * 0.5f, y + h * 0.1f)
                lineTo(x - hw * 0.15f, y + h * 0.05f)
                lineTo(x + hw * 0.15f, y + h * 0.05f)
                lineTo(x + hw * 0.45f, y + h * 0.12f)
                lineTo(x + hw * 0.45f, y + h * 0.4f)
                lineTo(x - hw * 0.5f, y + h * 0.4f)
                close()
            }
            CarModel.TRUCK -> Path().apply {
                moveTo(x - hw * 0.5f, y + h * 0.1f)
                lineTo(x + hw * 0.0f, y + h * 0.08f)
                lineTo(x + hw * 0.3f, y + h * 0.08f)
                lineTo(x + hw * 0.3f, y + h * 0.35f)
                lineTo(x - hw * 0.5f, y + h * 0.35f)
                close()
            }
            CarModel.HATCHBACK -> Path().apply {
                moveTo(x - hw * 0.35f, y + h * 0.1f)
                lineTo(x + hw * 0.3f, y + h * 0.08f)
                lineTo(x + hw * 0.3f, y + h * 0.38f)
                lineTo(x - hw * 0.35f, y + h * 0.38f)
                close()
            }
            CarModel.COUPE -> Path().apply {
                moveTo(x - hw * 0.35f, y + h * 0.06f)
                lineTo(x - hw * 0.1f, y + h * 0.02f)
                lineTo(x + hw * 0.15f, y + h * 0.02f)
                lineTo(x + hw * 0.35f, y + h * 0.1f)
                lineTo(x + hw * 0.35f, y + h * 0.32f)
                lineTo(x - hw * 0.35f, y + h * 0.32f)
                close()
            }
            CarModel.CONVERTIBLE -> Path().apply {
                moveTo(x - hw * 0.3f, y + h * 0.32f)
                lineTo(x - hw * 0.25f, y + h * 0.15f)
                lineTo(x + hw * 0.25f, y + h * 0.15f)
                lineTo(x + hw * 0.3f, y + h * 0.32f)
                close()
            }
            CarModel.PICKUP -> Path().apply {
                moveTo(x - hw * 0.45f, y + h * 0.1f)
                lineTo(x + hw * 0.0f, y + h * 0.08f)
                lineTo(x + hw * 0.25f, y + h * 0.1f)
                lineTo(x + hw * 0.25f, y + h * 0.35f)
                lineTo(x - hw * 0.45f, y + h * 0.35f)
                close()
            }
            CarModel.VAN -> Path().apply {
                moveTo(x - hw * 0.3f, y + h * 0.02f)
                lineTo(x + hw * 0.4f, y + h * 0.05f)
                lineTo(x + hw * 0.4f, y + h * 0.38f)
                lineTo(x - hw * 0.3f, y + h * 0.38f)
                close()
            }
            CarModel.MUSCLE -> Path().apply {
                moveTo(x - hw * 0.3f, y + h * 0.06f)
                lineTo(x - hw * 0.1f, y + h * 0.03f)
                lineTo(x + hw * 0.2f, y + h * 0.03f)
                lineTo(x + hw * 0.4f, y + h * 0.15f)
                lineTo(x + hw * 0.4f, y + h * 0.3f)
                lineTo(x - hw * 0.3f, y + h * 0.3f)
                close()
            }
            CarModel.SUPERCAR -> Path().apply {
                moveTo(x - hw * 0.25f, y + h * 0.02f)
                lineTo(x + hw * 0.15f, y - h * 0.02f)
                lineTo(x + hw * 0.35f, y + h * 0.08f)
                lineTo(x + hw * 0.35f, y + h * 0.3f)
                lineTo(x - hw * 0.25f, y + h * 0.3f)
                close()
            }
            CarModel.CLASSIC -> Path().apply {
                moveTo(x - hw * 0.4f, y + h * 0.12f)
                lineTo(x - hw * 0.15f, y + h * 0.08f)
                lineTo(x + hw * 0.2f, y + h * 0.1f)
                lineTo(x + hw * 0.45f, y + h * 0.22f)
                lineTo(x + hw * 0.45f, y + h * 0.42f)
                lineTo(x - hw * 0.4f, y + h * 0.42f)
                close()
            }
            CarModel.OFFROAD -> Path().apply {
                moveTo(x - hw * 0.35f, y + h * 0.08f)
                lineTo(x + hw * 0.3f, y + h * 0.05f)
                lineTo(x + hw * 0.45f, y + h * 0.12f)
                lineTo(x + hw * 0.45f, y + h * 0.38f)
                lineTo(x - hw * 0.35f, y + h * 0.38f)
                close()
            }
            CarModel.LIMO -> Path().apply {
                moveTo(x - hw * 0.4f, y + h * 0.12f)
                lineTo(x + hw * 0.35f, y + h * 0.1f)
                lineTo(x + hw * 0.5f, y + h * 0.15f)
                lineTo(x + hw * 0.5f, y + h * 0.42f)
                lineTo(x - hw * 0.4f, y + h * 0.42f)
                close()
            }
            CarModel.RACER -> Path().apply {
                moveTo(x - hw * 0.2f, y + h * 0.02f)
                lineTo(x + hw * 0.1f, y - h * 0.04f)
                lineTo(x + hw * 0.25f, y + h * 0.06f)
                lineTo(x + hw * 0.25f, y + h * 0.25f)
                lineTo(x - hw * 0.2f, y + h * 0.25f)
                close()
            }
        }
    }

    // ─── UTILITY ──────────────────────────────────────────────────────────

    fun roadWidthAtY(screenY: Float, horizon: Float, screenWidth: Float): Float {
        if (screenY <= horizon) return 0f
        val scale = (screenY - horizon) / (screenWidth * 0.85f - horizon)
        return screenWidth * 0.35f + scale * screenWidth * 0.45f
    }

    fun projectToScreen(
        worldZ: Float,
        worldX: Float,
        horizon: Float,
        screenWidth: Float,
        screenHeight: Float
    ): Offset {
        if (worldZ <= 0f) return Offset(screenWidth / 2f, screenHeight)
        val scale = FOCAL_LENGTH / (CAMERA_HEIGHT + worldZ)
        val screenY = horizon + CAMERA_HEIGHT * scale
        val screenX = screenWidth / 2f + worldX * scale
        return Offset(screenX, screenY)
    }
}
