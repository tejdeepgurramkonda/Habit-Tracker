package com.example.habittrackerr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NavigationFooterBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddHabitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalResponsiveDimensions.current
    val timeBasedColors = LocalTimeBasedColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = timeBasedColors.cardBackgroundColor.copy(alpha = 0.95f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = dimensions.cardCornerRadius,
            topEnd = dimensions.cardCornerRadius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensions.spacingLarge,
                    vertical = dimensions.spacingMedium
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Tab
            NavigationTabItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                dimensions = dimensions,
                timeBasedColors = timeBasedColors
            )

            // Timer Tab
            NavigationTabItem(
                icon = Icons.Default.Timer,
                label = "Timer",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                dimensions = dimensions,
                timeBasedColors = timeBasedColors
            )

            // Add Habit Button (Center)
            FloatingActionButton(
                onClick = onAddHabitClick,
                containerColor = timeBasedColors.cardContentColor,
                contentColor = Color.White,
                modifier = Modifier.size(dimensions.iconButtonSize + dimensions.spacingLarge),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Habit",
                    tint = Color.White,
                    modifier = Modifier.size(dimensions.iconSize + dimensions.spacingSmall)
                )
            }

            // Statistics Tab
            NavigationTabItem(
                icon = Icons.Outlined.BarChart,
                label = "Stats",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                dimensions = dimensions,
                timeBasedColors = timeBasedColors
            )

            // Profile Tab
            NavigationTabItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                dimensions = dimensions,
                timeBasedColors = timeBasedColors
            )
        }
    }
}

@Composable
private fun NavigationTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    dimensions: ResponsiveDimensions,
    timeBasedColors: TimeBasedColors
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(dimensions.spacingSmall)
            .width(dimensions.iconButtonSize + dimensions.spacingMedium)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(dimensions.iconButtonSize)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected)
                    timeBasedColors.cardContentColor
                else
                    timeBasedColors.textSecondaryColor,
                modifier = Modifier.size(dimensions.iconSize)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = MaterialTheme.typography.labelSmall.fontSize * dimensions.captionTextScale
            ),
            color = if (isSelected)
                timeBasedColors.cardContentColor
            else
                timeBasedColors.textSecondaryColor,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
