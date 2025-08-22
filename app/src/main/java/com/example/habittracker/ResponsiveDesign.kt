package com.example.habittracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Screen size categories
enum class ScreenSize {
    SMALL,   // < 600dp width
    MEDIUM,  // 600dp - 840dp width
    LARGE    // > 840dp width
}

// Responsive dimensions class
data class ResponsiveDimensions(
    val screenSize: ScreenSize,

    // Spacing
    val spacingXSmall: Dp,
    val spacingSmall: Dp,
    val spacingMedium: Dp,
    val spacingLarge: Dp,
    val spacingXLarge: Dp,

    // Component sizes
    val cardPadding: Dp,
    val cardCornerRadius: Dp,
    val iconSize: Dp,
    val iconButtonSize: Dp,
    val progressBarHeight: Dp,
    val progressBarRadius: Dp,
    val circularProgressSize: Dp,

    // Calendar
    val calendarItemSize: Dp,
    val calendarItemSpacing: Dp,

    // Habit card
    val habitCardMinHeight: Dp,
    val habitCardPadding: Dp,

    // Progress expansion
    val progressExpandedHeight: Dp,
    val progressCollapsedHeight: Dp,

    // Typography scaling
    val titleTextScale: Float,
    val bodyTextScale: Float,
    val captionTextScale: Float
)

// Create responsive dimensions based on screen size
@Composable
fun createResponsiveDimensions(): ResponsiveDimensions {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    val screenSize = when {
        screenWidthDp < 600.dp -> ScreenSize.SMALL
        screenWidthDp < 840.dp -> ScreenSize.MEDIUM
        else -> ScreenSize.LARGE
    }

    // Calculate scale factor based on screen width
    // Base design is for ~411dp width (typical phone)
    val baseWidth = 411.dp
    val scaleFactor = (screenWidthDp / baseWidth).coerceIn(0.8f, 1.4f)

    return when (screenSize) {
        ScreenSize.SMALL -> ResponsiveDimensions(
            screenSize = screenSize,
            spacingXSmall = (2.dp * scaleFactor),
            spacingSmall = (4.dp * scaleFactor),
            spacingMedium = (8.dp * scaleFactor),
            spacingLarge = (16.dp * scaleFactor),
            spacingXLarge = (24.dp * scaleFactor),
            cardPadding = (12.dp * scaleFactor),
            cardCornerRadius = (12.dp * scaleFactor),
            iconSize = (20.dp * scaleFactor),
            iconButtonSize = (36.dp * scaleFactor),
            progressBarHeight = (8.dp * scaleFactor),
            progressBarRadius = (4.dp * scaleFactor),
            circularProgressSize = (100.dp * scaleFactor),
            calendarItemSize = (36.dp * scaleFactor),
            calendarItemSpacing = (4.dp * scaleFactor),
            habitCardMinHeight = (64.dp * scaleFactor),
            habitCardPadding = (12.dp * scaleFactor),
            progressExpandedHeight = (280.dp * scaleFactor),
            progressCollapsedHeight = (70.dp * scaleFactor),
            titleTextScale = scaleFactor * 0.9f,
            bodyTextScale = scaleFactor * 0.95f,
            captionTextScale = scaleFactor
        )

        ScreenSize.MEDIUM -> ResponsiveDimensions(
            screenSize = screenSize,
            spacingXSmall = (3.dp * scaleFactor),
            spacingSmall = (6.dp * scaleFactor),
            spacingMedium = (12.dp * scaleFactor),
            spacingLarge = (20.dp * scaleFactor),
            spacingXLarge = (28.dp * scaleFactor),
            cardPadding = (16.dp * scaleFactor),
            cardCornerRadius = (16.dp * scaleFactor),
            iconSize = (24.dp * scaleFactor),
            iconButtonSize = (44.dp * scaleFactor),
            progressBarHeight = (10.dp * scaleFactor),
            progressBarRadius = (5.dp * scaleFactor),
            circularProgressSize = (120.dp * scaleFactor),
            calendarItemSize = (42.dp * scaleFactor),
            calendarItemSpacing = (6.dp * scaleFactor),
            habitCardMinHeight = (72.dp * scaleFactor),
            habitCardPadding = (16.dp * scaleFactor),
            progressExpandedHeight = (320.dp * scaleFactor),
            progressCollapsedHeight = (80.dp * scaleFactor),
            titleTextScale = scaleFactor,
            bodyTextScale = scaleFactor,
            captionTextScale = scaleFactor
        )

        ScreenSize.LARGE -> ResponsiveDimensions(
            screenSize = screenSize,
            spacingXSmall = (4.dp * scaleFactor),
            spacingSmall = (8.dp * scaleFactor),
            spacingMedium = (16.dp * scaleFactor),
            spacingLarge = (24.dp * scaleFactor),
            spacingXLarge = (32.dp * scaleFactor),
            cardPadding = (20.dp * scaleFactor),
            cardCornerRadius = (20.dp * scaleFactor),
            iconSize = (28.dp * scaleFactor),
            iconButtonSize = (52.dp * scaleFactor),
            progressBarHeight = (12.dp * scaleFactor),
            progressBarRadius = (6.dp * scaleFactor),
            circularProgressSize = (140.dp * scaleFactor),
            calendarItemSize = (48.dp * scaleFactor),
            calendarItemSpacing = (8.dp * scaleFactor),
            habitCardMinHeight = (80.dp * scaleFactor),
            habitCardPadding = (20.dp * scaleFactor),
            progressExpandedHeight = (360.dp * scaleFactor),
            progressCollapsedHeight = (90.dp * scaleFactor),
            titleTextScale = scaleFactor * 1.1f,
            bodyTextScale = scaleFactor * 1.05f,
            captionTextScale = scaleFactor
        )
    }
}

// Composition local for responsive dimensions
val LocalResponsiveDimensions = compositionLocalOf<ResponsiveDimensions> {
    error("ResponsiveDimensions not provided")
}

// Provider composable
@Composable
fun ResponsiveDesignProvider(
    content: @Composable () -> Unit
) {
    val dimensions = createResponsiveDimensions()

    CompositionLocalProvider(
        LocalResponsiveDimensions provides dimensions
    ) {
        content()
    }
}

// Helper extension functions for easy access
@Composable
fun Dp.responsive(): Dp {
    val dimensions = LocalResponsiveDimensions.current
    val scaleFactor = when (dimensions.screenSize) {
        ScreenSize.SMALL -> 0.9f
        ScreenSize.MEDIUM -> 1.0f
        ScreenSize.LARGE -> 1.1f
    }
    return this * scaleFactor
}
