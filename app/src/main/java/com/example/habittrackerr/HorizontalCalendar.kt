package com.example.habittrackerr

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
    val dimensions = LocalResponsiveDimensions.current
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

    Column(
        modifier = Modifier
            .size(
                width = dimensions.calendarItemSize,
                height = dimensions.calendarItemSize + dimensions.spacingLarge
            )
            .clip(RoundedCornerShape(dimensions.cardCornerRadius))
            .background(
                color = when {
                    isSelected -> timeBasedColors.cardContentColor
                    isToday -> timeBasedColors.cardContentColor.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(dimensions.spacingMedium)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(dimensions.spacingSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = MaterialTheme.typography.labelSmall.fontSize * dimensions.captionTextScale
            ),
            color = when {
                isSelected -> Color.White
                isFuture -> timeBasedColors.textSecondaryColor.copy(alpha = 0.5f)
                hasCompletedHabits -> timeBasedColors.textPrimaryColor.copy(alpha = 0.6f)
                else -> timeBasedColors.textSecondaryColor
            },
            textAlign = TextAlign.Center,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(dimensions.spacingXSmall))

        Box(
            modifier = Modifier
                .size(dimensions.calendarItemSize - dimensions.spacingMedium)
                .clip(RoundedCornerShape(50))
                .background(
                    when {
                        isSelected -> Color.White.copy(alpha = 0.2f)
                        isToday -> timeBasedColors.cardContentColor.copy(alpha = 0.1f)
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * dimensions.bodyTextScale
                ),
                color = when {
                    isSelected -> Color.White
                    isFuture -> timeBasedColors.textPrimaryColor.copy(alpha = 0.5f)
                    hasCompletedHabits -> timeBasedColors.textPrimaryColor.copy(alpha = 0.6f)
                    else -> timeBasedColors.textPrimaryColor
                },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
