package com.example.habittrackerr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Calendar
import java.util.Date

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitDao: HabitDao
) : ViewModel() {

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    private val _selectedHabit = MutableStateFlow<Habit?>(null)
    val selectedHabit: StateFlow<Habit?> = _selectedHabit.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    // Selected date for calendar functionality
    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    init {
        viewModelScope.launch {
            habitDao.getAllHabitsFlow().collect {
                _habits.value = it
            }
        }
    }

    /**
     * Adds a new habit to the database
     */
    fun addHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.insertHabit(habit)
        }
    }

    /**
     * Updates an existing habit in the database
     */
    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.updateHabit(habit)
        }
    }

    /**
     * Deletes a habit from the database
     */
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
        }
    }

    /**
     * Deletes a habit by its ID
     */
    fun deleteHabitById(id: Int) {
        viewModelScope.launch {
            habitDao.deleteHabitById(id)
        }
    }

    /**
     * Marks a habit as completed for a specific date
     * @param habit The habit to mark as completed
     * @param date The timestamp for the completion date
     */
    fun markHabitAsCompleted(habit: Habit, date: Long) {
        viewModelScope.launch {
            // Create a mutable copy of the habit's completedDates list
            val updatedCompletedDates = habit.completedDates.toMutableList()

            // Add the new date timestamp to this list
            updatedCompletedDates.add(date)

            // Create a copy of the original habit object with the updated completedDates list
            val updatedHabit = habit.copy(completedDates = updatedCompletedDates)

            // Call the habitDao.update() function with the updated habit object
            habitDao.updateHabit(updatedHabit)
        }
    }

    /**
     * Undoes the habit completion for today
     */
    fun undoHabitCompletion(habit: Habit) {
        viewModelScope.launch {
            val today = System.currentTimeMillis()

            // Remove all completion timestamps from today
            val updatedCompletedDates = habit.completedDates.filter { completedDate ->
                !isSameDay(completedDate, today)
            }

            // Update the habit with the filtered list
            val updatedHabit = habit.copy(completedDates = updatedCompletedDates)
            habitDao.updateHabit(updatedHabit)
        }
    }

    /**
     * Helper function to check if two timestamps are on the same calendar day
     */
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Shows the dialog for editing an existing habit
     */
    fun showEditDialog(habit: Habit) {
        _selectedHabit.value = habit
        _showDialog.value = true
    }

    /**
     * Shows the dialog for adding a new habit
     */
    fun showAddDialog() {
        _selectedHabit.value = null
        _showDialog.value = true
    }

    /**
     * Hides the dialog
     */
    fun hideDialog() {
        _showDialog.value = false
        _selectedHabit.value = null
    }

    /**
     * Updates the selected date for calendar functionality
     */
    fun updateSelectedDate(date: Date) {
        _selectedDate.value = date
    }

    /**
     * Marks a habit as completed for the currently selected date
     */
    fun markHabitAsCompletedForSelectedDate(habit: Habit) {
        markHabitAsCompleted(habit, _selectedDate.value.time)
    }

    /**
     * Undoes the habit completion for the currently selected date
     */
    fun undoHabitCompletionForSelectedDate(habit: Habit) {
        viewModelScope.launch {
            val selectedDateTimestamp = _selectedDate.value.time

            // Remove completion timestamps from the selected date
            val updatedCompletedDates = habit.completedDates.filter { completedDate ->
                !isSameDay(completedDate, selectedDateTimestamp)
            }

            // Update the habit with the filtered list
            val updatedHabit = habit.copy(completedDates = updatedCompletedDates)
            habitDao.updateHabit(updatedHabit)
        }
    }

    /**
     * Checks if a habit is completed for the currently selected date
     */
    fun isHabitCompletedForSelectedDate(habit: Habit): Boolean {
        val selectedDateTimestamp = _selectedDate.value.time
        return habit.completedDates.any { completedDate ->
            isSameDay(completedDate, selectedDateTimestamp)
        }
    }
}
