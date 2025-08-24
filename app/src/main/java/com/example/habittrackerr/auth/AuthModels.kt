package com.example.habittrackerr.auth

import java.time.LocalDateTime

// Simplified User data model without Firebase dependencies
data class User(
    val uid: String,
    val id: String = uid, // Add id property that maps to uid for compatibility
    val email: String,
    val displayName: String,
    val emailVerified: Boolean = true,
    val isEmailVerified: Boolean = true, // Add this property for compatibility
    val profileImageUrl: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastLoginAt: LocalDateTime? = null
)

// Authentication state with all required properties
data class AuthState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val user: User? = null,
    val requiresMfa: Boolean = false,
    val sessionExpiresAt: LocalDateTime? = null,
    val securityFlags: List<SecurityFlag> = emptyList()
)

// Simplified UI state for auth forms
data class AuthUiState(
    val currentScreen: AuthScreen = AuthScreen.LOGIN,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val isSignUpMode: Boolean = false // Add this property for LoginScreen compatibility
)

enum class AuthScreen {
    LOGIN,
    SIGNUP,
    FORGOT_PASSWORD
}

// Form states with all required properties
data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val rememberDevice: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isEmailValid: Boolean = false,
    val isPasswordValid: Boolean = false,
    val isValid: Boolean = false
)

data class SignUpFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val displayNameError: String? = null,
    val isEmailValid: Boolean = false,
    val isPasswordValid: Boolean = false,
    val isConfirmPasswordValid: Boolean = false,
    val isDisplayNameValid: Boolean = false,
    val isValid: Boolean = false
)

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmailSent: Boolean = false
)

// Auth error handling
data class AuthError(
    val type: AuthErrorType,
    val message: String,
    val isRetryable: Boolean = false,
    val suggestedAction: String? = null
)

enum class AuthErrorType {
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    EMAIL_ALREADY_EXISTS,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    NETWORK_ERROR,
    TOO_MANY_ATTEMPTS,
    BIOMETRIC_ERROR,
    BIOMETRIC_NOT_AVAILABLE,
    BIOMETRIC_NOT_ENROLLED,
    BIOMETRIC_CANCELLED,
    UNKNOWN_ERROR,
    MFA_REQUIRED,
    SESSION_EXPIRED
}

// Security and MFA
enum class MfaMethod {
    SMS,
    EMAIL,
    AUTHENTICATOR_APP,
    BACKUP_CODES
}

data class SecurityFlag(
    val type: SecurityFlagType,
    val message: String,
    val severity: SecuritySeverity = SecuritySeverity.LOW,
    val suspiciousActivity: Boolean = false
)

enum class SecurityFlagType {
    SUSPICIOUS_LOCATION,
    NEW_DEVICE,
    MULTIPLE_FAILED_ATTEMPTS,
    UNUSUAL_ACTIVITY
}

enum class SecuritySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

// Device info
data class DeviceInfo(
    val id: String,
    val name: String,
    val type: String,
    val model: String,
    val osVersion: String,
    val appVersion: String,
    val isCurrentDevice: Boolean = false,
    val lastUsed: LocalDateTime? = null,
    val isTrusted: Boolean = false
)

// Validation results
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
