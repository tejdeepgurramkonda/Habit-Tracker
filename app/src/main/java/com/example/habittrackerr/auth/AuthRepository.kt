package com.example.habittrackerr.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.habittrackerr.auth.biometric.BiometricAuthManager
import com.example.habittrackerr.auth.security.SecurityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityUtils: SecurityUtils,
    private val biometricManager: BiometricAuthManager
) {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val dataStore = context.dataStore

    // Preference keys
    private val SESSION_TOKEN_KEY = stringPreferencesKey("session_token")
    private val REMEMBER_DEVICE_KEY = booleanPreferencesKey("remember_device")
    private val LAST_LOGIN_KEY = longPreferencesKey("last_login")
    private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    private val USER_PASSWORD_HASH_KEY = stringPreferencesKey("user_password_hash")
    private val USER_DISPLAY_NAME_KEY = stringPreferencesKey("user_display_name")

    init {
        // Check for existing session on initialization
        CoroutineScope(Dispatchers.IO).launch {
            checkExistingSession()
        }
    }

    private suspend fun checkExistingSession() {
        try {
            println("AuthRepository: Checking existing session...")
            _authState.value = AuthState(isLoading = true) // Set loading state while checking

            dataStore.data.first().let { preferences ->
                val sessionToken = preferences[SESSION_TOKEN_KEY]
                val rememberDevice = preferences[REMEMBER_DEVICE_KEY] ?: false
                val userEmail = preferences[USER_EMAIL_KEY]
                val displayName = preferences[USER_DISPLAY_NAME_KEY]

                println("AuthRepository: Checking existing session - token exists: ${sessionToken != null}, rememberDevice: $rememberDevice")

                if (sessionToken != null && rememberDevice && userEmail != null) {
                    // Create user from stored data
                    val user = User(
                        uid = UUID.randomUUID().toString(),
                        email = userEmail,
                        displayName = displayName ?: userEmail.substringBefore("@"),
                        emailVerified = true,
                        profileImageUrl = null
                    )
                    _currentUser.value = user
                    _authState.value = AuthState(isAuthenticated = true, isLoading = false)
                    println("AuthRepository: Session restored for user: ${user.email}")
                } else {
                    println("AuthRepository: No valid session found, setting to unauthenticated")
                    // Don't call signOut() here - just set to unauthenticated state
                    _authState.value = AuthState(isAuthenticated = false, isLoading = false)
                }
            }
        } catch (e: Exception) {
            println("AuthRepository: Error checking session: ${e.message}")
            // Don't call signOut() on error - just set to unauthenticated state
            _authState.value = AuthState(isAuthenticated = false, isLoading = false)
        }
    }

    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
        rememberDevice: Boolean = false
    ): Result<User> {
        return try {
            _authState.value = AuthState(isLoading = true)

            // Simulate authentication delay
            delay(1000)

            // For demo purposes, create a simple validation
            // In a real app, you would validate against your backend or local storage
            val storedPasswordHash = dataStore.data.first()[USER_PASSWORD_HASH_KEY]
            val passwordHash = securityUtils.hashPassword(password)

            if (storedPasswordHash == null) {
                // First time user - store credentials
                dataStore.edit { preferences ->
                    preferences[USER_EMAIL_KEY] = email
                    preferences[USER_PASSWORD_HASH_KEY] = passwordHash
                    preferences[USER_DISPLAY_NAME_KEY] = email.substringBefore("@")
                }
            } else if (storedPasswordHash != passwordHash) {
                // Invalid password
                _authState.value = AuthState(
                    isLoading = false,
                    error = AuthError(AuthErrorType.INVALID_CREDENTIALS, "Invalid email or password")
                )
                return Result.failure(Exception("Invalid credentials"))
            }

            val user = User(
                uid = UUID.randomUUID().toString(),
                email = email,
                displayName = email.substringBefore("@"),
                emailVerified = true,
                profileImageUrl = null
            )

            // Store session
            if (rememberDevice) {
                val sessionToken = UUID.randomUUID().toString()
                dataStore.edit { preferences ->
                    preferences[SESSION_TOKEN_KEY] = sessionToken
                    preferences[REMEMBER_DEVICE_KEY] = rememberDevice
                    preferences[LAST_LOGIN_KEY] = System.currentTimeMillis()
                }
            }

            _currentUser.value = user
            _authState.value = AuthState(isAuthenticated = true, isLoading = false)

            println("AuthRepository: User signed in successfully: ${user.email}")
            Result.success(user)

        } catch (e: Exception) {
            _authState.value = AuthState(
                isLoading = false,
                error = AuthError(AuthErrorType.UNKNOWN_ERROR, e.message ?: "Sign in failed")
            )
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            println("AuthRepository: Signing out user")

            // Clear stored session but keep user credentials for future login
            dataStore.edit { preferences ->
                preferences.remove(SESSION_TOKEN_KEY)
                preferences[REMEMBER_DEVICE_KEY] = false
            }

            _currentUser.value = null
            _authState.value = AuthState(isAuthenticated = false, isLoading = false)

            println("AuthRepository: User signed out successfully")
        } catch (e: Exception) {
            println("AuthRepository: Error during sign out: ${e.message}")
        }
    }

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<User> {
        return try {
            _authState.value = AuthState(isLoading = true)

            // Simulate account creation delay
            delay(1000)

            val passwordHash = securityUtils.hashPassword(password)
            val userName = displayName ?: email.substringBefore("@")

            // Store user credentials
            dataStore.edit { preferences ->
                preferences[USER_EMAIL_KEY] = email
                preferences[USER_PASSWORD_HASH_KEY] = passwordHash
                preferences[USER_DISPLAY_NAME_KEY] = userName
            }

            val user = User(
                uid = UUID.randomUUID().toString(),
                email = email,
                displayName = userName,
                emailVerified = true,
                profileImageUrl = null
            )

            _currentUser.value = user
            _authState.value = AuthState(isAuthenticated = true, isLoading = false)

            println("AuthRepository: User account created successfully: ${user.email}")
            Result.success(user)

        } catch (e: Exception) {
            _authState.value = AuthState(
                isLoading = false,
                error = AuthError(AuthErrorType.UNKNOWN_ERROR, e.message ?: "Account creation failed")
            )
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            _authState.value = AuthState(isLoading = true)

            // Simulate password reset delay
            delay(1000)

            // In a real app, you would send an actual email or show instructions
            println("AuthRepository: Password reset email would be sent to: $email")

            _authState.value = AuthState(isLoading = false)
            Result.success(Unit)

        } catch (e: Exception) {
            _authState.value = AuthState(
                isLoading = false,
                error = AuthError(AuthErrorType.UNKNOWN_ERROR, e.message ?: "Password reset failed")
            )
            Result.failure(e)
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    suspend fun updateProfile(displayName: String?, profileImageUrl: String?): Result<User> {
        return try {
            val currentUser = _currentUser.value ?: return Result.failure(Exception("No user logged in"))

            val updatedUser = currentUser.copy(
                displayName = displayName ?: currentUser.displayName,
                profileImageUrl = profileImageUrl ?: currentUser.profileImageUrl
            )

            // Update stored display name
            if (displayName != null) {
                dataStore.edit { preferences ->
                    preferences[USER_DISPLAY_NAME_KEY] = displayName
                }
            }

            _currentUser.value = updatedUser
            Result.success(updatedUser)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set authenticated user (used by Firebase auth restoration)
     */
    suspend fun setAuthenticatedUser(user: User) {
        try {
            println("AuthRepository: Setting authenticated user from Firebase: ${user.email}")
            _currentUser.value = user
            _authState.value = AuthState(isAuthenticated = true, isLoading = false)

            // Store session info for persistence
            val sessionToken = UUID.randomUUID().toString()
            dataStore.edit { preferences ->
                preferences[SESSION_TOKEN_KEY] = sessionToken
                preferences[REMEMBER_DEVICE_KEY] = true // Firebase users should be remembered
                preferences[LAST_LOGIN_KEY] = System.currentTimeMillis()
                preferences[USER_EMAIL_KEY] = user.email
                preferences[USER_DISPLAY_NAME_KEY] = user.displayName
            }
        } catch (e: Exception) {
            println("AuthRepository: Error setting authenticated user: ${e.message}")
        }
    }
}
