package com.example.habittrackerr

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.*

// Timer state enum with all possible states
enum class TimerState {
    STOPPED,    // Timer is stopped/reset
    RUNNING,    // Timer is actively counting down
    PAUSED,     // Timer is paused
    FINISHED    // Timer has completed and alarm is active
}

// Timer preset data class for quick timer options
data class TimerPreset(
    val name: String,
    val minutes: Int,
    val seconds: Int,
    val icon: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onOpenDrawer: () -> Unit = {}) {
    val dimensions = LocalResponsiveDimensions.current
    val timeBasedColors = LocalTimeBasedColors.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    // Core timer state
    var timerState by remember { mutableStateOf(TimerState.STOPPED) }
    var totalTimeInSeconds by remember { mutableStateOf(300) } // Default 5 minutes
    var remainingTimeInSeconds by remember { mutableStateOf(totalTimeInSeconds) }
    var selectedMinutes by remember { mutableStateOf(5) }
    var selectedSeconds by remember { mutableStateOf(0) }

    // UI state
    var isAlarmActive by remember { mutableStateOf(false) }
    var showTimerControls by remember { mutableStateOf(true) }

    // Media player for alarm sound
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Animation states
    val progress by animateFloatAsState(
        targetValue = if (totalTimeInSeconds > 0) {
            1f - (remainingTimeInSeconds.toFloat() / totalTimeInSeconds.toFloat())
        } else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "timer_progress"
    )

    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotateAnimation by pulseAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    // Timer presets for quick selection
    val timerPresets = remember {
        listOf(
            TimerPreset("Pomodoro", 25, 0, "🍅", "Focus session"),
            TimerPreset("Short Break", 5, 0, "☕", "Quick rest"),
            TimerPreset("Long Break", 15, 0, "🛋️", "Extended rest"),
            TimerPreset("Meditation", 10, 0, "🧘", "Mindfulness"),
            TimerPreset("Exercise", 20, 0, "💪", "Workout time"),
            TimerPreset("Study", 30, 0, "📚", "Learning session"),
            TimerPreset("Deep Work", 45, 0, "🎯", "Intense focus"),
            TimerPreset("Power Nap", 20, 0, "😴", "Quick sleep")
        )
    }

    // Initialize media player
    LaunchedEffect(Unit) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                try {
                    setDataSource(context, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                } catch (e: Exception) {
                    setDataSource(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                }
                isLooping = true
                prepare()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Timer countdown logic with perfect synchronization
    LaunchedEffect(timerState, remainingTimeInSeconds) {
        when (timerState) {
            TimerState.RUNNING -> {
                if (remainingTimeInSeconds > 0) {
                    delay(1000L)
                    remainingTimeInSeconds--
                } else {
                    // Timer finished - trigger alarm
                    timerState = TimerState.FINISHED
                    isAlarmActive = true
                    triggerAlarm(context, mediaPlayer, hapticFeedback)
                }
            }
            TimerState.FINISHED -> {
                // Auto-stop alarm after 60 seconds if not manually stopped
                delay(60000L)
                if (isAlarmActive) {
                    stopAlarm(mediaPlayer)
                    isAlarmActive = false
                    timerState = TimerState.STOPPED
                    remainingTimeInSeconds = totalTimeInSeconds
                }
            }
            else -> {
                // Do nothing for STOPPED and PAUSED states
            }
        }
    }

    // Cleanup media player
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Update remaining time when total time changes (for preset selection)
    LaunchedEffect(totalTimeInSeconds) {
        if (timerState == TimerState.STOPPED) {
            remainingTimeInSeconds = totalTimeInSeconds
        }
    }

    DynamicGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Timer & Alarm",
                            color = timeBasedColors.textPrimaryColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Menu",
                                tint = timeBasedColors.textPrimaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = dimensions.spacingMedium)
            ) {
                // Main content - no scrolling, optimized layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp), // Space for control buttons
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Timer presets (only show when stopped)
                    AnimatedVisibility(
                        visible = timerState == TimerState.STOPPED,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        TimerPresetsSection(
                            presets = timerPresets,
                            onPresetSelected = { preset ->
                                selectedMinutes = preset.minutes
                                selectedSeconds = preset.seconds
                                totalTimeInSeconds = preset.minutes * 60 + preset.seconds
                                remainingTimeInSeconds = totalTimeInSeconds
                            },
                            dimensions = dimensions,
                            timeBasedColors = timeBasedColors
                        )
                    }

                    // Main timer display
                    TimerDisplaySection(
                        remainingTime = remainingTimeInSeconds,
                        totalTime = totalTimeInSeconds,
                        timerState = timerState,
                        progress = progress,
                        pulseScale = if (timerState == TimerState.FINISHED) pulseScale else 1f,
                        rotateAngle = if (isAlarmActive) rotateAnimation else 0f,
                        dimensions = dimensions,
                        timeBasedColors = timeBasedColors
                    )

                    // Time adjustment controls (only when stopped)
                    AnimatedVisibility(
                        visible = timerState == TimerState.STOPPED,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        TimeAdjustmentSection(
                            minutes = selectedMinutes,
                            seconds = selectedSeconds,
                            onMinutesChange = { minutes ->
                                selectedMinutes = minutes
                                totalTimeInSeconds = minutes * 60 + selectedSeconds
                                remainingTimeInSeconds = totalTimeInSeconds
                            },
                            onSecondsChange = { seconds ->
                                selectedSeconds = seconds
                                totalTimeInSeconds = selectedMinutes * 60 + seconds
                                remainingTimeInSeconds = totalTimeInSeconds
                            },
                            dimensions = dimensions,
                            timeBasedColors = timeBasedColors
                        )
                    }
                }

                // Control buttons - Fixed at bottom without background card
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.spacingLarge)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (timerState) {
                        TimerState.STOPPED -> {
                            FloatingActionButton(
                                onClick = {
                                    if (totalTimeInSeconds > 0) {
                                        timerState = TimerState.RUNNING
                                        stopAlarm(mediaPlayer)
                                        isAlarmActive = false
                                    }
                                },
                                containerColor = timeBasedColors.cardContentColor,
                                contentColor = Color.White,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Timer",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        TimerState.RUNNING -> {
                            FloatingActionButton(
                                onClick = { timerState = TimerState.PAUSED },
                                containerColor = timeBasedColors.textSecondaryColor,
                                contentColor = Color.White,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause Timer",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            FloatingActionButton(
                                onClick = {
                                    timerState = TimerState.STOPPED
                                    remainingTimeInSeconds = totalTimeInSeconds
                                    stopAlarm(mediaPlayer)
                                    isAlarmActive = false
                                },
                                containerColor = Color.Red,
                                contentColor = Color.White,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Timer",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        TimerState.PAUSED -> {
                            FloatingActionButton(
                                onClick = { timerState = TimerState.RUNNING },
                                containerColor = timeBasedColors.cardContentColor,
                                contentColor = Color.White,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume Timer",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            FloatingActionButton(
                                onClick = {
                                    timerState = TimerState.STOPPED
                                    remainingTimeInSeconds = totalTimeInSeconds
                                    stopAlarm(mediaPlayer)
                                    isAlarmActive = false
                                },
                                containerColor = Color.Red,
                                contentColor = Color.White,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Timer",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        TimerState.FINISHED -> {
                            FloatingActionButton(
                                onClick = {
                                    stopAlarm(mediaPlayer)
                                    isAlarmActive = false
                                    timerState = TimerState.STOPPED
                                    remainingTimeInSeconds = totalTimeInSeconds
                                },
                                containerColor = Color.Red,
                                contentColor = Color.White,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Alarm",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerPresetsSection(
    presets: List<TimerPreset>,
    onPresetSelected: (TimerPreset) -> Unit,
    dimensions: ResponsiveDimensions,
    timeBasedColors: TimeBasedColors
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Quick Timers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = timeBasedColors.textPrimaryColor,
            modifier = Modifier.padding(bottom = dimensions.spacingMedium)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
            contentPadding = PaddingValues(horizontal = dimensions.spacingMedium)
        ) {
            items(presets) { preset ->
                TimerPresetCard(
                    preset = preset,
                    onClick = { onPresetSelected(preset) },
                    dimensions = dimensions,
                    timeBasedColors = timeBasedColors
                )
            }
        }
    }
}

@Composable
fun TimerPresetCard(
    preset: TimerPreset,
    onClick: () -> Unit,
    dimensions: ResponsiveDimensions,
    timeBasedColors: TimeBasedColors
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(90.dp)
            .height(70.dp),
        shape = RoundedCornerShape(dimensions.cardCornerRadius),
        color = timeBasedColors.cardBackgroundColor,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensions.spacingSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = preset.icon,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp
            )
            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelMedium,
                color = timeBasedColors.textPrimaryColor,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "${preset.minutes}:${preset.seconds.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall,
                color = timeBasedColors.textSecondaryColor
            )
        }
    }
}

