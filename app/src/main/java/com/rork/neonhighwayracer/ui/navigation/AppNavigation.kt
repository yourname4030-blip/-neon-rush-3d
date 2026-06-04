package com.rork.neonhighwayracer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rork.neonhighwayracer.game.GraphicsPreset
import com.rork.neonhighwayracer.ui.screens.AssetDownloadScreen
import com.rork.neonhighwayracer.ui.screens.GameScreen
import com.rork.neonhighwayracer.ui.screens.HomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var isFirstLaunch by remember { mutableStateOf(true) }
    var needsAssetDownload by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        needsAssetDownload = !GraphicsPreset.isAssetsDownloaded(context)
    }

    val startDestination = if (needsAssetDownload) "asset_download" else "home"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("asset_download") {
            AssetDownloadScreen(
                onComplete = {
                    navController.navigate("home") {
                        popUpTo("asset_download") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onStartGame = {
                    navController.navigate("game") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("game") {
            GameScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
