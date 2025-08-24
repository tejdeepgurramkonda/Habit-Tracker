package com.example.habittrackerr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Assessment
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpandableHabitProgressBar(
    habits: List<Habit>,
    modifier: Modifier = Modifier
) {
    val timeBasedColors = LocalTimeBasedColors.current
    val dimensions = LocalResponsiveDimensions.current
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
        targetValue = if (isExpanded) dimensions.progressExpandedHeight else dimensions.progressCollapsedHeight,
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
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() }
            ) { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(dimensions.spacingXSmall))

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
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize * dimensions.bodyTextScale
                            ),
                            fontWeight = FontWeight.Bold,
                            color = timeBasedColors.textPrimaryColor
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize * dimensions.bodyTextScale
                            ),
                            fontWeight = FontWeight.Bold,
                            color = timeBasedColors.cardContentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensions.progressBarHeight)
                            .clip(RoundedCornerShape(dimensions.progressBarRadius))
                            .background(timeBasedColors.textSecondaryColor.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (habits.isEmpty()) 0.2f else animatedProgress)
                                .clip(RoundedCornerShape(dimensions.progressBarRadius))
                                .background(timeBasedColors.cardContentColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))
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
                    Spacer(modifier = Modifier.height(dimensions.spacingLarge))

                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * dimensions.titleTextScale
                        ),
                        fontWeight = FontWeight.Bold,
                        color = timeBasedColors.textPrimaryColor
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))

                    Box(
                        modifier = Modifier.size(dimensions.circularProgressSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            drawCircularProgress(
                                progress = circularAnimatedProgress,
                                color = timeBasedColors.cardContentColor,
                                strokeWidth = dimensions.spacingMedium.toPx()
                            )
                        }

                        Text(
                            text = "${(circularAnimatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize * dimensions.titleTextScale
                            ),
                            fontWeight = FontWeight.Bold,
                            color = timeBasedColors.textPrimaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacingLarge))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatisticItem(
                            label = "Completed",
                            value = todaysStats.completedHabits.toString(),
                            color = timeBasedColors.cardContentColor,
                            dimensions = dimensions
                        )
                        StatisticItem(
                            label = "Total",
                            value = todaysStats.totalHabits.toString(),
                            color = timeBasedColors.textPrimaryColor,
                            dimensions = dimensions
                        )
                        StatisticItem(
                            label = "Remaining",
                            value = todaysStats.remainingHabits.toString(),
                            color = timeBasedColors.textSecondaryColor,
                            dimensions = dimensions
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    color: Color,
    dimensions: ResponsiveDimensions
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = MaterialTheme.typography.headlineSmall.fontSize * dimensions.titleTextScale
            ),
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = MaterialTheme.typography.bodyMedium.fontSize * dimensions.captionTextScale
            ),
            color = color.copy(alpha = 0.7f)
        )
    }
}

