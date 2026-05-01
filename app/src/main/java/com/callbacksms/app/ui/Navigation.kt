package com.callbacksms.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.callbacksms.app.ui.screen.*
import com.callbacksms.app.viewmodel.MainViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home      : Screen("home",      "홈",     Icons.Default.Home)
    object Templates : Screen("templates", "템플릿", Icons.Default.Message)
    object History   : Screen("history",   "기록",   Icons.Default.History)
    object Settings  : Screen("settings",  "설정",   Icons.Default.Settings)
}

@Composable
fun Navigation(viewModel: MainViewModel, onRequestPermissions: () -> Unit) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Templates, Screen.History, Screen.Settings)
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = Screen.Home.route) {
            composable(Screen.Home.route)      { HomeScreen(viewModel, onRequestPermissions, padding) }
            composable(Screen.Templates.route) { TemplateScreen(viewModel, padding) }
            composable(Screen.History.route)   { HistoryScreen(viewModel, padding) }
            composable(Screen.Settings.route)  { SettingsScreen(viewModel, padding) }
        }
    }
}
