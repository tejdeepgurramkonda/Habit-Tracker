package com.example.habittrackerr

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay

@Composable
fun AchievementNotification(
    achievements: List<String>,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(achievements) {
        if (achievements.isNotEmpty()) {
            isVisible = true
            delay(5000) // Auto dismiss after 5 seconds
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Celebration icon
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Achievement",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🎉 Achievement${if (achievements.size > 1) "s" else ""} Unlocked!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    achievements.forEach { achievement ->
                        Text(
                            text = "• ${getAchievementTitle(achievement)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            isVisible = false
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Awesome!")
                    }
                }
            }
        }
    }
}

private fun getAchievementTitle(achievementId: String): String {
    return when (achievementId) {
        "WEEK_WARRIOR" -> "Week Warrior - 7 day streak!"
        "MONTH_MASTER" -> "Month Master - 30 day streak!"
        "HABIT_COLLECTOR" -> "Habit Collector - 10 habits created!"
        "CENTURY_CLUB" -> "Century Club - 100 completions!"
        "FIRST_STEP" -> "First Step - First habit completed!"
        "PERFECT_WEEK" -> "Perfect Week - All habits completed for 7 days!"
        "EARLY_BIRD" -> "Early Bird - Completed habits before 9 AM!"
        "NIGHT_OWL" -> "Night Owl - Completed habits after 9 PM!"
        "CONSISTENCY_KING" -> "Consistency King - No missed days in 14 days!"
        "VARIETY_MASTER" -> "Variety Master - 5 different habit types!"
        else -> achievementId.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
}
