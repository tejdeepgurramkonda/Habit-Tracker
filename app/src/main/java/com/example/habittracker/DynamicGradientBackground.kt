package com.example.habittracker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime

// Time periods for India timezone
private enum class TimePeriod {
    MORNING,    // 5 AM - 12 PM
    AFTERNOON,  // 12 PM - 5 PM
    EVENING,    // 5 PM - 9 PM
    NIGHT       // 9 PM - 5 AM
}

// Enhanced color schemes for each time period including card and content colors
data class TimeBasedColors(
    val backgroundTopColor: Color,
    val backgroundBottomColor: Color,
    val cardBackgroundColor: Color,
    val cardContentColor: Color,
    val textPrimaryColor: Color,
    val textSecondaryColor: Color,
    val iconTintColor: Color
)

// CompositionLocal to provide time-based colors throughout the app
val LocalTimeBasedColors = compositionLocalOf<TimeBasedColors> {
    error("No TimeBasedColors provided")
}

@Composable
fun DynamicGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // State to track current time period
    var currentTimePeriod by remember { mutableStateOf(getCurrentTimePeriod()) }

    // Update time period every minute
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000) // Check every minute
            currentTimePeriod = getCurrentTimePeriod()
        }
    }

    // Define comprehensive color schemes for each time period
    val timeBasedColors = when (currentTimePeriod) {
        TimePeriod.MORNING -> TimeBasedColors(
            backgroundTopColor = Color(0xFFFFE0B2),    // Soft peach
            backgroundBottomColor = Color(0xFFFFCC80),  // Light orange
            cardBackgroundColor = Color(0xFFFFF8E1),    // Very light cream
            cardContentColor = Color(0xFFFF8F00),       // Warm orange
            textPrimaryColor = Color(0xFF5D4037),       // Warm brown
            textSecondaryColor = Color(0xFF8D6E63),     // Light brown
            iconTintColor = Color(0xFFFF8F00)           // Warm orange
        )
        TimePeriod.AFTERNOON -> TimeBasedColors(
            backgroundTopColor = Color(0xFF81D4FA),     // Light sky blue
            backgroundBottomColor = Color(0xFF29B6F6),  // Bright blue
            cardBackgroundColor = Color(0xFFE3F2FD),    // Very light blue
            cardContentColor = Color(0xFF1976D2),       // Blue
            textPrimaryColor = Color(0xFF0D47A1),       // Dark blue
            textSecondaryColor = Color(0xFF1565C0),     // Medium blue
            iconTintColor = Color(0xFF2196F3)           // Blue
        )
        TimePeriod.EVENING -> TimeBasedColors(
            backgroundTopColor = Color(0xFFCE93D8),     // Light purple
            backgroundBottomColor = Color(0xFF9C27B0),  // Deep purple
            cardBackgroundColor = Color(0xFFF3E5F5),    // Very light purple
            cardContentColor = Color(0xFF7B1FA2),       // Purple
            textPrimaryColor = Color(0xFF4A148C),       // Dark purple
            textSecondaryColor = Color(0xFF6A1B9A),     // Medium purple
            iconTintColor = Color(0xFF9C27B0)           // Purple
        )
        TimePeriod.NIGHT -> TimeBasedColors(
            backgroundTopColor = Color(0xFF424242),     // Dark grey
            backgroundBottomColor = Color(0xFF212121),  // Very dark grey
            cardBackgroundColor = Color(0xFF303030),    // Dark card background
            cardContentColor = Color(0xFF757575),       // Light grey
            textPrimaryColor = Color(0xFFE0E0E0),       // Light text
            textSecondaryColor = Color(0xFFBDBDBD),     // Medium light text
            iconTintColor = Color(0xFF90A4AE)           // Blue grey
        )
    }

    // Animate all colors smoothly
    val animatedBackgroundTopColor by animateColorAsState(
        targetValue = timeBasedColors.backgroundTopColor,
        animationSpec = tween(durationMillis = 2000),
        label = "backgroundTopColorAnimation"
    )

    val animatedBackgroundBottomColor by animateColorAsState(
        targetValue = timeBasedColors.backgroundBottomColor,
        animationSpec = tween(durationMillis = 2000),
        label = "backgroundBottomColorAnimation"
    )

    val animatedCardBackgroundColor by animateColorAsState(
        targetValue = timeBasedColors.cardBackgroundColor,
        animationSpec = tween(durationMillis = 2000),
        label = "cardBackgroundColorAnimation"
    )

    val animatedCardContentColor by animateColorAsState(
        targetValue = timeBasedColors.cardContentColor,
        animationSpec = tween(durationMillis = 2000),
        label = "cardContentColorAnimation"
    )

    val animatedTextPrimaryColor by animateColorAsState(
        targetValue = timeBasedColors.textPrimaryColor,
        animationSpec = tween(durationMillis = 2000),
        label = "textPrimaryColorAnimation"
    )

    val animatedTextSecondaryColor by animateColorAsState(
        targetValue = timeBasedColors.textSecondaryColor,
        animationSpec = tween(durationMillis = 2000),
        label = "textSecondaryColorAnimation"
    )

    val animatedIconTintColor by animateColorAsState(
        targetValue = timeBasedColors.iconTintColor,
        animationSpec = tween(durationMillis = 2000),
        label = "iconTintColorAnimation"
    )

    // Create animated color scheme
    val animatedTimeBasedColors = TimeBasedColors(
        backgroundTopColor = animatedBackgroundTopColor,
        backgroundBottomColor = animatedBackgroundBottomColor,
        cardBackgroundColor = animatedCardBackgroundColor,
        cardContentColor = animatedCardContentColor,
        textPrimaryColor = animatedTextPrimaryColor,
        textSecondaryColor = animatedTextSecondaryColor,
        iconTintColor = animatedIconTintColor
    )

    CompositionLocalProvider(LocalTimeBasedColors provides animatedTimeBasedColors) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(animatedBackgroundTopColor, animatedBackgroundBottomColor),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            content()
        }
    }
}

// Helper function to get current time period based on India timezone
private fun getCurrentTimePeriod(): TimePeriod {
    val indiaZone = ZoneId.of("Asia/Kolkata")
    val currentTime = ZonedDateTime.now(indiaZone).toLocalTime()
    val hour = currentTime.hour

    return when (hour) {
        in 5..11 -> TimePeriod.MORNING     // 5 AM - 11:59 AM
        in 12..16 -> TimePeriod.AFTERNOON  // 12 PM - 4:59 PM
        in 17..20 -> TimePeriod.EVENING    // 5 PM - 8:59 PM
        else -> TimePeriod.NIGHT           // 9 PM - 4:59 AM
    }
}

// Extension function to get readable time period name (for debugging/display)
fun getCurrentTimePeriodName(): String {
    return when (getCurrentTimePeriod()) {
        TimePeriod.MORNING -> "Morning"
        TimePeriod.AFTERNOON -> "Afternoon"
        TimePeriod.EVENING -> "Evening"
        TimePeriod.NIGHT -> "Night"
    }
}
