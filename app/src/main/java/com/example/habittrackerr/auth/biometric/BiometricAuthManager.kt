package com.example.habittrackerr.auth.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _biometricState = MutableStateFlow(BiometricState())
    val biometricState: StateFlow<BiometricState> = _biometricState.asStateFlow()

    private var biometricPrompt: BiometricPrompt? = null
    private var promptInfo: PromptInfo? = null

    /**
     * Checks if biometric authentication is available on the device
     */
    fun isBiometricAvailable(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)

        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricAvailability.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricAvailability.SECURITY_UPDATE_REQUIRED
            else -> BiometricAvailability.NOT_AVAILABLE
        }
    }

    /**
     * Initialize biometric state
     */
    fun initializeBiometric() {
        val availability = isBiometricAvailable()
        _biometricState.value = _biometricState.value.copy(
            isAvailable = availability == BiometricAvailability.AVAILABLE
        )
    }

    /**
     * Enable biometric authentication
     */
    suspend fun enableBiometricAuth() {
        val availability = isBiometricAvailable()
        if (availability == BiometricAvailability.AVAILABLE) {
            _biometricState.value = _biometricState.value.copy(
                isEnabled = true,
                enabledAt = java.time.LocalDateTime.now()
            )
        }
    }

    /**
     * Get available biometric types
     */
    private fun getAvailableBiometricTypes(): List<BiometricType> {
        val types = mutableListOf<BiometricType>()
        val biometricManager = BiometricManager.from(context)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                types.add(BiometricType.FINGERPRINT)
            }
        }
        return types
    }

    /**
     * Gets biometric authentication statistics
     */
    fun getBiometricStats(): BiometricStats {
        val state = _biometricState.value
        return BiometricStats(
            isEnabled = state.isEnabled,
            availableTypes = getAvailableBiometricTypes(),
            totalAuthentications = state.authenticationCount,
            failedAttempts = state.failedAttempts,
            lastAuthenticationTime = state.lastAuthenticationTime?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            enabledAt = state.enabledAt?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
    }

    /**
     * Authenticates user with biometrics
     */
    fun authenticateWithBiometric(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Use your biometric credential to authenticate",
        description: String = "Place your finger on the sensor or look at the front camera",
        negativeButtonText: String = "Cancel",
        onSuccess: (BiometricCredentials) -> Unit,
        onError: (AuthError) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val availability = isBiometricAvailable()
        if (availability != BiometricAvailability.AVAILABLE) {
            onError(createBiometricError(availability))
            return
        }

        _biometricState.value = _biometricState.value.copy(
            isPromptShowing = true,
            isLoading = true
        )

        promptInfo = PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(true)
            .build()

        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                activity.lifecycleScope.launch {
                    _biometricState.value = _biometricState.value.copy(
                        isPromptShowing = false,
                        isLoading = false,
                        lastAuthenticationTime = java.time.LocalDateTime.now(),
                        authenticationCount = _biometricState.value.authenticationCount + 1
                    )

                    val credentials = BiometricCredentials(
                        cryptoObject = result.cryptoObject
                    )
                    onSuccess(credentials)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)

                activity.lifecycleScope.launch {
                    _biometricState.value = _biometricState.value.copy(
                        isPromptShowing = false,
                        isLoading = false,
                        failedAttempts = _biometricState.value.failedAttempts + 1
                    )

                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onCancel()
                        else -> {
                            val authError = AuthError(
                                code = errorCode,
                                message = errString.toString(),
                                type = AuthErrorType.BIOMETRIC_ERROR
                            )
                            onError(authError)
                        }
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()

                activity.lifecycleScope.launch {
                    _biometricState.value = _biometricState.value.copy(
                        failedAttempts = _biometricState.value.failedAttempts + 1
                    )
                }
            }
        }

        biometricPrompt = BiometricPrompt(activity,
            androidx.core.content.ContextCompat.getMainExecutor(context),
            authenticationCallback
        )

        biometricPrompt?.authenticate(promptInfo!!)
    }

    private fun createBiometricError(availability: BiometricAvailability): AuthError {
        return when (availability) {
            BiometricAvailability.NOT_AVAILABLE -> AuthError(
                code = -1,
                message = "Biometric authentication is not available on this device",
                type = AuthErrorType.BIOMETRIC_ERROR
            )
            BiometricAvailability.NOT_ENROLLED -> AuthError(
                code = -2,
                message = "No biometric credentials enrolled",
                type = AuthErrorType.BIOMETRIC_ERROR
            )
            BiometricAvailability.HARDWARE_UNAVAILABLE -> AuthError(
                code = -3,
                message = "Biometric hardware is temporarily unavailable",
                type = AuthErrorType.BIOMETRIC_ERROR
            )
            BiometricAvailability.SECURITY_UPDATE_REQUIRED -> AuthError(
                code = -4,
                message = "Security update required for biometric authentication",
                type = AuthErrorType.BIOMETRIC_ERROR
            )
            else -> AuthError(
                code = -5,
                message = "Unknown biometric error",
                type = AuthErrorType.UNKNOWN
            )
        }
    }
}

data class BiometricStats(
    val isEnabled: Boolean,
    val availableTypes: List<BiometricType>,
    val totalAuthentications: Int,
    val failedAttempts: Int,
    val lastAuthenticationTime: Long?,
    val enabledAt: Long?
)
