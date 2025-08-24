package com.example.habittrackerr.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.habittrackerr.stats.PermissionsState

@Composable
fun PermissionsDialog(
    permissionsState: PermissionsState,
    onDismiss: () -> Unit,
    onHealthConnectRequest: () -> Unit,
    onGoogleFitRequest: () -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current

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
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = timeBasedColors.cardBackgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fitness Data Permissions",
                        style = MaterialTheme.typography.headlineSmall,
                        color = timeBasedColors.textPrimaryColor,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = timeBasedColors.textSecondaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Explanation
                Text(
                    text = "To provide comprehensive statistics and insights, we need access to your fitness data. Your privacy is important to us - all data is stored locally and used only for analytics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = timeBasedColors.textSecondaryColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Health Connect Section
                if (permissionsState.healthConnectAvailable) {
                    PermissionCard(
                        title = "Health Connect (Recommended)",
                        description = "Android's unified health platform provides secure access to your fitness data from all apps.",
                        icon = Icons.Default.HealthAndSafety,
                        isGranted = permissionsState.healthConnectPermissions,
                        isRecommended = true,
                        onRequestPermission = onHealthConnectRequest,
                        features = listOf(
                            "Steps, distance, and calories",
                            "Exercise sessions and active minutes",
                            "Sleep duration and quality",
                            "Heart rate data",
                            "Secure, local data access"
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Google Fit Section
                if (permissionsState.googleFitAvailable) {
                    PermissionCard(
                        title = "Google Fit (Fallback)",
                        description = "Alternative fitness data source when Health Connect is not available.",
                        icon = Icons.Default.FitnessCenter,
                        isGranted = permissionsState.googleFitPermissions,
                        isRecommended = false,
                        onRequestPermission = onGoogleFitRequest,
                        features = listOf(
                            "Basic activity data",
                            "Steps and distance",
                            "Calories burned",
                            "Limited data types"
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Privacy Notice
                PrivacyNoticeCard()

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Maybe Later")
                    }

                    Button(
                        onClick = {
                            if (permissionsState.healthConnectAvailable) {
                                onHealthConnectRequest()
                            } else if (permissionsState.googleFitAvailable) {
                                onGoogleFitRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = permissionsState.healthConnectAvailable || permissionsState.googleFitAvailable
                    ) {
                        Text("Grant Permissions")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    isRecommended: Boolean,
    onRequestPermission: () -> Unit,
    features: List<String>
) {
    val timeBasedColors = LocalTimeBasedColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) {
                timeBasedColors.accentColor.copy(alpha = 0.05f)
            } else {
                timeBasedColors.cardContentColor.copy(alpha = 0.02f)
            }
        ),
        border = if (isRecommended) {
            BorderStroke(1.dp, timeBasedColors.accentColor.copy(alpha = 0.3f))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isRecommended) timeBasedColors.accentColor else timeBasedColors.textSecondaryColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = timeBasedColors.textPrimaryColor,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (isRecommended) {
                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .background(
                                        timeBasedColors.accentColor.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Recommended",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = timeBasedColors.accentColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = timeBasedColors.textSecondaryColor
                    )
                }

                // Status indicator
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isGranted) "Granted" else "Not granted",
                    tint = if (isGranted) Color(0xFF4CAF50) else timeBasedColors.textSecondaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Features list
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Feature",
                        tint = timeBasedColors.accentColor,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = timeBasedColors.textSecondaryColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecommended) timeBasedColors.accentColor else timeBasedColors.textSecondaryColor
                    )
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun PrivacyNoticeCard() {
    val timeBasedColors = LocalTimeBasedColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardContentColor.copy(alpha = 0.02f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Privacy",
                tint = timeBasedColors.accentColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Privacy & Security",
                    style = MaterialTheme.typography.titleSmall,
                    color = timeBasedColors.textPrimaryColor,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "• All fitness data is stored locally on your device\n• Only aggregated statistics are synced to the cloud\n• You can revoke permissions at any time\n• No raw health data is shared with third parties",
                    style = MaterialTheme.typography.bodySmall,
                    color = timeBasedColors.textSecondaryColor
                )
            }
        }
    }
}
