package com.example.habittrackerr

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.habittrackerr.ui.theme.Orange
import com.example.habittrackerr.ui.theme.Gold
import com.example.habittrackerr.ui.theme.Cyan

data class ProfileStatistic(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)

data class SettingsItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val hasSwitch: Boolean = false,
    val isEnabled: Boolean = false,
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    habitViewModel: HabitViewModel = hiltViewModel()
) {
    val timeBasedColors = LocalTimeBasedColors.current
    val dimensions = LocalResponsiveDimensions.current
    val scope = rememberCoroutineScope()

    // ViewModels - Use the authenticated user data
    val authViewModel: com.example.habittrackerr.auth.AuthViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    // State - Get user from auth system
    val authState by authViewModel.authState.collectAsState()
    val authenticatedUser by authViewModel.currentUser.collectAsState()
    val userData by profileViewModel.userData.collectAsState()
    val userProfile = userData?.profile
    val userPreferences = userData?.preferences
    val uiState by profileViewModel.uiState.collectAsState()

    // Initialize profile with the AUTHENTICATED user data
    LaunchedEffect(authenticatedUser) {
        authenticatedUser?.let { user ->
            println("ProfileScreen: Initializing with authenticated user: ${user.email}")
            // Use the authenticated user's data directly instead of creating separate profile
            profileViewModel.initializeWithUser(
                userId = user.id,
                email = user.email,
                displayName = user.displayName.ifEmpty { user.email.substringBefore("@") }
            )
        }
    }

    // Update last active date
    LaunchedEffect(Unit) {
        profileViewModel.updateLastActiveDate()
    }

    // Check for achievements periodically
    LaunchedEffect(userProfile?.totalCompletions, userProfile?.currentStreak) {
        delay(1000) // Small delay to ensure data is loaded
        profileViewModel.checkAndUnlockAchievements()
    }

    DynamicGradientBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Top App Bar - Show authenticated user name
            ProfileTopBar(
                userName = authenticatedUser?.displayName ?: "User",
                timeBasedColors = timeBasedColors,
                onSignOut = { authViewModel.signOut() }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensions.screenPadding),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingLarge)
            ) {
                item {
                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                }

                // Profile Header with AUTHENTICATED user data
                item {
                    EnhancedProfileHeader(
                        authenticatedUser = authenticatedUser,
                        userProfile = userProfile,
                        timeBasedColors = timeBasedColors,
                        dimensions = dimensions,
                        onEditClick = {
                            // Show edit profile dialog
                        }
                    )
                }

                // Statistics Cards with real data
                item {
                    EnhancedStatisticsSection(
                        userProfile = userProfile,
                        timeBasedColors = timeBasedColors,
                        dimensions = dimensions
                    )
                }

                // Achievement Section
                item {
                    AchievementSection(
                        userProfile = userProfile,
                        timeBasedColors = timeBasedColors,
                        dimensions = dimensions
                    )
                }

                // User Preferences Section
                item {
                    UserPreferencesSection(
                        userPreferences = userPreferences,
                        timeBasedColors = timeBasedColors,
                        dimensions = dimensions,
                        onPreferencesUpdate = { updatedPreferences ->
                            profileViewModel.updatePreferences(updatedPreferences)
                        }
                    )
                }

                // Security Settings
                item {
                    SecuritySection(
                        timeBasedColors = timeBasedColors,
                        dimensions = dimensions,
                        onNavigateToSecurity = {
                            navController.navigate("security_settings")
                        },
                        onSignOut = { authViewModel.signOut() }
                    )
                }

                // Data Management Section
                item {
                    DataManagementSection(
                        timeBasedColors = timeBasedColors,
                        dimensions = dimensions,
                        onExportData = {
                            scope.launch {
                                profileViewModel.exportUserData().collect { jsonData ->
                                    // Handle export (save to file, share, etc.)
                                }
                            }
                        },
                        onDeleteData = {
                            profileViewModel.deleteUserData()
                        }
                    )
                }
            }
        }

        // Show loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Show messages
        uiState.message?.let { message ->
            LaunchedEffect(message) {
                delay(3000)
                profileViewModel.clearMessage()
            }

            Snackbar(
                modifier = Modifier.padding(dimensions.screenPadding),
                action = {
                    TextButton(onClick = { profileViewModel.clearMessage() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(message)
            }
        }

        // Show achievement notifications
        if (uiState.newAchievements.isNotEmpty()) {
            AchievementNotification(
                achievements = uiState.newAchievements,
                onDismiss = { profileViewModel.dismissAchievements() }
            )
        }
    }
}

@Composable
private fun ProfileTopBar(
    userName: String,
    timeBasedColors: TimeBasedColors,
    onSignOut: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Welcome, $userName",
                style = MaterialTheme.typography.bodyMedium,
                color = timeBasedColors.textSecondaryColor
            )
        }

        IconButton(
            onClick = onSignOut
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Sign Out",
                tint = timeBasedColors.textSecondaryColor
            )
        }
    }
}

