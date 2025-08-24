package com.example.habittrackerr

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun AddHabitScreen(
    navController: NavHostController,
    habitViewModel: HabitViewModel
) {
    // Use rememberSaveable for state persistence across recompositions
    var showDialog by rememberSaveable { mutableStateOf(true) }

    // Handle dismissal properly - only navigate back once when dialog is dismissed
    LaunchedEffect(showDialog) {
        if (!showDialog) {
            navController.popBackStack()
        }
    }

    if (showDialog) {
        AddEditHabitDialog(
            habitViewModel = habitViewModel,
            habit = null,
            onDismiss = {
                showDialog = false
                // Navigation handled by LaunchedEffect above to prevent race conditions
            }
        )
    }
}

@Composable
fun EditHabitScreen(
    habitId: Int,
    navController: NavHostController,
    habitViewModel: HabitViewModel
) {
    // Get the habit to edit
    val habits by habitViewModel.habits.collectAsState()
    val habit = habits.find { it.id == habitId }
    var showDialog by rememberSaveable { mutableStateOf(true) }

    // Handle dismissal properly - only navigate back once when dialog is dismissed
    LaunchedEffect(showDialog) {
        if (!showDialog) {
            navController.popBackStack()
        }
    }

    if (showDialog && habit != null) {
        AddEditHabitDialog(
            habitViewModel = habitViewModel,
            habit = habit,
            onDismiss = {
                showDialog = false
                // Navigation handled by LaunchedEffect above to prevent race conditions
            }
        )
    } else if (habit == null) {
        // If habit not found, navigate back immediately
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
    }
}

@Composable
fun HabitDetailScreen(
    habitId: Int,
    navController: NavHostController,
    habitViewModel: HabitViewModel
) {
    // Simple habit detail screen - can be expanded later
    DynamicGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Habit Details",
                style = MaterialTheme.typography.headlineMedium,
                color = LocalTimeBasedColors.current.textPrimaryColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Habit ID: $habitId",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalTimeBasedColors.current.textPrimaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}
