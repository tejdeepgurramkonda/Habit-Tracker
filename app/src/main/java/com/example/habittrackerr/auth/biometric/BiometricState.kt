package com.example.habittrackerr.auth.biometric

import java.time.LocalDateTime

/**
 * Represents the current state of biometric authentication
 */
data class BiometricState(
    val isEnabled: Boolean = false,
    val isAvailable: Boolean = false,
    val isPromptShowing: Boolean = false,
    val isLoading: Boolean = false,
    val authenticationCount: Int = 0,
    val failedAttempts: Int = 0,
    val lastAuthenticationTime: LocalDateTime? = null,
    val enabledAt: LocalDateTime? = null,
    val error: String? = null
)

/**
 * Available biometric authentication types
 */
enum class BiometricType {
    FINGERPRINT,
    FACE,
    IRIS,
    VOICE
}

/**
 * Biometric availability status
 */
enum class BiometricAvailability {
    AVAILABLE,
    NOT_AVAILABLE,
    NOT_ENROLLED,
    HARDWARE_UNAVAILABLE,
    SECURITY_UPDATE_REQUIRED
}

/**
 * Biometric authentication credentials
 */
data class BiometricCredentials(
    val cryptoObject: androidx.biometric.BiometricPrompt.CryptoObject? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Authentication error details
 */
data class AuthError(
    val code: Int,
    val message: String,
    val type: AuthErrorType = AuthErrorType.UNKNOWN
)

/**
 * Types of authentication errors
 */
enum class AuthErrorType {
    BIOMETRIC_ERROR,
    NETWORK_ERROR,
    VALIDATION_ERROR,
    UNKNOWN
}
