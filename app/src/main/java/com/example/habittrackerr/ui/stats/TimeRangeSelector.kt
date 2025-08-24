package com.example.habittrackerr.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habittrackerr.LocalTimeBasedColors
import com.example.habittrackerr.stats.TimeRange

@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current

    Column {
        Text(
            text = "Time Range",
            style = MaterialTheme.typography.titleMedium,
            color = timeBasedColors.textPrimaryColor,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRange.values().forEach { range ->
                FilterChip(
                    onClick = { onRangeSelected(range) },
                    label = {
                        Text(
                            text = when (range) {
                                TimeRange.DAY -> "Today"
                                TimeRange.WEEK -> "Week"
                                TimeRange.MONTH -> "Month"
                            }
                        )
                    },
                    selected = selectedRange == range,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