@Composable
private fun EnhancedProfileHeader(
    authenticatedUser: com.example.habittrackerr.auth.User?,
    userProfile: UserProfile?,
    timeBasedColors: TimeBasedColors,
    dimensions: ResponsiveDimensions,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingXLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image/Avatar
            Box(
                modifier = Modifier
                    .size(dimensions.profileImageSize)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile?.profileImageUrl != null) {
                    // TODO: Load actual image using Coil or similar
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(dimensions.profileImageSize * 0.6f),
                        tint = Color.White
                    )
                } else {
                    Text(
                        text = (userProfile?.displayName ?: authenticatedUser?.displayName ?: "U").take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensions.spacingMedium))

            // Name and Email
            Text(
                text = userProfile?.displayName ?: authenticatedUser?.displayName ?: "Habit Master",
                style = MaterialTheme.typography.headlineSmall,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = userProfile?.email ?: authenticatedUser?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = timeBasedColors.textSecondaryColor
            )

            // Member since
            userProfile?.joinedDate?.let { joinedDate ->
                Text(
                    text = "Member since ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(joinedDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = timeBasedColors.textSecondaryColor
                )
            }

            Spacer(modifier = Modifier.height(dimensions.spacingMedium))

            // Edit Profile Button
            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSize)
                )
                Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                Text("Edit Profile")
            }
        }
    }
}

@Composable
private fun EnhancedStatisticsSection(
    userProfile: UserProfile?,
    timeBasedColors: TimeBasedColors,
    dimensions: ResponsiveDimensions
) {
    // Move color calculations outside remember to avoid Composable calls inside remember
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val statistics = remember(userProfile, primaryColor, secondaryColor, tertiaryColor) {
        listOf(
            ProfileStatistic(
                label = "Level",
                value = "${userProfile?.level ?: 1}",
                icon = Icons.Default.Star,
                color = Gold
            ),
            ProfileStatistic(
                label = "Total Habits",
                value = "${userProfile?.totalHabits ?: 0}",
                icon = Icons.Default.List,
                color = primaryColor
            ),
            ProfileStatistic(
                label = "Current Streak",
                value = "${userProfile?.currentStreak ?: 0} days",
                icon = Icons.Default.LocalFireDepartment,
                color = Orange
            ),
            ProfileStatistic(
                label = "Longest Streak",
                value = "${userProfile?.longestStreak ?: 0} days",
                icon = Icons.Default.EmojiEvents,
                color = Cyan
            ),
            ProfileStatistic(
                label = "Total Completions",
                value = "${userProfile?.totalCompletions ?: 0}",
                icon = Icons.Default.Check,
                color = secondaryColor
            ),
            ProfileStatistic(
                label = "Perfect Days",
                value = "${userProfile?.perfectDays ?: 0}",
                icon = Icons.Default.CheckCircle,
                color = tertiaryColor
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingLarge)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleLarge,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(dimensions.spacingMedium))

            // Experience Progress Bar
            userProfile?.let { profile ->
                val progress = profile.getExperienceProgress()
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "XP: ${profile.experiencePoints}",
                            style = MaterialTheme.typography.bodySmall,
                            color = timeBasedColors.textSecondaryColor
                        )
                        Text(
                            text = "Next: ${profile.getNextLevelExperience()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = timeBasedColors.textSecondaryColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Gold,
                        trackColor = timeBasedColors.cardBackgroundColor
                    )
                }

                Spacer(modifier = Modifier.height(dimensions.spacingLarge))
            }

            // Statistics Grid
            val chunkedStats = statistics.chunked(2)
            chunkedStats.forEach { rowStats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
                ) {
                    rowStats.forEach { stat ->
                        StatisticItem(
                            statistic = stat,
                            timeBasedColors = timeBasedColors,
                            dimensions = dimensions,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowStats.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(dimensions.spacingMedium))
            }
        }
    }
}

