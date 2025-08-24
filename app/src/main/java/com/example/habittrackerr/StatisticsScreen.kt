package com.example.habittrackerr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittrackerr.stats.*
import com.example.habittrackerr.ui.stats.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    habitViewModel: HabitViewModel,
    statsViewModel: StatsViewModel = hiltViewModel()
) {
    val timeBasedColors = LocalTimeBasedColors.current
    val summaryState by statsViewModel.summaryState.collectAsState()
    val timelineState by statsViewModel.timelineState.collectAsState()
    val taskAnalyticsState by statsViewModel.taskAnalyticsState.collectAsState()
    val historyState by statsViewModel.historyState.collectAsState()
    val permissionsState by statsViewModel.permissionsState.collectAsState()
    val selectedTimeRange by statsViewModel.selectedTimeRange.collectAsState()

    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showTaskDetailDialog by remember { mutableStateOf(false) }

    // Show permissions dialog if no permissions granted
    LaunchedEffect(permissionsState) {
        if (!permissionsState.hasAnyPermissions &&
            (permissionsState.healthConnectAvailable || permissionsState.googleFitAvailable)) {
            showPermissionsDialog = true
        }
    }

    DynamicGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.headlineMedium,
                    color = timeBasedColors.textPrimaryColor,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    // Settings button
                    IconButton(
                        onClick = { showPermissionsDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = timeBasedColors.textSecondaryColor
                        )
                    }

                    // Refresh button
                    IconButton(
                        onClick = { statsViewModel.refreshData() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = timeBasedColors.textSecondaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main content
            LazyColumn(
                state = rememberLazyListState(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fitness Summary Cards
                item {
                    FitnessSummarySection(
                        summaryState = summaryState,
                        onCardClick = { /* Navigate to detailed view */ }
                    )
                }

                // Time Range Selector
                item {
                    TimeRangeSelector(
                        selectedRange = selectedTimeRange,
                        onRangeSelected = { statsViewModel.selectTimeRange(it) }
                    )
                }

                // Timeline Chart
                item {
                    TimelineChartSection(
                        timelineState = timelineState,
                        onDataPointClick = { /* Show details for selected day */ }
                    )
                }

                // Task Analytics Section
                item {
                    TaskAnalyticsSection(
                        analyticsState = taskAnalyticsState,
                        onTaskClick = { taskId ->
                            statsViewModel.selectTask(taskId)
                            showTaskDetailDialog = true
                        }
                    )
                }

                // Task History Section
                item {
                    TaskHistorySection(
                        historyState = historyState,
                        onFilterChange = { filter ->
                            statsViewModel.setHistoryFilter(filter)
                        },
                        onTaskClick = { taskId ->
                            statsViewModel.selectTask(taskId)
                            showTaskDetailDialog = true
                        }
                    )
                }

                // Add some bottom padding
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Permissions Dialog
        if (showPermissionsDialog) {
            PermissionsDialog(
                permissionsState = permissionsState,
                onDismiss = { showPermissionsDialog = false },
                onHealthConnectRequest = { /* Handle Health Connect permission request */ },
                onGoogleFitRequest = { /* Handle Google Fit permission request */ }
            )
        }

        // Task Detail Dialog
        if (showTaskDetailDialog) {
            TaskDetailDialog(
                analytics = taskAnalyticsState.selectedTaskAnalytics,
                onDismiss = {
                    showTaskDetailDialog = false
                    statsViewModel.selectTask(null)
                },
                onExportCsv = { /* Handle CSV export */ }
            )
        }
    }
}
