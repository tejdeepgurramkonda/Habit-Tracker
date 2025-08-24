package com.example.habittrackerr.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habittrackerr.LocalTimeBasedColors
import com.example.habittrackerr.stats.FitnessSummaryState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FitnessSummarySection(
    summaryState: FitnessSummaryState,
    onCardClick: (String) -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Summary",
                style = MaterialTheme.typography.titleLarge,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.SemiBold
            )

            if (summaryState.lastUpdated > 0) {
                Text(
                    text = "Updated ${formatTime(summaryState.lastUpdated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = timeBasedColors.textSecondaryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (summaryState.isLoading) {
            LoadingSummaryCards()
        } else if (summaryState.error != null) {
            ErrorSummaryCard(summaryState.error)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    FitnessCard(
                        title = "Steps",
                        value = summaryState.steps.toString(),
                        unit = "steps",
                        icon = Icons.Default.DirectionsWalk,
                        color = Color(0xFF4CAF50),
                        onClick = { onCardClick("steps") }
                    )
                }

                item {
                    FitnessCard(
                        title = "Distance",
                        value = String.format("%.1f", summaryState.distance),
                        unit = "km",
                        icon = Icons.Default.Route,
                        color = Color(0xFF2196F3),
                        onClick = { onCardClick("distance") }
                    )
                }

                item {
                    FitnessCard(
                        title = "Calories",
                        value = summaryState.calories.toString(),
                        unit = "cal",
                        icon = Icons.Default.LocalFireDepartment,
                        color = Color(0xFFFF5722),
                        onClick = { onCardClick("calories") }
                    )
                }

                item {
                    FitnessCard(
                        title = "Active Time",
                        value = summaryState.activeMinutes.toString(),
                        unit = "min",
                        icon = Icons.Default.Timer,
                        color = Color(0xFF9C27B0),
                        onClick = { onCardClick("active_time") }
                    )
                }

                item {
                    FitnessCard(
                        title = "Sleep",
                        value = String.format("%.1f", summaryState.sleepHours),
                        unit = "hrs",
                        icon = Icons.Default.Bedtime,
                        color = Color(0xFF3F51B5),
                        onClick = { onCardClick("sleep") }
                    )
                }
            }
        }
    }
}

@Composable
private fun FitnessCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "card_alpha"
    )

    Card(
        modifier = Modifier
            .width(140.dp)
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            color.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = timeBasedColors.textSecondaryColor
                    )

                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        color = timeBasedColors.textPrimaryColor,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = timeBasedColors.textSecondaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingSummaryCards() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) {
            Card(
                modifier = Modifier
                    .width(140.dp)
                    .height(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LocalTimeBasedColors.current.cardBackgroundColor
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorSummaryCard(error: String) {
    val timeBasedColors = LocalTimeBasedColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Unable to load fitness data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = timeBasedColors.textPrimaryColor
                )

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = timeBasedColors.textSecondaryColor
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
