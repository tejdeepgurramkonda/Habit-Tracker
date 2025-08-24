package com.example.habittrackerr.auth

// Missing classes and enums that were causing compilation errors

data class BiometricCredentials(
    val cryptoObject: androidx.biometric.BiometricPrompt.CryptoObject?
)

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val error: AuthError) : AuthResult()
}
