package com.example.habittrackerr.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.habittrackerr.R
import com.example.habittrackerr.auth.oneplus.OnePlusCompatibilityManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInClientWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val onePlusCompatibilityManager: OnePlusCompatibilityManager
) {
    private val credentialManager = CredentialManager.create(context)

    init {
        // Setup OnePlus compatibility on initialization
        onePlusCompatibilityManager.setupOnePlusCompatibility()
        onePlusCompatibilityManager.logDeviceInfo()
    }

    /**
     * Build Google ID option
     */
    private fun buildGoogleIdOption(): GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(context.getString(R.string.default_web_client_id))
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(true)
        .build()

    /**
     * Build Credential Request
     */
    fun getCredentialRequest(): GetCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(buildGoogleIdOption())
        .build()

    /**
     * Coroutine-friendly sign-in with OnePlus error handling
     */
    suspend fun signIn(request: GetCredentialRequest): GoogleIdTokenCredential =
        withContext(Dispatchers.IO) {
            try {
                val result: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                extractToken(result)
            } catch (e: GetCredentialException) {
                // Check if this is an OnePlus-specific error that we can handle
                if (onePlusCompatibilityManager.handleGooglePlayServicesError(e)) {
                    // Log the error but don't crash - the user can still use email/password auth
                    android.util.Log.w("GoogleSignInWrapper", "OnePlus Google Sign-In limitation detected, falling back to alternative auth methods")
                    throw OnePlusAuthException("Google Sign-In is limited on this device. Please use email/password authentication.", e)
                }
                throw e
            } catch (e: GoogleIdTokenParsingException) {
                if (onePlusCompatibilityManager.handleGooglePlayServicesError(e)) {
                    throw OnePlusAuthException("Google Sign-In is limited on this device. Please use email/password authentication.", e)
                }
                throw e
            } catch (e: SecurityException) {
                if (onePlusCompatibilityManager.handleGooglePlayServicesError(e)) {
                    throw OnePlusAuthException("Google Sign-In is limited on this device. Please use email/password authentication.", e)
                }
                throw e
            }
        }

    /**
     * Simplified sign-in method
     */
    suspend fun signIn(): GoogleIdTokenCredential = signIn(getCredentialRequest())

    /**
     * Extract token from result
     */
    private fun extractToken(result: GetCredentialResponse): GoogleIdTokenCredential {
        val credential = result.credential
        return when (credential) {
            is GoogleIdTokenCredential -> credential
            else -> GoogleIdTokenCredential.createFrom(credential.data)
        }
    }

    /**
     * Check availability
     */
    fun isAvailable(): Boolean = true

    /**
     * Sign-out
     */
    fun signOut() {
        // Clear local session or tokens
    }

    /**
     * Revoke access
     */
    fun revokeAccess() {
        // Handle revocation if needed via backend
    }
}
