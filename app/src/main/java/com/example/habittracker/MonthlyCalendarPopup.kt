package com.example.habittracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonthlyCalendarPopup(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    habits: List<Habit>
) {
    val dimensions = LocalResponsiveDimensions.current
    val timeBasedColors = LocalTimeBasedColors.current

    // Calendar navigation states
    var currentCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply { time = selectedDate })
    }
    var currentSelectedDate by remember { mutableStateOf(selectedDate) }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.75f),
                shape = RoundedCornerShape(dimensions.cardCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = timeBasedColors.cardBackgroundColor
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensions.spacingLarge)
                ) {
                    // Header with close button and today button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Date",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize * dimensions.titleTextScale
                            ),
                            fontWeight = FontWeight.Bold,
                            color = timeBasedColors.textPrimaryColor
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                        ) {
                            // Today button
                            IconButton(
                                onClick = {
                                    val today = Date()
                                    currentCalendar = Calendar.getInstance().apply { time = today }
                                    currentSelectedDate = today
                                },
                                modifier = Modifier.size(dimensions.iconButtonSize)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = "Go to Today",
                                    tint = timeBasedColors.cardContentColor,
                                    modifier = Modifier.size(dimensions.iconSize)
                                )
                            }

                            // Close button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(dimensions.iconButtonSize)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = timeBasedColors.textPrimaryColor,
                                    modifier = Modifier.size(dimensions.iconSize)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacingLarge))

                    // Month/Year navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous month button
                        IconButton(
                            onClick = {
                                currentCalendar = Calendar.getInstance().apply {
                                    timeInMillis = currentCalendar.timeInMillis
                                    add(Calendar.MONTH, -1)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Previous Month",
                                tint = timeBasedColors.cardContentColor,
                                modifier = Modifier.size(dimensions.iconSize)
                            )
                        }

                        // Month and Year display (clickable for quick navigation)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
                            val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

                            Text(
                                text = monthFormat.format(currentCalendar.time),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize * dimensions.titleTextScale
                                ),
                                fontWeight = FontWeight.Bold,
                                color = timeBasedColors.textPrimaryColor,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = yearFormat.format(currentCalendar.time),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize * dimensions.bodyTextScale
                                ),
                                color = timeBasedColors.textSecondaryColor,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Next month button
                        IconButton(
                            onClick = {
                                currentCalendar = Calendar.getInstance().apply {
                                    timeInMillis = currentCalendar.timeInMillis
                                    add(Calendar.MONTH, 1)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Next Month",
                                tint = timeBasedColors.cardContentColor,
                                modifier = Modifier.size(dimensions.iconSize)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacingLarge))

                    // Days of week header
                    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * dimensions.bodyTextScale
                                ),
                                fontWeight = FontWeight.Medium,
                                color = timeBasedColors.cardContentColor,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))

                    // Calendar grid
                    val currentMonth = currentCalendar.get(Calendar.MONTH)
                    val currentYear = currentCalendar.get(Calendar.YEAR)

                    // Get first day of month and calculate starting position
                    val firstDayCalendar = Calendar.getInstance().apply {
                        set(currentYear, currentMonth, 1)
                    }
                    val firstDayOfWeek = firstDayCalendar.get(Calendar.DAY_OF_WEEK) - 1
                    val daysInMonth = firstDayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

                    // Create list of dates including empty cells for proper alignment
                    val calendarDates = mutableListOf<Date?>()
                    repeat(firstDayOfWeek) { calendarDates.add(null) } // Empty cells for previous month

                    for (day in 1..daysInMonth) {
                        firstDayCalendar.set(Calendar.DAY_OF_MONTH, day)
                        calendarDates.add(Date(firstDayCalendar.timeInMillis))
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                    ) {
                        items(calendarDates) { date ->
                            if (date != null) {
                                CalendarDateCard(
                                    date = date,
                                    selectedDate = currentSelectedDate,
                                    habits = habits,
                                    onDateSelected = { newDate ->
                                        currentSelectedDate = newDate
                                    },
                                    dimensions = dimensions,
                                    timeBasedColors = timeBasedColors
                                )
                            } else {
                                Spacer(modifier = Modifier.aspectRatio(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacingLarge))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Cancel",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * dimensions.bodyTextScale
                                )
                            )
                        }
                        Button(
                            onClick = {
                                onDateSelected(currentSelectedDate)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Select",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * dimensions.bodyTextScale
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDateCard(
    date: Date,
    selectedDate: Date,
    habits: List<Habit>,
    onDateSelected: (Date) -> Unit,
    dimensions: ResponsiveDimensions,
    timeBasedColors: TimeBasedColors
) {
    val calendar = Calendar.getInstance().apply { time = date }
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val today = Date()

    val isSelected = isSameDay(date.time, selectedDate.time)
    val isToday = isSameDay(date.time, today.time)
    val isFuture = date.after(today)

    // Check if any habits are completed for this date
    val hasCompletedHabits = habits.any { habit ->
        habit.completedDates.any { completedDate ->
            isSameDay(completedDate, date.time)
        }
    }

    // Calculate completion percentage for this date
    val completionCount = habits.count { habit ->
        habit.completedDates.any { completedDate ->
            isSameDay(completedDate, date.time)
        }
    }
    val totalHabits = habits.size
    val completionPercentage = if (totalHabits > 0) completionCount.toFloat() / totalHabits else 0f

    CalendarDayItem(
        date = date,
        isSelected = isSelected,
        isToday = isToday,
        hasHabits = hasCompletedHabits,
        onDateSelected = onDateSelected
    )
}

@Composable
private fun CalendarDayItem(
    date: Date,
    isSelected: Boolean,
    isToday: Boolean,
    hasHabits: Boolean,
    onDateSelected: (Date) -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current
    val calendar = Calendar.getInstance().apply { time = date }
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

    Surface(
        onClick = { onDateSelected(date) },
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
        color = when {
            isSelected -> timeBasedColors.cardContentColor
            isToday -> timeBasedColors.cardContentColor.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isSelected -> Color.White
                        isToday -> timeBasedColors.cardContentColor
                        else -> timeBasedColors.textPrimaryColor
                    },
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                )

                if (hasHabits && !isSelected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                color = timeBasedColors.cardContentColor,
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
