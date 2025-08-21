package com.example.habittracker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.util.*

data class TimeBasedColors(
    val cardBackgroundColor: Color,
    val cardContentColor: Color,
    val textPrimaryColor: Color,
    val textSecondaryColor: Color
)

val LocalTimeBasedColors = compositionLocalOf<TimeBasedColors> {
    error("TimeBasedColors not provided")
}

@Composable
fun DynamicGradientBackground(
    content: @Composable () -> Unit
) {
    val currentTime = Calendar.getInstance()
    val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)

    // Define time periods for India timezone
    val timeBasedColors = when (currentHour) {
        in 5..11 -> { // Morning: 5 AM - 12 PM
            TimeBasedColors(
                cardBackgroundColor = Color(0xFFFFF8E1).copy(alpha = 0.9f),
                cardContentColor = Color(0xFFFF8A50),
                textPrimaryColor = Color(0xFF2E2E2E),
                textSecondaryColor = Color(0xFF6E6E6E)
            )
        }
        in 12..16 -> { // Afternoon: 12 PM - 5 PM
            TimeBasedColors(
                cardBackgroundColor = Color(0xFFE3F2FD).copy(alpha = 0.9f),
                cardContentColor = Color(0xFF42A5F5),
                textPrimaryColor = Color(0xFF1A1A1A),
                textSecondaryColor = Color(0xFF5A5A5A)
            )
        }
        in 17..20 -> { // Evening: 5 PM - 9 PM
            TimeBasedColors(
                cardBackgroundColor = Color(0xFFF3E5F5).copy(alpha = 0.9f),
                cardContentColor = Color(0xFF8E24AA),
                textPrimaryColor = Color(0xFF2A2A2A),
                textSecondaryColor = Color(0xFF6A6A6A)
            )
        }
        else -> { // Night: 9 PM - 5 AM
            TimeBasedColors(
                cardBackgroundColor = Color(0xFF263238).copy(alpha = 0.9f),
                cardContentColor = Color(0xFF4FC3F7),
                textPrimaryColor = Color(0xFFE0E0E0),
                textSecondaryColor = Color(0xFFB0B0B0)
            )
        }
    }

    // Animated gradient colors
    val primaryGradientColor by animateColorAsState(
        targetValue = when (currentHour) {
            in 5..11 -> Color(0xFFFFF9C4)    // Light yellow for morning
            in 12..16 -> Color(0xFFE1F5FE)   // Light blue for afternoon
            in 17..20 -> Color(0xFFF8BBD9)   // Light pink for evening
            else -> Color(0xFF1A237E)        // Dark blue for night
        },
        animationSpec = tween(durationMillis = 2000),
        label = "primary_gradient"
    )

    val secondaryGradientColor by animateColorAsState(
        targetValue = when (currentHour) {
            in 5..11 -> Color(0xFFFFE0B2)    // Light orange for morning
            in 12..16 -> Color(0xFFBBDEFB)   // Medium blue for afternoon
            in 17..20 -> Color(0xFFE1BEE7)   // Light purple for evening
            else -> Color(0xFF0D47A1)        // Darker blue for night
        },
        animationSpec = tween(durationMillis = 2000),
        label = "secondary_gradient"
    )

    CompositionLocalProvider(LocalTimeBasedColors provides timeBasedColors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryGradientColor,
                            secondaryGradientColor
                        )
                    )
                )
        ) {
            content()
        }
    }
}
