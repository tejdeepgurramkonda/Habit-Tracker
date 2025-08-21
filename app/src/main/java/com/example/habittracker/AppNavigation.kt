package com.example.habittracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppNavigation() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val habitViewModel: HabitViewModel = hiltViewModel()

    // Wrap everything in DynamicGradientBackground to provide TimeBasedColors
    DynamicGradientBackground {
        Scaffold(
            containerColor = Color.Transparent, // Make scaffold transparent to show gradient
            bottomBar = {
                NavigationFooterBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onAddHabitClick = { habitViewModel.showAddDialog() }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> HabitDashboardScreen(habitViewModel = habitViewModel)
                    1 -> TimerScreen()
                    2 -> StatisticsScreen()
                    3 -> ProfileScreen()
                }
            }
        }
    }
}
