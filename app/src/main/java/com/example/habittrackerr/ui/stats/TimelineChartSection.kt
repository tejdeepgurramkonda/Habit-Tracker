package com.example.habittrackerr.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habittrackerr.LocalTimeBasedColors
import com.example.habittrackerr.stats.TimelineState
import com.example.habittrackerr.stats.ChartDataPoint
import kotlin.math.max

@Composable
fun TimelineChartSection(
    timelineState: TimelineState,
    onDataPointClick: (ChartDataPoint) -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    color = timeBasedColors.textPrimaryColor,
                    fontWeight = FontWeight.SemiBold
                )

                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = "Chart",
                    tint = timeBasedColors.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                timelineState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                timelineState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = "No data",
                                tint = timeBasedColors.textSecondaryColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Unable to load chart data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = timeBasedColors.textSecondaryColor
                            )
                        }
                    }
                }

                timelineState.chartData.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No data available for selected period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = timeBasedColors.textSecondaryColor
                        )
                    }
                }

                else -> {
                    LineChart(
                        data = timelineState.chartData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        onDataPointClick = onDataPointClick
                    )
                }
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    onDataPointClick: (ChartDataPoint) -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current
    val primaryColor = timeBasedColors.accentColor
    val secondaryColor = timeBasedColors.textSecondaryColor

    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it.value } ?: 1f
    val minValue = data.minOfOrNull { it.value } ?: 0f
    val valueRange = max(maxValue - minValue, 1f)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clickable { /* Handle overall chart click */ }
        ) {
            val width = size.width
            val height = size.height
            val padding = 40.dp.toPx()

            val chartWidth = width - (padding * 2)
            val chartHeight = height - (padding * 2)

            // Draw grid lines
            drawGridLines(
                width = chartWidth,
                height = chartHeight,
                offsetX = padding,
                offsetY = padding,
                color = secondaryColor.copy(alpha = 0.2f)
            )

            // Create path for line chart
            val path = Path()
            val points = mutableListOf<Offset>()

            data.forEachIndexed { index, point ->
                val x = padding + (index.toFloat() / (data.size - 1)) * chartWidth
                val y = padding + chartHeight - ((point.value - minValue) / valueRange) * chartHeight

                points.add(Offset(x, y))

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Draw line
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw data points
            points.forEachIndexed { index, point ->
                drawCircle(
                    color = primaryColor,
                    radius = 6.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }
        }

        // Overlay for click detection on data points
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 40.dp)
        ) {
            data.forEachIndexed { index, dataPoint ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onDataPointClick(dataPoint) }
                )
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.take(5).forEach { point -> // Show max 5 labels to avoid crowding
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = timeBasedColors.textSecondaryColor
                )
            }
        }
    }
}

private fun DrawScope.drawGridLines(
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color
) {
    val horizontalLines = 4
    val verticalLines = 6

    // Horizontal grid lines
    repeat(horizontalLines) { i ->
        val y = offsetY + (i.toFloat() / (horizontalLines - 1)) * height
        drawLine(
            color = color,
            start = Offset(offsetX, y),
            end = Offset(offsetX + width, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Vertical grid lines
    repeat(verticalLines) { i ->
        val x = offsetX + (i.toFloat() / (verticalLines - 1)) * width
        drawLine(
            color = color,
            start = Offset(x, offsetY),
            end = Offset(x, offsetY + height),
            strokeWidth = 1.dp.toPx()
        )
    }
}