@Composable
fun TimerDisplaySection(
    remainingTime: Int,
    totalTime: Int,
    timerState: TimerState,
    progress: Float,
    pulseScale: Float,
    rotateAngle: Float,
    dimensions: ResponsiveDimensions,
    timeBasedColors: TimeBasedColors
) {
    Box(
        modifier = Modifier
            .size(250.dp)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawCircularProgress(
                progress = progress,
                strokeWidth = 12.dp.toPx(),
                color = when (timerState) {
                    TimerState.RUNNING -> timeBasedColors.cardContentColor
                    TimerState.PAUSED -> timeBasedColors.textSecondaryColor
                    TimerState.FINISHED -> Color.Red
                    else -> timeBasedColors.textSecondaryColor.copy(alpha = 0.3f)
                },
                backgroundColor = timeBasedColors.textSecondaryColor.copy(alpha = 0.1f)
            )
        }

        // Timer text display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatTime(remainingTime),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = when (timerState) {
                    TimerState.FINISHED -> Color.Red
                    TimerState.RUNNING -> timeBasedColors.textPrimaryColor
                    TimerState.PAUSED -> timeBasedColors.textPrimaryColor.copy(alpha = 0.7f)
                    else -> timeBasedColors.textPrimaryColor
                },
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(dimensions.spacingXSmall))

            Text(
                text = when (timerState) {
                    TimerState.STOPPED -> "Ready to start"
                    TimerState.RUNNING -> "Running..."
                    TimerState.PAUSED -> "Paused"
                    TimerState.FINISHED -> "Time's Up!"
                },
                style = MaterialTheme.typography.titleSmall,
                color = when (timerState) {
                    TimerState.FINISHED -> Color.Red
                    TimerState.RUNNING -> timeBasedColors.cardContentColor
                    else -> timeBasedColors.textSecondaryColor
                },
                fontWeight = FontWeight.Medium
            )

            if (timerState != TimerState.STOPPED && totalTime > 0) {
                Text(
                    text = "of ${formatTime(totalTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = timeBasedColors.textSecondaryColor,
                    modifier = Modifier.padding(top = dimensions.spacingXSmall)
                )
            }
        }
    }
}

