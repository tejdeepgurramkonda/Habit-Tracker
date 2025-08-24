package com.example.habittrackerr

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitDialog(
    habitViewModel: HabitViewModel,
    habit: Habit? = null,
    onDismiss: () -> Unit
) {
    val dimensions = LocalResponsiveDimensions.current
    val scope = rememberCoroutineScope()

    // Use rememberSaveable for state persistence across recompositions
    var name by rememberSaveable { mutableStateOf(habit?.name ?: "") }
    var selectedColor by rememberSaveable { mutableStateOf(habit?.colorHex ?: "#FF5733") }
    var selectedFrequency by rememberSaveable { mutableStateOf(habit?.frequencyType ?: "DAILY") }
    var selectedIcon by rememberSaveable { mutableStateOf(habit?.iconId ?: "🏃") }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    val colors = listOf(
        "#FF5733", "#33FF57", "#3357FF", "#FF33F5", "#F5FF33",
        "#33FFF5", "#F533FF", "#5733FF", "#FF3357", "#57FF33"
    )

    val icons = listOf("🏃", "💪", "📚", "🧘", "💧", "🚶", "🍎", "😴", "🎯", "✍️")

    val frequencies = listOf("DAILY", "WEEKLY")

    // Handle back button press
    BackHandler {
        if (!isSaving) {
            onDismiss()
        }
    }

    // Robust dismissal function
    val handleDismiss = {
        if (!isSaving) {
            onDismiss()
        }
    }

    // Non-blocking save function
    val handleSave = {
        if (name.isNotBlank() && !isSaving) {
            isSaving = true
            scope.launch {
                try {
                    val newHabit = if (habit == null) {
                        Habit(
                            id = 0,
                            name = name,
                            iconId = selectedIcon,
                            colorHex = selectedColor,
                            frequencyType = selectedFrequency,
                            frequencyValue = emptyList(),
                            createdAt = System.currentTimeMillis(),
                            completedDates = emptyList()
                        )
                    } else {
                        habit.copy(
                            name = name,
                            iconId = selectedIcon,
                            colorHex = selectedColor,
                            frequencyType = selectedFrequency
                        )
                    }

                    if (habit == null) {
                        habitViewModel.addHabit(newHabit)
                    } else {
                        habitViewModel.updateHabit(newHabit)
                    }

                    // Dismiss after successful save
                    onDismiss()
                } catch (e: Exception) {
                    // Handle save error gracefully
                    isSaving = false
                    // Could show error to user here
                }
            }
        }
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(dimensions.cardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions.spacingXLarge),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingLarge)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (habit == null) "Add Habit" else "Edit Habit",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize * dimensions.titleTextScale
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = handleDismiss,
                        enabled = !isSaving,
                        modifier = Modifier.size(dimensions.iconButtonSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(dimensions.iconSize)
                        )
                    }
                }

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            "Habit Name",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize * dimensions.bodyTextScale
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * dimensions.bodyTextScale
                    )
                )

                // Icon Selection
                Text(
                    text = "Select Icon",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize * dimensions.titleTextScale
                    ),
                    fontWeight = FontWeight.Medium
                )

                // Icons in two rows for better responsiveness
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
                    ) {
                        icons.take(5).forEach { icon ->
                            Box(
                                modifier = Modifier
                                    .size(dimensions.iconButtonSize)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedIcon == icon)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (selectedIcon == icon) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        indication = LocalIndication.current,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { selectedIcon = icon }
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = icon,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize * dimensions.titleTextScale
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
                    ) {
                        icons.drop(5).forEach { icon ->
                            Box(
                                modifier = Modifier
                                    .size(dimensions.iconButtonSize)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedIcon == icon)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (selectedIcon == icon) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        indication = LocalIndication.current,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { selectedIcon = icon }
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = icon,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize * dimensions.titleTextScale
                                    )
                                )
                            }
                        }
                    }
                }

                // Color Selection
                Text(
                    text = "Select Color",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize * dimensions.titleTextScale
                    ),
                    fontWeight = FontWeight.Medium
                )

                // Colors in two rows for better responsiveness
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
                    ) {
                        colors.take(5).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(dimensions.iconButtonSize)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        indication = LocalIndication.current,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { selectedColor = color }
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(dimensions.iconSize)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
                    ) {
                        colors.drop(5).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(dimensions.iconButtonSize)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        indication = LocalIndication.current,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { selectedColor = color }
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(dimensions.iconSize)
                                    )
                                }
                            }
                        }
                    }
                }

                // Frequency Selection
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize * dimensions.titleTextScale
                    ),
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
                ) {
                    frequencies.forEach { frequency ->
                        FilterChip(
                            onClick = { selectedFrequency = frequency },
                            label = {
                                Text(
                                    frequency,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * dimensions.bodyTextScale
                                    )
                                )
                            },
                            selected = selectedFrequency == frequency,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Action Buttons
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
                        onClick = handleSave,
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() && !isSaving
                    ) {
                        Text(
                            if (habit == null) "Add" else "Update",
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
