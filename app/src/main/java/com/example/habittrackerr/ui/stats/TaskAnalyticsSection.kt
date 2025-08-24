package com.example.habittrackerr.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habittrackerr.LocalTimeBasedColors
import com.example.habittrackerr.stats.TaskAnalyticsState
import com.example.habittrackerr.data.entities.TaskAnalyticsEntity

@Composable
fun TaskAnalyticsSection(
    analyticsState: TaskAnalyticsState,
    onTaskClick: (Int) -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Task Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    color = timeBasedColors.textPrimaryColor,
                    fontWeight = FontWeight.SemiBold
                )
                
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analytics",
                    tint = timeBasedColors.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when {
                analyticsState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                analyticsState.error != null -> {
                    ErrorMessage(analyticsState.error)
                }
                
                analyticsState.analytics.isEmpty() -> {
                    EmptyAnalyticsMessage()
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(analyticsState.analytics) { analytics ->
                            TaskAnalyticsCard(
                                analytics = analytics,
                                onClick = { onTaskClick(analytics.taskId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskAnalyticsCard(
    analytics: TaskAnalyticsEntity,
    onClick: () -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current
    val completionColor = when {
        analytics.completionRate >= 0.8f -> Color(0xFF4CAF50)
        analytics.completionRate >= 0.5f -> Color(0xFFFFC107)
        else -> Color(0xFFFF5722)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardContentColor.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Task #${analytics.taskId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = timeBasedColors.textPrimaryColor,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricChip(
                        label = "Rate",
                        value = "${(analytics.completionRate * 100).toInt()}%",
                        color = completionColor
                    )
                    
                    MetricChip(
                        label = "Streak",
                        value = "${analytics.currentStreak}",
                        color = timeBasedColors.accentColor
                    )
                    
                    MetricChip(
                        label = "Total",
                        value = "${analytics.totalCompletions}",
                        color = timeBasedColors.textSecondaryColor
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = timeBasedColors.textSecondaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ErrorMessage(error: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
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
                text = "Unable to load analytics",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalTimeBasedColors.current.textSecondaryColor
            )
        }
    }
}

@Composable
private fun EmptyAnalyticsMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = "No analytics",
                tint = LocalTimeBasedColors.current.textSecondaryColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No analytics data available",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalTimeBasedColors.current.textSecondaryColor
            )
        }
    }
}