@Composable
fun TimeAdjustmentSection(
    minutes: Int,
    seconds: Int,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    dimensions: ResponsiveDimensions,
    timeBasedColors: TimeBasedColors
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
    ) {
        Text(
            text = "Set Timer Duration",
            style = MaterialTheme.typography.titleSmall,
            color = timeBasedColors.textPrimaryColor,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeControlGroup(
                label = "Minutes",
                value = minutes,
                onIncrement = { if (minutes < 99) onMinutesChange(minutes + 1) },
                onDecrement = { if (minutes > 0) onMinutesChange(minutes - 1) },
                dimensions = dimensions,
                timeBasedColors = timeBasedColors
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.displaySmall,
                color = timeBasedColors.textPrimaryColor,
                fontWeight = FontWeight.Bold
            )

            TimeControlGroup(
                label = "Seconds",
                value = seconds,
                onIncrement = { if (seconds < 59) onSecondsChange(seconds + 1) },
                onDecrement = { if (seconds > 0) onSecondsChange(seconds - 1) },
                dimensions = dimensions,
                timeBasedColors = timeBasedColors
            )
        }
    }
}

@Composable
fun TimeControlGroup(
    label: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    dimensions: ResponsiveDimensions,
    timeBasedColors: TimeBasedColors
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingXSmall)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = timeBasedColors.textSecondaryColor,
            fontWeight = FontWeight.Medium
        )

        Surface(
            modifier = Modifier
                .size(30.dp)
                .clickable { onIncrement() },
            shape = CircleShape,
            color = timeBasedColors.cardContentColor.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase $label",
                    tint = timeBasedColors.cardContentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineMedium,
            color = timeBasedColors.textPrimaryColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(50.dp)
        )

        Surface(
            modifier = Modifier
                .size(30.dp)
                .clickable { onDecrement() },
            shape = CircleShape,
            color = timeBasedColors.cardContentColor.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease $label",
                    tint = timeBasedColors.cardContentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Helper functions
fun DrawScope.drawCircularProgress(
    progress: Float,
    strokeWidth: Float,
    color: Color,
    backgroundColor: Color
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = (size.minDimension - strokeWidth) / 2

    // Background circle
    drawCircle(
        color = backgroundColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Progress arc
    if (progress > 0f) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

fun triggerAlarm(context: Context, mediaPlayer: MediaPlayer?, hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    // Play alarm sound
    try {
        mediaPlayer?.start()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Trigger haptic feedback
    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

    // Trigger vibration
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 1000)
        val effect = VibrationEffect.createWaveform(pattern, 0)
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 1000)
        vibrator.vibrate(pattern, 0)
    }
}

fun stopAlarm(mediaPlayer: MediaPlayer?) {
    try {
        mediaPlayer?.pause()
        mediaPlayer?.seekTo(0)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
