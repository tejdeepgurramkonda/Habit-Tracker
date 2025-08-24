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
import androidx.compose.ui.graphics.Color
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
    var showGoogleFitAuth by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

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
                    // Temporarily disable Google Fit Auth button to prevent crashes
                    // TODO: Re-enable after fixing dependencies
                    /*
                    IconButton(
                        onClick = { showGoogleFitAuth = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Google Fit",
                            tint = if (permissionsState.googleFitPermissions)
                                Color(0xFF4CAF50) else timeBasedColors.textSecondaryColor
                        )
                    }
                    */

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

                    // Refresh button with loading indicator
                    if (summaryState.isLoading || timelineState.isLoading) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                authErrorMessage = null
                                statsViewModel.refreshData()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = timeBasedColors.textSecondaryColor
                            )
                        }
                    }
                }
            }

            // Show auth error if any
            authErrorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF5722).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF5722),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Show connection status
            if (!permissionsState.hasAnyPermissions) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFC107).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No fitness data source connected. Connect Google Fit or Health Connect to see your fitness statistics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFC107)
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

        // Google Fit Authentication Dialog
        if (showGoogleFitAuth) {
            AlertDialog(
                onDismissRequest = { showGoogleFitAuth = false },
                title = { Text("Google Fit Integration") },
                text = {
                    GoogleFitAuthCard(
                        onAuthSuccess = {
                            showGoogleFitAuth = false
                            authErrorMessage = null
                            statsViewModel.refreshData()
                        },
                        onAuthError = { error ->
                            authErrorMessage = error
                            showGoogleFitAuth = false
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showGoogleFitAuth = false }) {
                        Text("Close")
                    }
                }
            )
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