@Composable
private fun StatisticItem(
    statistic: ProfileStatistic,
    timeBasedColors: TimeBasedColors,
    dimensions: ResponsiveDimensions,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.surfaceColor.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = statistic.icon,
                contentDescription = null,
                tint = statistic.color,
                modifier = Modifier.size(dimensions.iconSize)
            )

            Spacer(modifier = Modifier.height(dimensions.spacingSmall))

            Text(
                text = statistic.value,
                style = MaterialTheme.typography.titleMedium,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = statistic.label,
                style = MaterialTheme.typography.bodySmall,
                color = timeBasedColors.textSecondaryColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions
// Note: calculateCurrentStreak and calculateLongestStreak functions are defined in UserRepository.kt

private fun formatJoinDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
private fun AchievementSection(
    userProfile: UserProfile?,
    timeBasedColors: TimeBasedColors,
    dimensions: ResponsiveDimensions
) {
    val achievements = remember(userProfile) {
        listOf(
            Achievement(
                id = "first_step",
                title = "First Step",
                description = "Complete your first habit",
                iconResId = "star",
                isUnlocked = (userProfile?.totalCompletions ?: 0) > 0,
                category = AchievementCategory.MILESTONE,
                requiredValue = 1,
                currentValue = userProfile?.totalCompletions ?: 0
            ),
            Achievement(
                id = "week_warrior",
                title = "Week Warrior",
                description = "Maintain a 7-day streak",
                iconResId = "shield",
                isUnlocked = (userProfile?.longestStreak ?: 0) >= 7,
                category = AchievementCategory.STREAK,
                requiredValue = 7,
                currentValue = userProfile?.longestStreak ?: 0
            ),
            Achievement(
                id = "month_master",
                title = "Month Master",
                description = "Maintain a 30-day streak",
                iconResId = "trophy",
                isUnlocked = (userProfile?.longestStreak ?: 0) >= 30,
                category = AchievementCategory.STREAK,
                requiredValue = 30,
                currentValue = userProfile?.longestStreak ?: 0
            ),
            Achievement(
                id = "habit_builder",
                title = "Habit Builder",
                description = "Create 10 habits",
                iconResId = "build",
                isUnlocked = (userProfile?.totalHabits ?: 0) >= 10,
                category = AchievementCategory.VARIETY,
                requiredValue = 10,
                currentValue = userProfile?.totalHabits ?: 0
            ),
            Achievement(
                id = "century_club",
                title = "Century Club",
                description = "Complete 100 habits",
                iconResId = "celebration",
                isUnlocked = (userProfile?.totalCompletions ?: 0) >= 100,
                category = AchievementCategory.COMPLETION,
                requiredValue = 100,
                currentValue = userProfile?.totalCompletions ?: 0
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingLarge)
        ) {
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleLarge,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(dimensions.spacingMedium))

            // Achievement grid
            val chunkedAchievements = achievements.chunked(3)
            chunkedAchievements.forEach { rowAchievements ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    rowAchievements.forEach { achievement ->
                        AchievementBadge(
                            achievement = achievement,
                            timeBasedColors = timeBasedColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowAchievements.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (rowAchievements != chunkedAchievements.last()) {
                    Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    achievement: Achievement,
    timeBasedColors: TimeBasedColors,
    modifier: Modifier = Modifier
) {
    val alpha = if (achievement.isUnlocked) 1f else 0.3f
    val badgeColor = if (achievement.isUnlocked) Gold else timeBasedColors.textSecondaryColor

    // Map iconResId to actual ImageVector
    val iconVector = when (achievement.iconResId) {
        "star" -> Icons.Default.Star
        "shield" -> Icons.Default.Shield
        "trophy" -> Icons.Default.EmojiEvents
        "build" -> Icons.Default.Build
        "celebration" -> Icons.Default.Celebration
        else -> Icons.Default.Star
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = badgeColor.copy(alpha = alpha * 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = achievement.title,
                tint = badgeColor.copy(alpha = alpha),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = achievement.title,
            style = MaterialTheme.typography.bodySmall,
            color = timeBasedColors.textPrimaryColor.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun UserPreferencesSection(
    userPreferences: UserPreferences?,
    timeBasedColors: TimeBasedColors,
    dimensions: ResponsiveDimensions,
    onPreferencesUpdate: (UserPreferences) -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(userPreferences?.notificationsEnabled ?: true) }
    var reminderSound by remember { mutableStateOf(userPreferences?.reminderSound ?: true) }
    var vibrationEnabled by remember { mutableStateOf(userPreferences?.vibrationEnabled ?: true) }
    var darkModeEnabled by remember { mutableStateOf(userPreferences?.darkModeEnabled ?: false) }

    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            Text(
                text = "User Preferences",
                style = MaterialTheme.typography.titleLarge,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )

            // Notifications
            PreferenceItem(
                title = "Notifications",
                subtitle = "Receive notifications for habit reminders",
                isEnabled = notificationsEnabled,
                onToggle = { enabled ->
                    notificationsEnabled = enabled
                    val currentUserId = userPreferences?.userId ?: "default"
                    onPreferencesUpdate(userPreferences?.copy(notificationsEnabled = enabled) ?: UserPreferences(userId = currentUserId, notificationsEnabled = enabled))
                },
                timeBasedColors = timeBasedColors,
                dimensions = dimensions
            )

            // Reminder Sound
            PreferenceItem(
                title = "Reminder Sound",
                subtitle = "Play sound for notifications",
                isEnabled = reminderSound,
                onToggle = { enabled ->
                    reminderSound = enabled
                    val currentUserId = userPreferences?.userId ?: "default"
                    onPreferencesUpdate(userPreferences?.copy(reminderSound = enabled) ?: UserPreferences(userId = currentUserId, reminderSound = enabled))
                },
                timeBasedColors = timeBasedColors,
                dimensions = dimensions
            )

            // Vibration
            PreferenceItem(
                title = "Vibration",
                subtitle = "Vibrate for notifications",
                isEnabled = vibrationEnabled,
                onToggle = { enabled ->
                    vibrationEnabled = enabled
                    val currentUserId = userPreferences?.userId ?: "default"
                    onPreferencesUpdate(userPreferences?.copy(vibrationEnabled = enabled) ?: UserPreferences(userId = currentUserId, vibrationEnabled = enabled))
                },
                timeBasedColors = timeBasedColors,
                dimensions = dimensions
            )

            // Dark Mode
            PreferenceItem(
                title = "Dark Mode",
                subtitle = "Enable dark mode for the app",
                isEnabled = darkModeEnabled,
                onToggle = { enabled ->
                    darkModeEnabled = enabled
                    val currentUserId = userPreferences?.userId ?: "default"
                    onPreferencesUpdate(userPreferences?.copy(darkModeEnabled = enabled) ?: UserPreferences(userId = currentUserId, darkModeEnabled = enabled))
                },
                timeBasedColors = timeBasedColors,
                dimensions = dimensions
            )

            // Reset Preferences
            TextButton(
                onClick = {
                    notificationsEnabled = true
                    reminderSound = true
                    vibrationEnabled = true
                    darkModeEnabled = false
                    val currentUserId = userPreferences?.userId ?: "default"
                    onPreferencesUpdate(UserPreferences(userId = currentUserId))
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = timeBasedColors.cardContentColor
                ),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Reset to Default")
            }
        }
    }
}

@Composable
private fun PreferenceItem(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    timeBasedColors: TimeBasedColors,
    dimensions: ResponsiveDimensions
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle(!isEnabled) }
            .padding(dimensions.spacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = timeBasedColors.textSecondaryColor
            )
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = timeBasedColors.cardContentColor,
                checkedTrackColor = timeBasedColors.cardContentColor.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SecuritySection(
    timeBasedColors: TimeBasedColors,
    dimensions: ResponsiveDimensions,
    onNavigateToSecurity: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleLarge,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )

            // Change Password
            TextButton(
                onClick = onNavigateToSecurity,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = timeBasedColors.cardContentColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Change Password",
                    modifier = Modifier.size(dimensions.iconSize)
                )
                Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                Text("Change Password")
            }

            // Sign Out
            TextButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    modifier = Modifier.size(dimensions.iconSize)
                )
                Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                Text("Sign Out")
            }
        }
    }
}

