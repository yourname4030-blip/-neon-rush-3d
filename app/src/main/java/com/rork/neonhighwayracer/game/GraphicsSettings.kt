package com.rork.neonhighwayracer.game

import android.content.Context
import android.content.SharedPreferences

enum class GraphicsPreset(
    val label: String,
    val description: String,
    val targetFps: Int,
    val renderScale: Float,
    val particleMultiplier: Float,
    val shadowQuality: Int,
    val bloomEnabled: Boolean,
    val ssrEnabled: Boolean,
    val motionBlurEnabled: Boolean,
    val volumetricClouds: Boolean,
    val dynamicWeather: Boolean,
    val roadReflectionDetail: Int,
    val neonGlowQuality: Int,
    val maxDrawDistance: Float,
    val antiAliasing: Boolean,
    val textureResolution: Int // 0=low, 1=med, 2=high, 3=ultra(8K)
) {
    ULTRA(
        label = "ULTRA",
        description = "8K Textures · SSR · RT Shadows · PBR · Bloom · Motion Blur · Volumetric Clouds",
        targetFps = 60,
        renderScale = 1.5f,
        particleMultiplier = 3.0f,
        shadowQuality = 3,
        bloomEnabled = true,
        ssrEnabled = true,
        motionBlurEnabled = true,
        volumetricClouds = true,
        dynamicWeather = true,
        roadReflectionDetail = 3,
        neonGlowQuality = 3,
        maxDrawDistance = 600f,
        antiAliasing = true,
        textureResolution = 3
    ),
    HIGH(
        label = "HIGH",
        description = "1080p · Shadows · Bloom · High Draw Distance",
        targetFps = 60,
        renderScale = 1.0f,
        particleMultiplier = 2.0f,
        shadowQuality = 2,
        bloomEnabled = true,
        ssrEnabled = false,
        motionBlurEnabled = true,
        volumetricClouds = false,
        dynamicWeather = false,
        roadReflectionDetail = 2,
        neonGlowQuality = 2,
        maxDrawDistance = 500f,
        antiAliasing = true,
        textureResolution = 2
    ),
    MEDIUM(
        label = "MEDIUM",
        description = "720p · Basic Shadows · Standard Effects",
        targetFps = 45,
        renderScale = 0.75f,
        particleMultiplier = 1.0f,
        shadowQuality = 1,
        bloomEnabled = false,
        ssrEnabled = false,
        motionBlurEnabled = false,
        volumetricClouds = false,
        dynamicWeather = false,
        roadReflectionDetail = 1,
        neonGlowQuality = 1,
        maxDrawDistance = 400f,
        antiAliasing = false,
        textureResolution = 1
    ),
    LOW(
        label = "LOW",
        description = "480p · Minimal Effects · Battery Saver",
        targetFps = 30,
        renderScale = 0.5f,
        particleMultiplier = 0.5f,
        shadowQuality = 0,
        bloomEnabled = false,
        ssrEnabled = false,
        motionBlurEnabled = false,
        volumetricClouds = false,
        dynamicWeather = false,
        roadReflectionDetail = 0,
        neonGlowQuality = 0,
        maxDrawDistance = 300f,
        antiAliasing = false,
        textureResolution = 0
    );

    companion object {
        private const val PREFS_NAME = "neon_highway_graphics"
        private const val KEY_PRESET = "graphics_preset"
        private const val KEY_ASSETS_DOWNLOADED = "ultra_assets_downloaded"
        private const val KEY_FIRST_LAUNCH = "first_launch_done"
        private const val KEY_OVERHEAT_WARNING_SHOWN = "overheat_warning_shown"

        fun load(context: Context): GraphicsPreset {
            val prefs = getPrefs(context)
            val name = prefs.getString(KEY_PRESET, ULTRA.name) ?: ULTRA.name
            return try { valueOf(name) } catch (_: Exception) { ULTRA }
        }

        fun save(context: Context, preset: GraphicsPreset) {
            getPrefs(context).edit().putString(KEY_PRESET, preset.name).apply()
        }

        fun isAssetsDownloaded(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_ASSETS_DOWNLOADED, false)
        }

        fun setAssetsDownloaded(context: Context, downloaded: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_ASSETS_DOWNLOADED, downloaded).apply()
        }

        fun isFirstLaunch(context: Context): Boolean {
            return !getPrefs(context).getBoolean(KEY_FIRST_LAUNCH, false)
        }

        fun markFirstLaunchDone(context: Context) {
            getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
        }

        fun isOverheatWarningShown(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_OVERHEAT_WARNING_SHOWN, false)
        }

        fun markOverheatWarningShown(context: Context) {
            getPrefs(context).edit().putBoolean(KEY_OVERHEAT_WARNING_SHOWN, true).apply()
        }

        private fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
}

enum class WeatherType(val label: String) {
    CLEAR("Clear Sky"),
    CLOUDY("Overcast"),
    RAIN("Heavy Rain"),
    STORM("Thunderstorm"),
    FOG("Dense Fog"),
    SUNSET("Neon Sunset");

    companion object {
        fun random(): WeatherType = entries[(System.nanoTime() % entries.size).toInt()]
    }
}

data class WeatherState(
    val current: WeatherType = WeatherType.CLEAR,
    val transitionProgress: Float = 1f,
    val next: WeatherType = WeatherType.CLEAR,
    val rainIntensity: Float = 0f,
    val fogDensity: Float = 0f,
    val lightningTimer: Float = 0f,
    val lightningAlpha: Float = 0f,
    val cloudCoverage: Float = 0f,
    val windAngle: Float = 0f,
    val elapsed: Float = 0f
)
