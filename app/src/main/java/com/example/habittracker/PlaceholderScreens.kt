package com.example.habittracker

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// TimerScreen is now implemented in TimerScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onOpenDrawer: () -> Unit = {}) {
    val dimensions = LocalResponsiveDimensions.current
    val timeBasedColors = LocalTimeBasedColors.current

    DynamicGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Statistics",
                            color = timeBasedColors.textPrimaryColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = MaterialTheme.typography.titleLarge.fontSize * dimensions.titleTextScale
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.size(dimensions.iconButtonSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Menu",
                                tint = timeBasedColors.textPrimaryColor,
                                modifier = Modifier.size(dimensions.iconSize)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = "Statistics",
                    tint = timeBasedColors.cardContentColor,
                    modifier = Modifier.size(dimensions.iconSize * 2)
                )

                Spacer(modifier = Modifier.height(dimensions.spacingLarge))

                Text(
                    text = "Statistics & Analytics",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize * dimensions.titleTextScale
                    ),
                    fontWeight = FontWeight.Bold,
                    color = timeBasedColors.textPrimaryColor
                )

                Spacer(modifier = Modifier.height(dimensions.spacingMedium))

                Text(
                    text = "Coming Soon!",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * dimensions.bodyTextScale
                    ),
                    color = timeBasedColors.textSecondaryColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onOpenDrawer: () -> Unit = {}) {
    val dimensions = LocalResponsiveDimensions.current
    val timeBasedColors = LocalTimeBasedColors.current

    DynamicGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Profile",
                            color = timeBasedColors.textPrimaryColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = MaterialTheme.typography.titleLarge.fontSize * dimensions.titleTextScale
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.size(dimensions.iconButtonSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Menu",
                                tint = timeBasedColors.textPrimaryColor,
                                modifier = Modifier.size(dimensions.iconSize)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = timeBasedColors.cardContentColor,
                    modifier = Modifier.size(dimensions.iconSize * 2)
                )

                Spacer(modifier = Modifier.height(dimensions.spacingLarge))

                Text(
                    text = "User Profile",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize * dimensions.titleTextScale
                    ),
                    fontWeight = FontWeight.Bold,
                    color = timeBasedColors.textPrimaryColor
                )

                Spacer(modifier = Modifier.height(dimensions.spacingMedium))

                Text(
                    text = "Coming Soon!",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * dimensions.bodyTextScale
                    ),
                    color = timeBasedColors.textSecondaryColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
