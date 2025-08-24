package com.example.habittrackerr.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.habittrackerr.LocalTimeBasedColors
import com.example.habittrackerr.stats.TaskHistoryState
import com.example.habittrackerr.stats.HistoryFilter
import com.example.habittrackerr.data.analytics.TaskHistoryItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskHistorySection(
    historyState: TaskHistoryState,
    onFilterChange: (HistoryFilter) -> Unit,
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
                    text = "Task History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = timeBasedColors.textPrimaryColor
                )

                IconButton(
                    onClick = { /* Navigate to full history */ }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = "View All History",
                        tint = timeBasedColors.textSecondaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.values().forEach { filter ->
                    FilterChip(
                        onClick = { onFilterChange(filter) },
                        label = {
                            Text(
                                text = when (filter) {
                                    HistoryFilter.ALL -> "All Tasks"
                                    HistoryFilter.ACTIVE -> "Active"
                                    HistoryFilter.DELETED -> "Deleted"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = historyState.filter == filter,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = timeBasedColors.accentColor.copy(alpha = 0.2f),
                            selectedLabelColor = timeBasedColors.accentColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                historyState.isLoading -> {
                    LoadingIndicator()
                }
                historyState.error != null -> {
                    ErrorMessage(historyState.error)
                }
                historyState.items.isEmpty() -> {
                    EmptyHistoryMessage(historyState.filter)
                }
                else -> {
                    // Use Column instead of LazyColumn to avoid infinite height constraints
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        historyState.items.take(5).forEach { item ->
                            TaskHistoryCard(
                                item = item,
                                onClick = { onTaskClick(item.habit.id) }
                            )
                        }

                        if (historyState.items.size > 5) {
                            TextButton(
                                onClick = { /* Navigate to full history */ },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("View All (${historyState.items.size} total)")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskHistoryCard(
    item: TaskHistoryItem,
    onClick: () -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (item.isActive) {
                timeBasedColors.cardContentColor.copy(alpha = 0.05f)
            } else {
                timeBasedColors.textSecondaryColor.copy(alpha = 0.1f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Habit icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(item.habit.colorHex.toColorInt()).copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.habit.iconId,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.habit.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = timeBasedColors.textPrimaryColor,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.isActive) TextDecoration.None else TextDecoration.LineThrough
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status badge
                    StatusBadge(
                        isActive = item.isActive,
                        isDeleted = item.habit.isDeleted
                    )

                    // Analytics summary
                    item.analytics?.let { analytics ->
                        Text(
                            text = "${(analytics.completionRate * 100).toInt()}% • ${analytics.currentStreak} streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = timeBasedColors.textSecondaryColor
                        )
                    }
                }

                // Last activity
                Text(
                    text = "Last active: ${formatDate(item.lastActivity)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = timeBasedColors.textSecondaryColor
                )
            }

            // Action indicator
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
private fun StatusBadge(
    isActive: Boolean,
    isDeleted: Boolean
) {
    val timeBasedColors = LocalTimeBasedColors.current

    val (text, color) = when {
        isDeleted -> "Deleted" to Color(0xFFFF5722)
        isActive -> "Active" to Color(0xFF4CAF50)
        else -> "Inactive" to timeBasedColors.textSecondaryColor
    }

    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyHistoryMessage(filter: HistoryFilter) {
    val timeBasedColors = LocalTimeBasedColors.current

    val message = when (filter) {
        HistoryFilter.ALL -> "No tasks found. Create your first habit to see it here!"
        HistoryFilter.ACTIVE -> "No active tasks. All your tasks might be deleted or you haven't created any yet."
        HistoryFilter.DELETED -> "No deleted tasks found."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = when (filter) {
                HistoryFilter.ALL -> Icons.AutoMirrored.Filled.Assignment
                HistoryFilter.ACTIVE -> Icons.Default.PlayArrow
                HistoryFilter.DELETED -> Icons.Default.Delete
            },
            contentDescription = null,
            tint = timeBasedColors.textSecondaryColor,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = timeBasedColors.textSecondaryColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ErrorMessage(@Suppress("UNUSED_PARAMETER") error: String) {
    val timeBasedColors = LocalTimeBasedColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = Color(0xFFFF5722),
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Unable to load task history. Please try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = timeBasedColors.textSecondaryColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
