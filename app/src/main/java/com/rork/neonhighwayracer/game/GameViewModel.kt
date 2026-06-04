package com.rork.neonhighwayracer.game

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var frameCount = 0
    private var spawnTimerCar = 0f
    private var spawnTimerCoin = 0f
    private var speedTimer = 0f
    private var lastEnemyLane = -1
    private var lastCoinLane = -1
    private var weatherTimer = 0f
    private var weatherChangeInterval = 0f
    private var graphicsPreset: GraphicsPreset = GraphicsPreset.ULTRA

    fun init(context: Context) {
        graphicsPreset = GraphicsPreset.load(context)
        val weather = randomWeather()
        _uiState.update {
            it.copy(
                graphicsPreset = graphicsPreset,
                weatherState = WeatherState(
                    current = weather,
                    cloudCoverage = cloudCoverageFor(weather),
                    rainIntensity = rainFor(weather),
                    fogDensity = fogFor(weather)
                )
            )
        }
        weatherChangeInterval = 45f + Random.nextFloat() * 60f
        startWeatherCycle()
    }

    fun setGraphicsPreset(preset: GraphicsPreset, context: Context) {
        graphicsPreset = preset
        GraphicsPreset.save(context, preset)
        _uiState.update { it.copy(graphicsPreset = preset) }
    }

    private fun startWeatherCycle() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                transitionWeather()
            }
        }
    }

    private fun transitionWeather() {
        val state = _uiState.value
        val weather = state.weatherState
        val newTransition = weather.transitionProgress + 0.005f

        if (newTransition >= 1f) {
            // Pick new weather
            val newWeather = randomWeather()
            _uiState.update {
                it.copy(
                    weatherState = WeatherState(
                        current = newWeather,
                        transitionProgress = 0f,
                        next = randomWeather(),
                        rainIntensity = rainFor(newWeather),
                        fogDensity = fogFor(newWeather),
                        cloudCoverage = cloudCoverageFor(newWeather),
                        windAngle = Random.nextFloat() * 6.28f,
                        elapsed = 0f
                    )
                )
            }
        } else {
            // Smooth transition
            val fromRain = weather.rainIntensity
            val toRain = rainFor(weather.next)
            val fromFog = weather.fogDensity
            val toFog = fogFor(weather.next)
            val fromCloud = weather.cloudCoverage
            val toCloud = cloudCoverageFor(weather.next)

            val t = smoothstep(newTransition)
            _uiState.update {
                it.copy(
                    weatherState = weather.copy(
                        transitionProgress = newTransition,
                        rainIntensity = lerp(fromRain, toRain, t),
                        fogDensity = lerp(fromFog, toFog, t),
                        cloudCoverage = lerp(fromCloud, toCloud, t),
                        lightningTimer = weather.lightningTimer + 0.016f,
                        lightningAlpha = if (weather.lightningAlpha > 0.01f)
                            weather.lightningAlpha * 0.85f else 0f,
                        elapsed = weather.elapsed + 0.016f
                    )
                )
            }
        }

        // Random lightning in storms
        if (_uiState.value.weatherState.current == WeatherType.STORM ||
            _uiState.value.weatherState.next == WeatherType.STORM) {
            if (Random.nextFloat() < 0.003f) {
                _uiState.update {
                    it.copy(
                        weatherState = it.weatherState.copy(
                            lightningAlpha = 0.6f + Random.nextFloat() * 0.4f,
                            lightningTimer = 0f
                        )
                    )
                }
            }
        }
    }

    private fun randomWeather(): WeatherType {
        val roll = Random.nextFloat()
        return when {
            roll < 0.35f -> WeatherType.CLEAR
            roll < 0.55f -> WeatherType.CLOUDY
            roll < 0.70f -> WeatherType.SUNSET
            roll < 0.82f -> WeatherType.RAIN
            roll < 0.93f -> WeatherType.FOG
            else -> WeatherType.STORM
        }
    }

    private fun rainFor(w: WeatherType): Float = when (w) {
        WeatherType.RAIN -> 0.5f + Random.nextFloat() * 0.5f
        WeatherType.STORM -> 0.7f + Random.nextFloat() * 0.3f
        else -> 0f
    }

    private fun fogFor(w: WeatherType): Float = when (w) {
        WeatherType.FOG -> 0.5f + Random.nextFloat() * 0.5f
        WeatherType.STORM -> 0.2f
        else -> 0f
    }

    private fun cloudCoverageFor(w: WeatherType): Float = when (w) {
        WeatherType.CLEAR -> 0.1f + Random.nextFloat() * 0.2f
        WeatherType.SUNSET -> 0.2f + Random.nextFloat() * 0.3f
        WeatherType.CLOUDY -> 0.6f + Random.nextFloat() * 0.4f
        WeatherType.RAIN -> 0.7f + Random.nextFloat() * 0.3f
        WeatherType.STORM -> 0.8f + Random.nextFloat() * 0.2f
        WeatherType.FOG -> 0.5f + Random.nextFloat() * 0.3f
    }

    private fun smoothstep(t: Float): Float = t * t * (3f - 2f * t)
    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun startGame() {
        val state = _uiState.value
        _uiState.value = GameUiState(
            gameState = GameState.PLAYING,
            playerCar = Car(
                model = state.selectedCarModel,
                color = state.selectedCarColor,
                lane = 2,
                worldZ = PLAYER_Z,
                isPlayer = true
            ),
            selectedCarModel = state.selectedCarModel,
            selectedCarColor = state.selectedCarColor,
            highScore = state.highScore,
            graphicsPreset = graphicsPreset,
            weatherState = state.weatherState
        )
    }

    fun selectCar(model: CarModel) {
        _uiState.update { it.copy(selectedCarModel = model) }
    }

    fun selectColor(color: CarColor) {
        _uiState.update { it.copy(selectedCarColor = color) }
    }

    fun steerLeft() {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING) return
        if (state.playerLaneProgress > 0f) {
            _uiState.update {
                it.copy(playerLaneProgress = (it.playerLaneProgress - LANE_CHANGE_SPEED * 2)
                    .coerceAtLeast(0f))
            }
        }
    }

    fun steerRight() {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING) return
        if (state.playerLaneProgress < LANE_COUNT - 1) {
            _uiState.update {
                it.copy(playerLaneProgress = (it.playerLaneProgress + LANE_CHANGE_SPEED * 2)
                    .coerceAtMost((LANE_COUNT - 1).toFloat()))
            }
        }
    }

    fun swipeToLane(targetLane: Int) {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING) return
        val clamped = targetLane.coerceIn(0, LANE_COUNT - 1)
        _uiState.update { it.copy(playerLaneProgress = clamped.toFloat()) }
    }

    fun returnToMenu() {
        val state = _uiState.value
        _uiState.value = GameUiState(
            gameState = GameState.MENU,
            selectedCarModel = state.selectedCarModel,
            selectedCarColor = state.selectedCarColor,
            highScore = state.highScore,
            graphicsPreset = graphicsPreset,
            weatherState = state.weatherState
        )
    }

    fun restartGame() {
        startGame()
    }

    /** Called every frame. dt is delta time in seconds. */
    fun tick(dt: Float) {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING && state.gameState != GameState.CRASHING) return

        frameCount++
        val preset = state.graphicsPreset

        if (state.gameState == GameState.CRASHING) {
            handleCrashAnimation(state, dt, preset)
            return
        }

        val speed = state.speed

        // Update speed over time
        speedTimer += dt
        val newSpeed = BASE_SPEED + SPEED_INCREMENT * (speedTimer / SPEED_INTERVAL_SECONDS)

        val elapsedMillis = state.elapsedSeconds * 1000L + (dt * 1000).toLong()
        val newElapsedSeconds = (elapsedMillis / 1000).toInt()

        // Move road scroll
        val newRoadScroll = (state.roadScroll + speed * 2f) % 100f

        // Update weather
        transitionWeather()

        // Move enemy cars
        val movedCars = state.enemyCars.mapNotNull { car ->
            val newZ = car.worldZ - speed * 40f * dt
            if (newZ <= DESPAWN_Z || newZ > preset.maxDrawDistance) null
            else car.copy(worldZ = newZ)
        }.toMutableList()

        // Move coins
        val movedCoins = state.coins.mapNotNull { coin ->
            val newZ = coin.worldZ - speed * 40f * dt
            if (newZ <= DESPAWN_Z || newZ > preset.maxDrawDistance) null
            else coin.copy(worldZ = newZ)
        }.toMutableList()

        // Spawn enemy cars
        spawnTimerCar += dt
        val carSpawnInterval = (1.2f / (newSpeed / BASE_SPEED)).coerceAtLeast(
            if (preset == GraphicsPreset.ULTRA) 0.2f else 0.3f
        )
        if (spawnTimerCar >= carSpawnInterval && movedCars.size < MAX_ENEMY_CARS) {
            spawnTimerCar = 0f
            val lane = generateUniqueLane(lastEnemyLane, movedCars.map { it.lane }, SPAWN_Z, CAR_Z_SPACING)
            lastEnemyLane = lane
            val model = CarModel.entries[Random.nextInt(CarModel.entries.size)]
            val color = CarColor.ALL[Random.nextInt(CarColor.ALL.size)]
            movedCars.add(
                Car(
                    model = model,
                    color = color,
                    lane = lane,
                    worldZ = SPAWN_Z + Random.nextFloat() * 80f,
                    speed = newSpeed * (0.4f + Random.nextFloat() * 0.4f)
                )
            )
        }

        // Spawn coins (ULTRA: more coins)
        spawnTimerCoin += dt
        val coinMultiplier = if (preset == GraphicsPreset.ULTRA) 1.5f else 1f
        val coinSpawnInterval = ((2.0f / (newSpeed / BASE_SPEED)) / coinMultiplier).coerceAtLeast(0.4f)
        if (spawnTimerCoin >= coinSpawnInterval && movedCoins.size < MAX_COINS) {
            spawnTimerCoin = 0f
            val lane = generateUniqueLane(lastCoinLane, movedCoins.map { it.lane }, SPAWN_Z, COIN_Z_SPACING)
            lastCoinLane = lane
            movedCoins.add(
                Coin(
                    lane = lane,
                    worldZ = SPAWN_Z + Random.nextFloat() * 60f
                )
            )
        }

        // Check collisions with enemy cars
        val playerLane = state.playerLaneProgress
        var crashDetected = false
        for (car in movedCars) {
            val enemyLaneF = car.lane.toFloat()
            val laneDiff = abs(playerLane - enemyLaneF)
            val zDiff = abs(car.worldZ - PLAYER_Z)
            if (laneDiff < 0.7f && zDiff < 25f) {
                crashDetected = true
                break
            }
        }

        // Check coin collection
        var newCoinCount = state.coinCount
        var newScore = state.score
        for (coin in movedCoins) {
            if (coin.collected) continue
            val laneDiff = abs(playerLane - coin.lane.toFloat())
            val zDiff = abs(coin.worldZ - PLAYER_Z)
            if (laneDiff < 0.8f && zDiff < 20f) {
                coin.collected = true
                newCoinCount++
                newScore += COIN_VALUE
            }
        }

        // Distance-based score
        val newDistance = state.distanceTraveled + speed * 40f * dt
        newScore += (speed * dt * 2).toInt()

        if (crashDetected) {
            val crashParticles = generateCrashParticles(state, preset)
            _uiState.update {
                it.copy(
                    gameState = GameState.CRASHING,
                    crashAnimationTime = 0f,
                    particles = crashParticles,
                    enemyCars = movedCars.toList(),
                    coins = movedCoins.toList(),
                    speed = newSpeed,
                    distanceTraveled = newDistance,
                    roadScroll = newRoadScroll,
                    coinCount = newCoinCount,
                    score = newScore
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    enemyCars = movedCars.toList(),
                    coins = movedCoins.toList(),
                    score = newScore,
                    coinCount = newCoinCount,
                    speed = newSpeed,
                    distanceTraveled = newDistance,
                    elapsedSeconds = speedTimer.toInt(),
                    roadScroll = newRoadScroll,
                    totalFrames = frameCount.toLong()
                )
            }
        }
    }

    private fun handleCrashAnimation(state: GameUiState, dt: Float, preset: GraphicsPreset) {
        val newTime = state.crashAnimationTime + dt
        val newParticles = state.particles.mapNotNull { p ->
            val newLife = p.life - dt
            if (newLife <= 0f) null
            else p.copy(
                x = p.x + p.vx * dt,
                y = p.y + p.vy * dt,
                vy = p.vy + 200f * dt,
                life = newLife
            )
        }.toMutableList()

        // Add more particles during crash
        val particleRate = when {
            preset == GraphicsPreset.ULTRA -> 2
            preset.particleMultiplier >= 2f -> 3
            else -> 5
        }
        if (newTime < 0.8f && frameCount % particleRate == 0) {
            val burstCount = (3 * preset.particleMultiplier).toInt().coerceAtLeast(1)
            repeat(burstCount) {
                val angle = Random.nextFloat() * 6.2832f
                val spd = 50f + Random.nextFloat() * 300f
                newParticles.add(
                    Particle(
                        x = 0f,
                        y = 0f,
                        vx = cos(angle) * spd,
                        vy = sin(angle) * spd - 100f,
                        life = 0.4f + Random.nextFloat() * 0.6f,
                        maxLife = 1f,
                        color = Color(
                            red = Random.nextFloat(),
                            green = Random.nextFloat() * 0.5f,
                            blue = Random.nextFloat() * 0.8f + 0.2f
                        ),
                        size = 2f + Random.nextFloat() * 6f * preset.particleMultiplier
                    )
                )
            }
        }

        if (newTime >= 2.0f) {
            val finalScore = state.score
            val newHighScore = maxOf(finalScore, state.highScore)
            _uiState.update {
                it.copy(
                    gameState = GameState.GAME_OVER,
                    particles = emptyList(),
                    crashAnimationTime = 0f,
                    highScore = newHighScore
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    crashAnimationTime = newTime,
                    particles = newParticles
                )
            }
        }
    }

    private fun generateCrashParticles(state: GameUiState, preset: GraphicsPreset): List<Particle> {
        val particles = mutableListOf<Particle>()
        val mult = preset.particleMultiplier
        val baseCount = (40 * mult).toInt().coerceAtLeast(20)
        val sparkCount = (20 * mult).toInt().coerceAtLeast(10)

        repeat(baseCount) {
            val angle = Random.nextFloat() * 6.2832f
            val spd = 100f + Random.nextFloat() * 400f * mult
            particles.add(
                Particle(
                    x = 0f,
                    y = 0f,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd - 200f,
                    life = 0.5f + Random.nextFloat() * 1.5f,
                    maxLife = 2f,
                    color = Color(
                        red = 1f,
                        green = 0.2f + Random.nextFloat() * 0.6f,
                        blue = Random.nextFloat() * 0.3f
                    ),
                    size = 3f + Random.nextFloat() * 8f * mult
                )
            )
        }

        // Spark particles
        repeat(sparkCount) {
            val angle = Random.nextFloat() * 6.2832f
            val spd = 200f + Random.nextFloat() * 500f * mult
            particles.add(
                Particle(
                    x = 0f,
                    y = 0f,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd - 300f,
                    life = 0.2f + Random.nextFloat() * 0.4f,
                    maxLife = 0.6f,
                    color = Color(1f, 1f, 0.2f + Random.nextFloat() * 0.8f),
                    size = 1f + Random.nextFloat() * 3f * mult
                )
            )
        }

        // ULTRA: neon shard particles
        if (preset == GraphicsPreset.ULTRA) {
            repeat(15) {
                val angle = Random.nextFloat() * 6.2832f
                val spd = 300f + Random.nextFloat() * 600f
                particles.add(
                    Particle(
                        x = 0f,
                        y = 0f,
                        vx = cos(angle) * spd,
                        vy = sin(angle) * spd - 400f,
                        life = 0.3f + Random.nextFloat() * 0.8f,
                        maxLife = 1.1f,
                        color = state.playerCar.color.neonColor,
                        size = 2f + Random.nextFloat() * 5f
                    )
                )
            }
        }

        return particles
    }

    private fun generateUniqueLane(
        lastLane: Int,
        occupiedLanes: List<Int>,
        spawnZ: Float,
        minDistance: Float
    ): Int {
        val candidates = (0 until LANE_COUNT).toMutableList()
        candidates.shuffled()
        for (lane in candidates) {
            if (lane != lastLane) return lane
        }
        return Random.nextInt(LANE_COUNT)
    }
}
