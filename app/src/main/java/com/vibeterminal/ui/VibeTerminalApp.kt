package com.vibeterminal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vibeterminal.ui.terminal.TerminalScreen
import com.vibeterminal.ui.settings.SettingsScreen
import com.vibeterminal.ui.vscode.VSCodeLayout

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Terminal : Screen("terminal", "Terminal", Icons.Default.Terminal)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VibeTerminalApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Z Fold6 adaptive layout
    val activity = LocalContext.current as? android.app.Activity
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }
    val isExpandedScreen = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

    Scaffold(
        // Bottom bar removed - using left sidebar only
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Navigation rail for expanded screens (Z Fold6 unfolded)
            if (isExpandedScreen) {
                NavigationRail {
                    Spacer(Modifier.weight(1f))
                    listOf(Screen.Terminal, Screen.Settings).forEach { screen ->
                        NavigationRailItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            // Main content
            NavHost(
                navController = navController,
                startDestination = Screen.Terminal.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Terminal.route) {
                    // Use VS Code-style layout for terminal screen
                    VSCodeLayout()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }
    }
}