private fun DrawScope.drawCircularProgress(
    progress: Float,
    color: Color,
    strokeWidth: Float = 12.dp.toPx()
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = (size.minDimension - strokeWidth) / 2
    val backgroundColor = color.copy(alpha = 0.1f)

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
    habitViewModel: HabitViewModel = hiltViewModel()
) {
    val dimensions = LocalResponsiveDimensions.current
    val habits by habitViewModel.habits.collectAsState()
    val showDialog by habitViewModel.showDialog.collectAsState()
    val selectedHabit by habitViewModel.selectedHabit.collectAsState()
    val selectedDate by habitViewModel.selectedDate.collectAsState()

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // State for monthly calendar popup
    var showMonthlyCalendar by remember { mutableStateOf(false) }

    // Get current month and year
    val currentMonthYear = remember {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        formatter.format(Date())
    }

    // Calculate current streak
    val currentStreak = remember(habits) {
        calculateMainStreak(habits)
    }

    DynamicGradientBackground {
        val timeBasedColors = LocalTimeBasedColors.current

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawerContent(
                    timeBasedColors = timeBasedColors,
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Today",
                                    color = timeBasedColors.textPrimaryColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = MaterialTheme.typography.titleLarge.fontSize * dimensions.titleTextScale
                                    )
                                )

                                // Streak Display in Center
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                                    ) {
                                        Text(
                                            text = "🔥",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize * dimensions.titleTextScale
                                            )
                                        )
                                        Text(
                                            text = currentStreak.toString(),
                                            color = timeBasedColors.cardContentColor,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = MaterialTheme.typography.titleLarge.fontSize * dimensions.titleTextScale
                                            )
                                        )
                                    }
                                    Text(
                                        text = if (currentStreak == 1) "day streak" else "days streak",
                                        color = timeBasedColors.textSecondaryColor,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = MaterialTheme.typography.bodySmall.fontSize * dimensions.captionTextScale
                                        )
                                    )
                                }

                                // Month/Year aligned to the right
                                Text(
                                    text = currentMonthYear,
                                    color = timeBasedColors.textPrimaryColor,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize * dimensions.titleTextScale
                                    ),
                                    modifier = Modifier
                                        .clickable(
                                            indication = LocalIndication.current,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { showMonthlyCalendar = true }
                                        .padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch { drawerState.open() }
                                },
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
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(top = dimensions.spacingMedium)
                ) {
                    // Horizontal Calendar
                    HorizontalCalendar(
                        habits = habits,
                        selectedDate = selectedDate,
                        onDateSelected = { date -> habitViewModel.updateSelectedDate(date) },
                        modifier = Modifier.padding(vertical = dimensions.spacingXSmall)
                    )

                    // Habit Progress Bar
                    ExpandableHabitProgressBar(
                        habits = habits,
                        modifier = Modifier.padding(vertical = dimensions.spacingXSmall)
                    )

                    // My Habits title
                    Text(
                        text = "My Habits",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize * dimensions.titleTextScale
                        ),
                        fontWeight = FontWeight.Bold,
                        color = timeBasedColors.textPrimaryColor,
                        modifier = Modifier.padding(horizontal = dimensions.spacingLarge, vertical = dimensions.spacingXSmall)
                    )

                    // Habits List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = dimensions.spacingLarge),
                        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
                        contentPadding = PaddingValues(vertical = dimensions.spacingLarge)
                    ) {
                        if (habits.isEmpty()) {
                            item {
                                EmptyStateCard()
                            }
                        } else {
                            items(habits, key = { habit -> habit.id }) { habit ->
                                HabitCard(
                                    habit = habit,
                                    viewModel = habitViewModel,
                                    selectedDate = selectedDate,
                                    onLongPress = { habitViewModel.showEditDialog(habit) },
                                    onDoubleClick = { habitViewModel.showEditDialog(habit) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }

                // Show the habit dialog when needed
                if (showDialog) {
                    AddEditHabitDialog(
                        habitViewModel = habitViewModel,
                        habit = selectedHabit,
                        onDismiss = { habitViewModel.hideDialog() }
                    )
                }

                // Show monthly calendar popup when needed
                if (showMonthlyCalendar) {
                    MonthlyCalendarPopup(
                        selectedDate = selectedDate,
                        onDateSelected = { date -> habitViewModel.updateSelectedDate(date) },
                        onDismiss = { showMonthlyCalendar = false },
                        habits = habits
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitCard(
    habit: Habit,
    viewModel: HabitViewModel,
    selectedDate: Date,
    onLongPress: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = remember(habit.completedDates, selectedDate) {
        habit.completedDates.any { completedDate ->
            isSameDay(completedDate, selectedDate.time)
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
                            viewModel.undoHabitCompletionForSelectedDate(habit)
                        } else {
                            viewModel.markHabitAsCompletedForSelectedDate(habit)
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

@Composable
fun NavigationDrawerContent(
    timeBasedColors: TimeBasedColors,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = timeBasedColors.cardBackgroundColor,
        drawerContentColor = timeBasedColors.textPrimaryColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Profile Header
            ProfileHeader(timeBasedColors = timeBasedColors)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = timeBasedColors.textSecondaryColor.copy(alpha = 0.3f)
            )

            // Menu Items
            DrawerMenuItem(
                icon = Icons.Default.Person,
                title = "Profile",
                timeBasedColors = timeBasedColors,
                onClick = {
                    // Handle profile navigation
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.AccountCircle,
                title = "Account",
                timeBasedColors = timeBasedColors,
                onClick = {
                    // Handle account navigation
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Assessment,
                title = "Statistics",
                timeBasedColors = timeBasedColors,
                onClick = {
                    // Handle statistics navigation
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                timeBasedColors = timeBasedColors,
                onClick = {
                    // Handle notifications navigation
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                timeBasedColors = timeBasedColors,
                onClick = {
                    // Handle settings navigation
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Help,
                title = "Help & Support",
                timeBasedColors = timeBasedColors,
                onClick = {
                    // Handle help navigation
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Info,
                title = "About",
                timeBasedColors = timeBasedColors,
                onClick = {
                    // Handle about navigation
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = timeBasedColors.textSecondaryColor.copy(alpha = 0.3f)
            )

            // Logout
            DrawerMenuItem(
                icon = Icons.Default.ExitToApp,
                title = "Logout",
                timeBasedColors = timeBasedColors,
                isDestructive = true,
                onClick = {
                    // Handle logout
                    onCloseDrawer()
                }
            )
        }
    }
}

@Composable
fun ProfileHeader(timeBasedColors: TimeBasedColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(timeBasedColors.cardContentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Picture",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // User Name
        Text(
            text = "John Doe",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = timeBasedColors.textPrimaryColor
        )

        // User Email
        Text(
            text = "john.doe@example.com",
            style = MaterialTheme.typography.bodyMedium,
            color = timeBasedColors.textSecondaryColor
        )
    }
}

@Composable
fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    timeBasedColors: TimeBasedColors,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isDestructive) Color(0xFFE53E3E) else timeBasedColors.cardContentColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) Color(0xFFE53E3E) else timeBasedColors.textPrimaryColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun calculateMainStreak(habits: List<Habit>): Int {
    if (habits.isEmpty()) return 0

    val today = Calendar.getInstance()
    var currentStreak = 0
    var checkDate = Calendar.getInstance()

    // Start from yesterday and work backwards
    checkDate.add(Calendar.DAY_OF_YEAR, -1)

    while (true) {
        val dateToCheck = checkDate.time

        // Check if ALL habits were completed on this date
        val allHabitsCompleted = habits.all { habit ->
            habit.completedDates.any { completedDate ->
                isSameDay(completedDate, dateToCheck.time)
            }
        }

        if (allHabitsCompleted) {
            currentStreak++
            checkDate.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }

    return currentStreak
}
