package com.example.habittracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HorizontalCalendar(
    habits: List<Habit>,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeBasedColors = LocalTimeBasedColors.current
    val calendar = Calendar.getInstance()
    val today = Date()

    // Generate dates for the current month
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    calendar.set(currentYear, currentMonth, 1)

    val datesInMonth = mutableListOf<Date>()
    val maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    for (day in 1..maxDayInMonth) {
        calendar.set(Calendar.DAY_OF_MONTH, day)
        datesInMonth.add(Date(calendar.timeInMillis))
    }

    val listState = rememberLazyListState()

    // Auto-scroll to today's date
    LaunchedEffect(Unit) {
        val todayIndex = datesInMonth.indexOfFirst { date ->
            isSameDay(date.time, today.time)
        }
        if (todayIndex != -1) {
            listState.animateScrollToItem(maxOf(0, todayIndex - 3))
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(datesInMonth) { date ->
            CalendarDateItem(
                date = date,
                habits = habits,
                isSelected = isSameDay(date.time, selectedDate.time),
                isToday = isSameDay(date.time, today.time),
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
private fun CalendarDateItem(
    date: Date,
    habits: List<Habit>,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current
    val calendar = Calendar.getInstance().apply { time = date }
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(date).uppercase()

    // Check if any habits are completed for this date
    val hasCompletedHabits = habits.any { habit ->
        habit.completedDates.any { completedDate ->
            isSameDay(completedDate, date.time)
        }
    }

    // Determine if this is a future date
    val isFuture = date.after(Date())

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 70.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                when {
                    isSelected -> timeBasedColors.cardContentColor
                    isToday -> timeBasedColors.cardContentColor.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayOfWeek,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isSelected -> Color.White
                    hasCompletedHabits && !isFuture -> timeBasedColors.textSecondaryColor.copy(alpha = 0.6f)
                    else -> timeBasedColors.textSecondaryColor
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    isSelected -> Color.White
                    hasCompletedHabits && !isFuture -> timeBasedColors.textPrimaryColor.copy(alpha = 0.6f)
                    else -> timeBasedColors.textPrimaryColor
                },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            // Completion indicator
            if (hasCompletedHabits && !isFuture) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            if (isSelected) Color.White else timeBasedColors.cardContentColor,
                            RoundedCornerShape(2.dp)
                        )
                )
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
