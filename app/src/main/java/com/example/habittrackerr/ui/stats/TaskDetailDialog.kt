package com.example.habittrackerr.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.habittrackerr.LocalTimeBasedColors
import com.example.habittrackerr.data.analytics.TaskAnalyticsWithFitness

@Composable
fun TaskDetailDialog(
    analytics: TaskAnalyticsWithFitness?,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current

    if (analytics == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(
                containerColor = timeBasedColors.cardBackgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Task Analytics Details",
                        style = MaterialTheme.typography.headlineSmall,
                        color = timeBasedColors.textPrimaryColor,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(onClick = onExportCsv) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export CSV",
                                tint = timeBasedColors.accentColor
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = timeBasedColors.textSecondaryColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Key Metrics Cards
                    KeyMetricsSection(analytics.analytics)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Time of Day Distribution
                    TimeOfDaySection(analytics.analytics.timeOfDayDistribution)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Fitness Correlation
                    if (analytics.fitnessDataAvailable) {
                        FitnessCorrelationSection(analytics)

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Recent Activity Summary
                    RecentActivitySection(analytics.analytics)
                }
            }
        }
    }
}

@Composable
private fun KeyMetricsSection(analytics: com.example.habittrackerr.data.entities.TaskAnalyticsEntity) {
    val timeBasedColors = LocalTimeBasedColors.current

    Text(
        text = "Key Metrics",
        style = MaterialTheme.typography.titleMedium,
        color = timeBasedColors.textPrimaryColor,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Completion Rate",
            value = "${(analytics.completionRate * 100).toInt()}%",
            icon = Icons.Default.CheckCircle,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Current Streak",
            value = "${analytics.currentStreak}",
            icon = Icons.Default.LocalFireDepartment,
            color = Color(0xFFFF5722),
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Longest Streak",
            value = "${analytics.longestStreak}",
            icon = Icons.Default.Timeline,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Total Completions",
            value = "${analytics.totalCompletions}",
            icon = Icons.Default.Assignment,
            color = Color(0xFF9C27B0),
            modifier = Modifier.weight(1f)
        )
    }

    if (analytics.averageCompletionTimeMinutes > 0) {
        Spacer(modifier = Modifier.height(12.dp))

        MetricCard(
            title = "Avg. Completion Time",
            value = "${analytics.averageCompletionTimeMinutes.toInt()} min",
            icon = Icons.Default.Timer,
            color = Color(0xFFFFC107),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val timeBasedColors = LocalTimeBasedColors.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = timeBasedColors.textSecondaryColor
            )
        }
    }
}

@Composable
private fun TimeOfDaySection(distribution: Map<Int, Int>) {
    val timeBasedColors = LocalTimeBasedColors.current

    Text(
        text = "Time of Day Distribution",
        style = MaterialTheme.typography.titleMedium,
        color = timeBasedColors.textPrimaryColor,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (distribution.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = timeBasedColors.cardContentColor.copy(alpha = 0.05f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No time data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = timeBasedColors.textSecondaryColor
                )
            }
        }
    } else {
        // Simple time distribution display
        val maxCount = distribution.values.maxOrNull() ?: 1

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = timeBasedColors.cardContentColor.copy(alpha = 0.02f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                distribution.entries.sortedBy { it.key }.forEach { (hour, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${hour}:00",
                            style = MaterialTheme.typography.bodySmall,
                            color = timeBasedColors.textSecondaryColor,
                            modifier = Modifier.width(48.dp)
                        )

                        LinearProgressIndicator(
                            progress = count.toFloat() / maxCount,
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp),
                            color = timeBasedColors.accentColor
                        )

                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.bodySmall,
                            color = timeBasedColors.textSecondaryColor,
                            modifier = Modifier.width(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun FitnessCorrelationSection(analytics: TaskAnalyticsWithFitness) {
    val timeBasedColors = LocalTimeBasedColors.current

    Text(
        text = "Fitness Correlation",
        style = MaterialTheme.typography.titleMedium,
        color = timeBasedColors.textPrimaryColor,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardContentColor.copy(alpha = 0.02f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Avg. Steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = timeBasedColors.textSecondaryColor
                    )
                    Text(
                        text = "${analytics.averageSteps.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = timeBasedColors.textPrimaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = "Avg. Calories",
                        style = MaterialTheme.typography.bodySmall,
                        color = timeBasedColors.textSecondaryColor
                    )
                    Text(
                        text = "${analytics.averageCalories.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = timeBasedColors.textPrimaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "These averages are calculated for days when this task was completed.",
                style = MaterialTheme.typography.bodySmall,
                color = timeBasedColors.textSecondaryColor
            )
        }
    }
}

@Composable
private fun RecentActivitySection(analytics: com.example.habittrackerr.data.entities.TaskAnalyticsEntity) {
    val timeBasedColors = LocalTimeBasedColors.current

    Text(
        text = "Summary",
        style = MaterialTheme.typography.titleMedium,
        color = timeBasedColors.textPrimaryColor,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardContentColor.copy(alpha = 0.02f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Date Range: ${analytics.dateRange.replace("_", " to ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = timeBasedColors.textPrimaryColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This analysis includes ${analytics.totalCompletions} completions with a ${(analytics.completionRate * 100).toInt()}% completion rate. Your current streak is ${analytics.currentStreak} days, and your longest streak was ${analytics.longestStreak} days.",
                style = MaterialTheme.typography.bodySmall,
                color = timeBasedColors.textSecondaryColor
            )
        }
    }
}