@Composable
private fun DataManagementSection(
    timeBasedColors: TimeBasedColors,
    dimensions: ResponsiveDimensions,
    onExportData: () -> Unit,
    onDeleteData: () -> Unit
) {
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dataItems = listOf(
        SettingsItem(
            title = "Export Data",
            subtitle = "Backup your habits and progress",
            icon = Icons.Default.CloudUpload,
            onClick = { showExportDialog = true }
        ),
        SettingsItem(
            title = "Import Data",
            subtitle = "Restore from backup",
            icon = Icons.Default.CloudDownload,
            onClick = { showImportDialog = true }
        ),
        SettingsItem(
            title = "Clear All Data",
            subtitle = "Delete all habits and progress",
            icon = Icons.Default.DeleteForever,
            onClick = { showDeleteDialog = true }
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleLarge,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )

            // Export Data
            TextButton(
                onClick = onExportData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = timeBasedColors.cardContentColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Export Data",
                    modifier = Modifier.size(dimensions.iconSize)
                )
                Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                Text("Export Data")
            }

            // Import Data
            TextButton(
                onClick = { onImportData() }, // Fix: Add function call syntax
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = timeBasedColors.cardContentColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Import Data",
                    modifier = Modifier.size(dimensions.iconSize)
                )
                Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                Text("Import Data")
            }

            // Clear All Data
            TextButton(
                onClick = onDeleteData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Clear All Data",
                    modifier = Modifier.size(dimensions.iconSize)
                )
                Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                Text("Clear All Data")
            }
        }
    }

    // Dialogs
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Data") },
            text = { Text("Export your habits and progress data to a backup file.") },
            confirmButton = {
                TextButton(onClick = {
                    // TODO: Implement export functionality
                    showExportDialog = false
                }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Data") },
            text = { Text("Import habits and progress from a backup file. This will replace your current data.") },
            confirmButton = {
                TextButton(onClick = {
                    // TODO: Implement import functionality
                    showImportDialog = false
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Clear All Data") },
            text = { Text("This will permanently delete all your habits and progress. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Implement clear all data functionality
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AboutSection(timeBasedColors: TimeBasedColors, dimensions: ResponsiveDimensions) {
    val aboutItems = listOf(
        SettingsItem(
            title = "App Version",
            subtitle = "1.0.0",
            icon = Icons.Default.Info
        ),
        SettingsItem(
            title = "Privacy Policy",
            subtitle = "How we protect your data",
            icon = Icons.Default.PrivacyTip
        ),
        SettingsItem(
            title = "Terms of Service",
            subtitle = "Legal terms and conditions",
            icon = Icons.Default.Gavel
        ),
        SettingsItem(
            title = "Rate App",
            subtitle = "Share your feedback",
            icon = Icons.Default.Star
        ),
        SettingsItem(
            title = "Contact Support",
            subtitle = "Get help with the app",
            icon = Icons.AutoMirrored.Filled.ContactSupport
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(dimensions.spacingMedium))

            LazyColumn(
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
            ) {
                items(aboutItems.chunked(3)) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                    ) {
                        rowItems.forEach { item ->
                            SettingsItemRow(
                                item = item,
                                timeBasedColors = timeBasedColors,
                                dimensions = dimensions
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit,
    timeBasedColors: TimeBasedColors
) {
    var name by remember { mutableStateOf(userProfile.displayName) }
    var email by remember { mutableStateOf(userProfile.email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = timeBasedColors.cardBackgroundColor,
        title = {
            Text(
                text = "Edit Profile",
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = timeBasedColors.textPrimaryColor,
                        unfocusedTextColor = timeBasedColors.textPrimaryColor,
                        focusedBorderColor = timeBasedColors.cardContentColor,
                        unfocusedBorderColor = timeBasedColors.textSecondaryColor
                    )
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = timeBasedColors.textPrimaryColor,
                        unfocusedTextColor = timeBasedColors.textPrimaryColor,
                        focusedBorderColor = timeBasedColors.cardContentColor,
                        unfocusedBorderColor = timeBasedColors.textSecondaryColor
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(userProfile.copy(displayName = name, email = email))
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = timeBasedColors.cardContentColor
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = timeBasedColors.textSecondaryColor
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StatisticsDialog(
    userProfile: UserProfile,
    habits: List<Habit>,
    onDismiss: () -> Unit,
    timeBasedColors: TimeBasedColors
) {
    val detailedStats = remember(habits) {
        calculateDetailedStatistics(habits)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = timeBasedColors.cardBackgroundColor,
        title = {
            Text(
                text = "Detailed Statistics",
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(detailedStats) { stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stat.label,
                            color = timeBasedColors.textPrimaryColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stat.value,
                            color = timeBasedColors.cardContentColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = timeBasedColors.cardContentColor
                )
            ) {
                Text("Close")
            }
        }
    )
}

// Data classes
// Achievement class is defined in UserPreferences.kt

data class DetailedStatistic(
    val label: String,
    val value: String
)

// Helper functions
private fun calculateDetailedStatistics(habits: List<Habit>): List<DetailedStatistic> {
    val calendar = Calendar.getInstance()
    val today = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val weekStart = today - (6 * 24 * 60 * 60 * 1000)
    val monthStart = today - (29 * 24 * 60 * 60 * 1000)

    val thisWeekCompletions = habits.sumOf { habit ->
        habit.completedDates.count { date -> date >= weekStart }
    }

    val thisMonthCompletions = habits.sumOf { habit ->
        habit.completedDates.count { date -> date >= monthStart }
    }

    val averagePerDay = if (habits.isNotEmpty()) {
        val totalDays = 30
        thisMonthCompletions.toFloat() / totalDays
    } else 0f

    val completionRate = if (habits.isNotEmpty()) {
        val possibleCompletions = habits.size * 30 // 30 days
        (thisMonthCompletions.toFloat() / possibleCompletions * 100).toInt()
    } else 0

    return listOf(
        DetailedStatistic("This Week", "$thisWeekCompletions completions"),
        DetailedStatistic("This Month", "$thisMonthCompletions completions"),
        DetailedStatistic("Daily Average", String.format("%.1f", averagePerDay)),
        DetailedStatistic("Completion Rate", "$completionRate%"),
        DetailedStatistic("Most Active Day", getMostActiveDay(habits)),
        DetailedStatistic("Active Habits", "${habits.count { it.completedDates.isNotEmpty() }}"),
        DetailedStatistic("Perfect Days", "${getPerfectDays(habits)} days")
    )
}

private fun getMostActiveDay(habits: List<Habit>): String {
    val dayCount = mutableMapOf<Int, Int>()

    habits.forEach { habit ->
        habit.completedDates.forEach { date ->
            val calendar = Calendar.getInstance().apply { timeInMillis = date }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            dayCount[dayOfWeek] = dayCount.getOrDefault(dayOfWeek, 0) + 1
        }
    }

    val mostActiveDay = dayCount.maxByOrNull { it.value }?.key ?: Calendar.SUNDAY
    val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    return dayNames[mostActiveDay - 1]
}

private fun getPerfectDays(habits: List<Habit>): Int {
    if (habits.isEmpty()) return 0

    val calendar = Calendar.getInstance()
    var perfectDays = 0

    for (i in 0 until 30) { // Check last 30 days
        calendar.timeInMillis = System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000)
        val dayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000

        val completedHabitsToday = habits.count { habit ->
            habit.completedDates.any { date ->
                date in dayStart until dayEnd
            }
        }

        if (completedHabitsToday == habits.size) {
            perfectDays++
        }
    }

    return perfectDays
}

// Fix missing onImportData function
private fun onImportData() {
    // TODO: Implement import functionality
}

// Add missing SettingsItemRow composable
@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    timeBasedColors: TimeBasedColors,
    dimensions: ResponsiveDimensions,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = item.onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(
            contentColor = timeBasedColors.cardContentColor
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(dimensions.iconSize)
            )
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Fix UserPreferences constructor with userId parameter
private fun createDefaultUserPreferences(userId: String) = UserPreferences(
    userId = userId,
    notificationsEnabled = true,
    reminderSound = true,
    vibrationEnabled = true,
    darkModeEnabled = false
)
