package com.example.habittrackerr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.habittrackerr.auth.AuthViewModel
import com.example.habittrackerr.ui.theme.HabitTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize authentication state monitoring
        initializeAuthState()

        setContent {
            HabitTrackerTheme {
                ResponsiveDesignProvider {
                    AppNavigation()
                }
            }
        }
    }

    /**
     * Initialize authentication state monitoring
     */
    private fun initializeAuthState() {
        lifecycleScope.launch {
            // Monitor authentication state changes
            authViewModel.authState.collect { authState ->
                // Handle authentication state changes if needed
                // This is where you could add analytics, logging, or other side effects
                when {
                    authState.isAuthenticated -> {
                        // User is authenticated - could trigger sync, analytics, etc.
                    }
                    authState.error != null -> {
                        // Handle authentication errors if needed
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh authentication state when app comes to foreground
        // This helps with session management
    }

    override fun onPause() {
        super.onPause()
        // Handle any cleanup when app goes to background
    }
}
