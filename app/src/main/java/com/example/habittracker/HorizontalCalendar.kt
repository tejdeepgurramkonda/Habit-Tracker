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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class CalendarDay(
    val date: Date,
    val dayOfWeek: String,
    val dayOfMonth: Int,
    val isSelected: Boolean = false,
    val isToday: Boolean = false,
    val isPastDate: Boolean = false,
    val isFutureDate: Boolean = false
)

@Composable
fun HorizontalCalendar(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeBasedColors = LocalTimeBasedColors.current
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()

    // Generate all days of the current month
    // Update when selectedDate changes to refresh isSelected state
    val days = remember(selectedDate) {
        val calendar = Calendar.getInstance()
        mutableListOf<CalendarDay>().apply {
            // Set calendar to the first day of the selected month
            calendar.time = selectedDate
            calendar.set(Calendar.DAY_OF_MONTH, 1)

            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            // Generate all days in the month
            while (calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear) {
                val currentDate = calendar.time
                val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(currentDate)
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

                // Check if this date is today
                val isToday = isSameDay(currentDate.time, today.timeInMillis)
                val isSelected = isSameDay(currentDate.time, selectedDate.time)

                // Check if this is a past or future date
                val isPastDate = currentDate.time < today.timeInMillis && !isToday
                val isFutureDate = currentDate.time > today.timeInMillis && !isToday

                add(
                    CalendarDay(
                        date = Date(currentDate.time),
                        dayOfWeek = dayOfWeek,
                        dayOfMonth = dayOfMonth,
                        isSelected = isSelected,
                        isToday = isToday,
                        isPastDate = isPastDate,
                        isFutureDate = isFutureDate
                    )
                )

                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
    }

    val listState = rememberLazyListState()

    // Calculate proper centering offset to align today's date below the Today button
    val centerOffset = remember {
        // Calculate offset to center the selected item perfectly below the Today button
        // Screen center minus half of item width (45dp/2 = 22.5dp) ≈ -100 to -120
        -550// Optimized for centering below Today button
    }

    // Initial scroll to center today's date when calendar first loads
    LaunchedEffect(Unit) {
        val todayIndex = days.indexOfFirst { it.isToday }
        if (todayIndex >= 0) {
            // Use scrollToItem immediately to position today's date below Today button
            listState.scrollToItem(
                index = todayIndex,
                scrollOffset = centerOffset
            )
        }
    }

    // Scroll to selected date when it changes (especially for Today button)
    LaunchedEffect(selectedDate) {
        val selectedIndex = days.indexOfFirst { isSameDay(it.date.time, selectedDate.time) }
        if (selectedIndex >= 0) {
            // Always animate to center the selected date below the Today button
            listState.animateScrollToItem(
                index = selectedIndex,
                scrollOffset = centerOffset
            )
        }
    }

    // Remove Card wrapper - place LazyRow directly on screen
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp), // Increased spacing
        contentPadding = PaddingValues(horizontal = 16.dp), // Increased padding for better scrolling
        userScrollEnabled = true // Explicitly enable user scrolling
    ) {
        items(days) { day ->
            CalendarDayCard(
                calendarDay = day,
                timeBasedColors = timeBasedColors,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun CalendarDayCard(
    calendarDay: CalendarDay,
    timeBasedColors: TimeBasedColors,
    onDateSelected: (Date) -> Unit
) {
    val backgroundColor = when {
        calendarDay.isSelected -> timeBasedColors.cardContentColor
        calendarDay.isToday -> timeBasedColors.cardContentColor.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val textColor = when {
        calendarDay.isSelected -> Color.White
        calendarDay.isToday -> timeBasedColors.cardContentColor
        else -> timeBasedColors.textPrimaryColor
    }

    // Apply opacity for past dates (completed dates)
    val cardOpacity = when {
        calendarDay.isPastDate -> 0.6f
        calendarDay.isFutureDate -> 1.0f
        else -> 1.0f
    }

    val contentAlpha = if (calendarDay.isPastDate) 0.6f else 1.0f

    // Simple Box approach without any complex border logic
    Box(
        modifier = Modifier
            .width(45.dp)
            .height(65.dp)
            .alpha(cardOpacity)
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .clickable { onDateSelected(calendarDay.date) },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Day of week (e.g., "Wed")
            Text(
                text = calendarDay.dayOfWeek.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Day of month (e.g., "17")
            Text(
                text = calendarDay.dayOfMonth.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper function to check if two timestamps are on the same calendar day
private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
