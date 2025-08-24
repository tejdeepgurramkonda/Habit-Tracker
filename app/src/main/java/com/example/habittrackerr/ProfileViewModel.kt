package com.example.habittrackerr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val habitDao: HabitDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // User data flows
    val userData: StateFlow<UserData?> = currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            userRepository.getUserData(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val userProfile: StateFlow<UserProfile?> = userData
        .map { it?.profile }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Initialize with authenticated user - ALWAYS sync with Firebase Auth data
    fun initializeWithUser(userId: String, email: String, displayName: String) {
        viewModelScope.launch {
            _currentUserId.value = userId

            // Always update or create profile with authenticated user data
            val existingProfile = userRepository.getUserProfile(userId).first()
            if (existingProfile == null) {
                // Create new profile with authenticated user data
                println("ProfileViewModel: Creating new profile for $email with display name: $displayName")
                userRepository.createUserProfile(userId, email, displayName)
            } else {
                // Update existing profile with current authenticated user data to ensure sync
                println("ProfileViewModel: Updating existing profile with authenticated user data: $displayName")
                val updatedProfile = existingProfile.copy(
                    email = email,
                    displayName = displayName,
                    updatedAt = System.currentTimeMillis()
                )
                userRepository.updateUserProfile(updatedProfile)
            }

            // Sync statistics
            syncUserStatistics()
        }
    }

    fun updateProfile(updatedProfile: UserProfile) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                userRepository.updateUserProfile(updatedProfile)
                _uiState.value = _uiState.value.copy(isLoading = false, message = "Profile updated successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update profile: ${e.message}"
                )
            }
        }
    }

    fun updatePreferences(updatedPreferences: UserPreferences) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                userRepository.updateUserPreferences(updatedPreferences)
                _uiState.value = _uiState.value.copy(isLoading = false, message = "Preferences saved")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save preferences: ${e.message}"
                )
            }
        }
    }

    fun syncUserStatistics() {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value
                if (userId != null) {
                    userRepository.syncUserStatistics(userId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to sync statistics: ${e.message}"
                )
            }
        }
    }

    fun updateLastActiveDate() {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId != null) {
                userRepository.updateLastActiveDate(userId)
            }
        }
    }

    fun deleteUserData() {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value
                if (userId != null) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    userRepository.deleteUserData(userId)
                    _currentUserId.value = null
                    _uiState.value = _uiState.value.copy(isLoading = false, message = "User data deleted")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete user data: ${e.message}"
                )
            }
        }
    }

    fun exportUserData(): Flow<String> = flow {
        val userId = _currentUserId.value ?: return@flow
        val userData = userRepository.getUserData(userId).first()
        val habits = habitDao.getAllHabits().first()

        val exportData = mapOf(
            "userProfile" to userData?.profile,
            "userPreferences" to userData?.preferences,
            "habits" to habits,
            "exportDate" to System.currentTimeMillis()
        )

        emit(com.google.gson.Gson().toJson(exportData))
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    // Achievement system
    fun checkAndUnlockAchievements() {
        viewModelScope.launch {
            val currentUserId = _currentUserId.value ?: return@launch
            val profile = userProfile.value ?: return@launch
            val habits = habitDao.getAllHabits().first()

            val newAchievements = mutableListOf<String>()

            // Check various achievement conditions
            if (profile.currentStreak >= 7 && !profile.achievements.contains("WEEK_WARRIOR")) {
                newAchievements.add("WEEK_WARRIOR")
            }

            if (profile.currentStreak >= 30 && !profile.achievements.contains("MONTH_MASTER")) {
                newAchievements.add("MONTH_MASTER")
            }

            if (profile.totalHabits >= 10 && !profile.achievements.contains("HABIT_COLLECTOR")) {
                newAchievements.add("HABIT_COLLECTOR")
            }

            if (profile.totalCompletions >= 100 && !profile.achievements.contains("CENTURY_CLUB")) {
                newAchievements.add("CENTURY_CLUB")
            }

            if (newAchievements.isNotEmpty()) {
                val updatedProfile = profile.copy(
                    achievements = profile.achievements + newAchievements,
                    experiencePoints = profile.experiencePoints + (newAchievements.size * 50)
                )
                updateProfile(updatedProfile)

                _uiState.value = _uiState.value.copy(
                    newAchievements = newAchievements,
                    message = "🎉 New achievement${if (newAchievements.size > 1) "s" else ""} unlocked!"
                )
            }
        }
    }

    fun dismissAchievements() {
        _uiState.value = _uiState.value.copy(newAchievements = emptyList())
    }
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val newAchievements: List<String> = emptyList(),
    val showEditDialog: Boolean = false,
    val showPreferencesDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)
