package com.rork.neonhighwayracer.game

import androidx.compose.ui.graphics.Color

const val LANE_COUNT = 5
const val MAX_ENEMY_CARS = 8
const val MAX_COINS = 6
const val COIN_VALUE = 10
const val CAR_Z_SPACING = 60f
const val COIN_Z_SPACING = 50f
const val BASE_SPEED = 3f
const val SPEED_INCREMENT = 0.5f
const val SPEED_INTERVAL_SECONDS = 30
const val PLAYER_Z = 0f
const val SPAWN_Z = 400f
const val DESPAWN_Z = -10f
const val LANE_CHANGE_SPEED = 0.15f

enum class CarModel(val displayName: String) {
    SEDAN("Sedan"),
    SPORTS("Sports"),
    SUV("SUV"),
    TRUCK("Truck"),
    HATCHBACK("Hatchback"),
    COUPE("Coupe"),
    CONVERTIBLE("Convertible"),
    PICKUP("Pickup"),
    VAN("Van"),
    MUSCLE("Muscle"),
    SUPERCAR("Supercar"),
    CLASSIC("Classic"),
    OFFROAD("Off-Road"),
    LIMO("Limo"),
    RACER("Racer")
}

data class CarColor(
    val name: String,
    val bodyColor: Color,
    val neonColor: Color
) {
    companion object {
        val ALL: List<CarColor> = listOf(
            CarColor("Red", Color(0xFFE53935), Color(0xFFFF5252)),
            CarColor("Blue", Color(0xFF1E88E5), Color(0xFF448AFF)),
            CarColor("Green", Color(0xFF43A047), Color(0xFF69F0AE)),
            CarColor("Yellow", Color(0xFFFDD835), Color(0xFFFFFF00)),
            CarColor("Orange", Color(0xFFFB8C00), Color(0xFFFFAB40)),
            CarColor("Purple", Color(0xFF8E24AA), Color(0xFFE040FB)),
            CarColor("Cyan", Color(0xFF00ACC1), Color(0xFF18FFFF)),
            CarColor("Pink", Color(0xFFD81B60), Color(0xFFFF80AB)),
            CarColor("White", Color(0xFFF5F5F5), Color(0xFFFFFFFF)),
            CarColor("Silver", Color(0xFF90A4AE), Color(0xFFCFD8DC)),
            CarColor("Gold", Color(0xFFFFB300), Color(0xFFFFD740)),
            CarColor("Lime", Color(0xFFC0CA33), Color(0xFFEEFF41)),
            CarColor("Magenta", Color(0xFFD81B60), Color(0xFFFF4081)),
            CarColor("Teal", Color(0xFF00897B), Color(0xFF64FFDA)),
            CarColor("Coral", Color(0xFFFF6E40), Color(0xFFFF9E80))
        )
    }
}

data class Car(
    val model: CarModel,
    val color: CarColor,
    var lane: Int,
    var worldZ: Float,
    var speed: Float = 0f,
    val isPlayer: Boolean = false
)

data class Coin(
    var lane: Int,
    var worldZ: Float,
    var collected: Boolean = false,
    val bobOffset: Float = (0..628).random() / 100f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    var color: Color,
    var size: Float
)

enum class GameState {
    MENU,
    PLAYING,
    CRASHING,
    GAME_OVER
}

data class Raindrop(
    var x: Float,
    var y: Float,
    var z: Float,
    var speed: Float,
    var length: Float
)

data class MotionTrail(
    var worldX: Float,
    var worldZ: Float,
    var age: Float,
    val bodyColor: androidx.compose.ui.graphics.Color,
    val neonColor: androidx.compose.ui.graphics.Color
)

data class GameUiState(
    val gameState: GameState = GameState.MENU,
    val playerCar: Car = Car(
        model = CarModel.SPORTS,
        color = CarColor.ALL[0],
        lane = 2,
        worldZ = PLAYER_Z,
        isPlayer = true
    ),
    val enemyCars: List<Car> = emptyList(),
    val coins: List<Coin> = emptyList(),
    val score: Int = 0,
    val coinCount: Int = 0,
    val speed: Float = BASE_SPEED,
    val distanceTraveled: Float = 0f,
    val elapsedSeconds: Int = 0,
    val playerLaneProgress: Float = 2f,
    val crashAnimationTime: Float = 0f,
    val particles: List<Particle> = emptyList(),
    val roadScroll: Float = 0f,
    val selectedCarModel: CarModel = CarModel.SPORTS,
    val selectedCarColor: CarColor = CarColor.ALL[0],
    val highScore: Int = 0,
    val graphicsPreset: GraphicsPreset = GraphicsPreset.ULTRA,
    val weatherState: WeatherState = WeatherState(),
    val raindrops: List<Raindrop> = emptyList(),
    val motionTrails: List<MotionTrail> = emptyList(),
    val totalFrames: Long = 0,
    val fpsCounter: Int = 60
)
