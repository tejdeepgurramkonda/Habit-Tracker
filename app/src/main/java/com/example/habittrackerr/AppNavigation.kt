package com.example.habittrackerr

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.habittrackerr.auth.AuthViewModel
import com.example.habittrackerr.auth.LoginScreen
import kotlinx.coroutines.delay

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Add comprehensive logging for authentication state
    LaunchedEffect(authState.isAuthenticated, currentUser) {
        println("AppNavigation: Authentication state changed to: ${authState.isAuthenticated}")
        println("AppNavigation: Current user: ${currentUser?.email ?: "None"}")
        println("AppNavigation: Auth loading: ${authState.isLoading}")
        println("AppNavigation: Auth error: ${authState.error?.message ?: "None"}")
    }

    // Reduce loading screen time - only show loading for the first 1 second or until auth state is determined
    var showLoadingScreen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1000) // Maximum 1 second loading (reduced from 2 seconds)
        showLoadingScreen = false
    }

    LaunchedEffect(authState.isAuthenticated, currentUser) {
        // Stop loading immediately when authentication state is determined
        if (!authState.isLoading) {
            showLoadingScreen = false
        }
    }

    // Show loading screen only briefly while authentication is being determined
    if (authState.isLoading && showLoadingScreen) {
        AuthLoadingScreen()
        return
    }

    // Authentication routing
    when {
        authState.isAuthenticated && currentUser != null -> {
            // User is fully authenticated and user data is available
            AuthenticatedAppNavigation(authViewModel = authViewModel)
        }
        authState.requiresMfa -> {
            // User needs to complete MFA
            MfaVerificationScreen(authViewModel = authViewModel)
        }
        authState.user?.isEmailVerified == false -> {
            // User exists but email is not verified
            EmailVerificationScreen(authViewModel = authViewModel)
        }
        else -> {
            // User is not authenticated, show login/signup
            UnauthenticatedNavigation(authViewModel = authViewModel)
        }
    }
}

@Composable
private fun AuthLoadingScreen() {
    DynamicGradientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Checking authentication...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalTimeBasedColors.current.textSecondaryColor
                )
            }
        }
    }
}

@Composable
private fun UnauthenticatedNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "auth"
    ) {
        composable("auth") {
            LoginScreen(
                viewModel = authViewModel,
                navController = navController
            )
        }
    }
}

@Composable
private fun MfaVerificationScreen(authViewModel: AuthViewModel) {
    // TODO: Implement MFA verification screen
    DynamicGradientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Multi-Factor Authentication Required",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { authViewModel.navigateBack() }
                ) {
                    Text("Back to Login")
                }
            }
        }
    }
}

@Composable
private fun EmailVerificationScreen(authViewModel: AuthViewModel) {
    // TODO: Implement email verification screen
    DynamicGradientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Email Verification Required",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { authViewModel.resendEmailVerification() }
                ) {
                    Text("Resend Verification Email")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { authViewModel.checkEmailVerification() }
                ) {
                    Text("I've Verified My Email")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { authViewModel.signOut() }
                ) {
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedAppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }
    val habitViewModel: HabitViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Session timeout monitoring
    val authState by authViewModel.authState.collectAsState()
    LaunchedEffect(authState.sessionExpiresAt) {
        authState.sessionExpiresAt?.let { expiresAt ->
            val now = java.time.LocalDateTime.now()
            if (now.isAfter(expiresAt)) {
                // Session expired, sign out user
                authViewModel.signOut()
            }
        }
    }

    // Security monitoring - check for suspicious activity
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // Check every minute
            // Check for security issues
            if (authState.securityFlags.any { it.suspiciousActivity }) {
                // Handle suspicious activity - could show warning dialog
                println("Security Alert: Suspicious activity detected")
            }
        }
    }

    // Wrap everything in DynamicGradientBackground to provide TimeBasedColors
    DynamicGradientBackground {
        Scaffold(
            containerColor = Color.Transparent, // Make scaffold transparent to show gradient
            bottomBar = {
                NavigationFooterBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onAddHabitClick = { navController.navigate("add_habit") }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = when (selectedTab) {
                    0 -> "habits"
                    1 -> "timer"
                    2 -> "statistics"
                    3 -> "profile"
                    else -> "habits"
                },
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("habits") {
                    HabitDashboardScreen(
                        habitViewModel = habitViewModel
                    )
                }
                composable("timer") {
                    TimerScreen()
                }
                composable("statistics") {
                    StatisticsScreen(
                        habitViewModel = habitViewModel
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        navController = navController,
                        habitViewModel = habitViewModel
                    )
                }
                composable("add_habit") {
                    AddHabitScreen(
                        navController = navController,
                        habitViewModel = habitViewModel
                    )
                }
                composable("edit_habit/{habitId}") { backStackEntry ->
                    val habitId = backStackEntry.arguments?.getString("habitId")?.toIntOrNull()
                    if (habitId != null) {
                        EditHabitScreen(
                            habitId = habitId,
                            navController = navController,
                            habitViewModel = habitViewModel
                        )
                    }
                }
                composable("habit_detail/{habitId}") { backStackEntry ->
                    val habitId = backStackEntry.arguments?.getString("habitId")?.toIntOrNull()
                    if (habitId != null) {
                        HabitDetailScreen(
                            habitId = habitId,
                            navController = navController,
                            habitViewModel = habitViewModel
                        )
                    }
                }
                composable("security_settings") {
                    SecuritySettingsScreen(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }
            }

            // Handle tab changes - Fix navigation synchronization
            LaunchedEffect(selectedTab) {
                val targetRoute = when (selectedTab) {
                    0 -> "habits"
                    1 -> "timer"
                    2 -> "statistics"
                    3 -> "profile"
                    else -> "habits"
                }

                // Only navigate if we're not already on the target route
                if (navController.currentDestination?.route != targetRoute) {
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }

        // Show session expiry warning
        authState.sessionExpiresAt?.let { expiresAt ->
            val now = java.time.LocalDateTime.now()
            val minutesLeft = java.time.Duration.between(now, expiresAt).toMinutes()

            if (minutesLeft in 1..5) {
                SessionExpiryWarning(
                    minutesLeft = minutesLeft.toInt(),
                    onExtendSession = {
                        // Refresh session
                        // This would typically involve re-authenticating
                    },
                    onSignOut = { authViewModel.signOut() }
                )
            }
        }
    }
}

@Composable
private fun SessionExpiryWarning(
    minutesLeft: Int,
    onExtendSession: () -> Unit,
    onSignOut: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text("Session Expiring Soon")
            },
            text = {
                Text("Your session will expire in $minutesLeft minute${if (minutesLeft != 1) "s" else ""}. Do you want to extend your session?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onExtendSession()
                        showDialog = false
                    }
                ) {
                    Text("Extend Session")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onSignOut()
                        showDialog = false
                    }
                ) {
                    Text("Sign Out")
                }
            }
        )
    }
}

@Composable
private fun SecuritySettingsScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    // TODO: Implement comprehensive security settings screen
    DynamicGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Security Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = LocalTimeBasedColors.current.textPrimaryColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Security options would go here
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}
