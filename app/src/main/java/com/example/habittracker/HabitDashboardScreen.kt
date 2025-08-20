package com.example.habittracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpandableHabitProgressBar(
    habits: List<Habit>,
    modifier: Modifier = Modifier
) {
    val timeBasedColors = LocalTimeBasedColors.current
    var isExpanded by remember { mutableStateOf(false) }

    // Calculate today's habit completion stats
    val todaysStats = remember(habits) {
        calculateHabitStats(habits)
    }

    // Animate the progress for horizontal bar (immediate)
    val animatedProgress by animateFloatAsState(
        targetValue = todaysStats.progressPercentage,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )

    // Separate animation for circular progress (starts from 0 when expanding)
    val circularAnimatedProgress by animateFloatAsState(
        targetValue = if (isExpanded) todaysStats.progressPercentage else 0f,
        animationSpec = tween(
            durationMillis = if (isExpanded) 1200 else 0,
            delayMillis = if (isExpanded) 300 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "circular_progress_animation"
    )

    // Animation for expansion
    val expandedHeight by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else 80.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "expand_animation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(expandedHeight)
            .padding(horizontal = 0.dp)
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(0.5.dp))

            // Show horizontal progress bar
            AnimatedVisibility(
                visible = !isExpanded,
                enter = fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${todaysStats.completedHabits}/${todaysStats.totalHabits}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = timeBasedColors.textPrimaryColor
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = timeBasedColors.cardContentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(timeBasedColors.textSecondaryColor.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (habits.isEmpty()) 0.2f else animatedProgress)
                                .clip(RoundedCornerShape(6.dp))
                                .background(timeBasedColors.cardContentColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Show circular progress and statistics
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(
                    animationSpec = tween(300, delayMillis = 150, easing = FastOutSlowInEasing)
                ) + slideInVertically(
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 4 }
                ),
                exit = fadeOut(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) + slideOutVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    targetOffsetY = { -it / 4 }
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = timeBasedColors.textPrimaryColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            drawCircularProgress(
                                progress = circularAnimatedProgress,
                                color = timeBasedColors.cardContentColor,
                                backgroundColor = timeBasedColors.textSecondaryColor.copy(alpha = 0.3f)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${(circularAnimatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = timeBasedColors.textPrimaryColor
                            )
                            Text(
                                text = "Complete",
                                style = MaterialTheme.typography.bodyMedium,
                                color = timeBasedColors.textSecondaryColor,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            value = todaysStats.completedHabits.toString(),
                            label = "Completed",
                            color = Color(0xFF4CAF50)
                        )

                        StatItem(
                            value = todaysStats.totalHabits.toString(),
                            label = "Total",
                            color = Color(0xFF2196F3)
                        )

                        StatItem(
                            value = todaysStats.remainingHabits.toString(),
                            label = "Remaining",
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
    }
}

private fun DrawScope.drawCircularProgress(
    progress: Float,
    color: Color,
    backgroundColor: Color,
    strokeWidth: Float = 12.dp.toPx()
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = (size.minDimension - strokeWidth) / 2

    drawCircle(
        color = backgroundColor,
        radius = radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )

    if (progress > 0f) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            topLeft = Offset(
                center.x - radius,
                center.y - radius
            ),
            size = Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
    }
}

data class HabitStats(
    val totalHabits: Int,
    val completedHabits: Int,
    val remainingHabits: Int,
    val progressPercentage: Float
)

private fun calculateHabitStats(habits: List<Habit>): HabitStats {
    val today = Date()
    val totalHabits = habits.size

    val completedHabits = habits.count { habit ->
        habit.completedDates.any { completedDate ->
            isSameDay(completedDate, today.time)
        }
    }

    val remainingHabits = totalHabits - completedHabits
    val progressPercentage = if (totalHabits > 0) {
        completedHabits.toFloat() / totalHabits.toFloat()
    } else {
        0f
    }

    return HabitStats(
        totalHabits = totalHabits,
        completedHabits = completedHabits,
        remainingHabits = remainingHabits,
        progressPercentage = progressPercentage
    )
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HabitDashboardScreen(
    navController: NavController,
    viewModel: HabitViewModel = hiltViewModel()
) {
    val habits by viewModel.habits.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val selectedHabit by viewModel.selectedHabit.collectAsState()

    DynamicGradientBackground {
        val timeBasedColors = LocalTimeBasedColors.current

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    containerColor = timeBasedColors.cardContentColor,
                    contentColor = Color.White,
                    modifier = Modifier.shadow(8.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Habit",
                        tint = Color.White
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(top = 16.dp)
            ) {
                // Habit Progress Bar
                ExpandableHabitProgressBar(
                    habits = habits,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // My Habits title
                Text(
                    text = "My Habits",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = timeBasedColors.textPrimaryColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Habits List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (habits.isEmpty()) {
                        item {
                            EmptyStateCard()
                        }
                    } else {
                        items(habits, key = { habit -> habit.id }) { habit ->
                            HabitCard(
                                habit = habit,
                                viewModel = viewModel,
                                onLongPress = { viewModel.showEditDialog(habit) },
                                onDoubleClick = { viewModel.showEditDialog(habit) },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                }
            }
        }

        // Show the habit dialog when needed
        if (showDialog) {
            AddEditHabitDialog(
                habitViewModel = viewModel,
                habit = selectedHabit,
                onDismiss = { viewModel.hideDialog() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitCard(
    habit: Habit,
    viewModel: HabitViewModel,
    onLongPress: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = Date()
    val isCompleted = remember(habit.completedDates) {
        habit.completedDates.any { completedDate ->
            isSameDay(completedDate, today.time)
        }
    }
    val timeBasedColors = LocalTimeBasedColors.current

    // Animation states
    var triggerAnimation by remember { mutableStateOf(false) }
    val cardScale = remember { Animatable(1f) }
    val rippleProgress = remember { Animatable(0f) }
    val checkmarkProgress = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }

    // Trigger animations when habit becomes completed
    LaunchedEffect(isCompleted) {
        if (isCompleted && !triggerAnimation) {
            triggerAnimation = true

            launch {
                cardScale.animateTo(
                    targetValue = 1.05f,
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                )
                cardScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                )
            }

            launch {
                rippleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            }

            launch {
                checkmarkProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            }

            launch {
                particleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(800, easing = FastOutSlowInEasing)
                )
            }
        } else if (!isCompleted) {
            triggerAnimation = false
            cardScale.snapTo(1f)
            rippleProgress.snapTo(0f)
            checkmarkProgress.snapTo(0f)
            particleProgress.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale.value)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .clip(RoundedCornerShape(16.dp))
            .background(timeBasedColors.cardBackgroundColor)
            .combinedClickable(
                onClick = { },
                onLongClick = onLongPress,
                onDoubleClick = onDoubleClick
            )
            .padding(16.dp)
            .alpha(if (isCompleted) 0.6f else 1.0f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(habit.colorHex))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = habit.iconId,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = timeBasedColors.textPrimaryColor
                    )
                    Text(
                        text = habit.frequencyType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = timeBasedColors.textSecondaryColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .alpha(if (isCompleted) 1.0f / 0.6f else 1.0f)
                    .size(40.dp)
            ) {
                if (rippleProgress.value > 0f) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawRippleEffect(
                            progress = rippleProgress.value,
                            center = Offset(size.width / 2, size.height / 2),
                            maxRadius = 60.dp.toPx(),
                            color = Color(android.graphics.Color.parseColor(habit.colorHex))
                        )
                    }
                }

                if (particleProgress.value > 0f) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawParticleBurst(
                            progress = particleProgress.value,
                            center = Offset(size.width / 2, size.height / 2),
                            color = Color(android.graphics.Color.parseColor(habit.colorHex))
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (isCompleted) {
                            viewModel.undoHabitCompletion(habit)
                        } else {
                            viewModel.markHabitAsCompleted(habit, today.time)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (isCompleted)
                                timeBasedColors.cardContentColor
                            else
                                timeBasedColors.cardContentColor.copy(alpha = 0.3f)
                        )
                ) {
                    if (isCompleted && checkmarkProgress.value > 0f) {
                        Canvas(
                            modifier = Modifier.size(20.dp)
                        ) {
                            drawAnimatedCheckmark(
                                progress = checkmarkProgress.value,
                                color = Color.White,
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Check,
                            contentDescription = if (isCompleted) "Completed - Tap to undo" else "Mark as Complete",
                            tint = if (isCompleted)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper function to draw ripple effect
private fun DrawScope.drawRippleEffect(
    progress: Float,
    center: Offset,
    maxRadius: Float,
    color: Color
) {
    val currentRadius = progress * maxRadius
    val alpha = 1f - progress

    drawCircle(
        color = color.copy(alpha = alpha * 0.3f),
        radius = currentRadius,
        center = center,
        style = Stroke(width = 4.dp.toPx())
    )

    if (progress > 0.3f) {
        val secondaryProgress = (progress - 0.3f) / 0.7f
        val secondaryRadius = secondaryProgress * maxRadius * 0.8f
        val secondaryAlpha = 1f - secondaryProgress

        drawCircle(
            color = color.copy(alpha = secondaryAlpha * 0.2f),
            radius = secondaryRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// Helper function to draw particle burst
private fun DrawScope.drawParticleBurst(
    progress: Float,
    center: Offset,
    color: Color
) {
    val particleCount = 8
    val maxDistance = 50.dp.toPx()

    for (i in 0 until particleCount) {
        val angle = (i * 360f / particleCount) * (kotlin.math.PI / 180f)
        val distance = progress * maxDistance
        val particleSize = (1f - progress) * 4.dp.toPx()

        val x = center.x + cos(angle).toFloat() * distance
        val y = center.y + sin(angle).toFloat() * distance

        if (particleSize > 0) {
            drawCircle(
                color = color.copy(alpha = 1f - progress),
                radius = particleSize,
                center = Offset(x, y)
            )
        }
    }
}

// Helper function to draw animated checkmark
private fun DrawScope.drawAnimatedCheckmark(
    progress: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    val checkSize = size.minDimension
    val strokePath = Path()

    // Define checkmark path points
    val startX = checkSize * 0.2f
    val startY = checkSize * 0.5f
    val midX = checkSize * 0.4f
    val midY = checkSize * 0.7f
    val endX = checkSize * 0.8f
    val endY = checkSize * 0.3f

    // Create the checkmark path
    path.moveTo(startX, startY)
    path.lineTo(midX, midY)
    path.lineTo(endX, endY)

    // Create a path measure to get portions of the path
    val pathMeasure = PathMeasure()
    pathMeasure.setPath(path, false)
    val totalLength = pathMeasure.length
    val animatedLength = totalLength * progress

    // Extract the animated portion
    pathMeasure.getSegment(0f, animatedLength, strokePath, true)

    drawPath(
        path = strokePath,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

@Composable
fun EmptyStateCard() {
    val timeBasedColors = LocalTimeBasedColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🌟",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "No Habits Yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = timeBasedColors.textPrimaryColor
            )
            Text(
                text = "Start building great habits!\nTap the + button to add your first habit.",
                style = MaterialTheme.typography.bodyMedium,
                color = timeBasedColors.textSecondaryColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
