package com.example.habittrackerr.auth

import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittrackerr.auth.biometric.BiometricAuthManager
import com.example.habittrackerr.auth.biometric.BiometricState
import com.example.habittrackerr.auth.security.SecurityUtils
import com.example.habittrackerr.data.firebase.FirebaseSyncService
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseSyncService: FirebaseSyncService,
    private val firebaseAuth: FirebaseAuth,
    private val securityUtils: SecurityUtils,
    private val biometricManager: BiometricAuthManager,
    private val googleSignInClient: GoogleSignInClientWrapper
) : ViewModel() {

    // Auth state from repository
    val authState: StateFlow<AuthState> = authRepository.authState
    val currentUser: StateFlow<User?> = authRepository.currentUser

    // UI state management
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Form states
    private val _loginFormState = MutableStateFlow(LoginFormState())
    val loginFormState: StateFlow<LoginFormState> = _loginFormState.asStateFlow()

    private val _signUpFormState = MutableStateFlow(SignUpFormState())
    val signUpFormState = _signUpFormState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordState())
    val forgotPasswordState = _forgotPasswordState.asStateFlow()

    // Biometric state
    val biometricState: StateFlow<BiometricState> = biometricManager.biometricState

    // Firebase-specific states
    private val _firebaseAuthState = MutableStateFlow(FirebaseAuthState())
    val firebaseAuthState: StateFlow<FirebaseAuthState> = _firebaseAuthState.asStateFlow()

    init {
        // Monitor auth state changes
        viewModelScope.launch {
            authState.collect { state ->
                updateUiStateFromAuthState(state)
            }
        }

        // Monitor Firebase auth state
        viewModelScope.launch {
            monitorFirebaseAuthState()
        }
    }

    private fun updateUiStateFromAuthState(state: AuthState) {
        _uiState.value = _uiState.value.copy(
            isLoading = state.isLoading,
            isAuthenticated = state.isAuthenticated,
            error = state.error?.message
        )
    }

    private suspend fun monitorFirebaseAuthState() {
        firebaseAuth.authStateReactor().collect { firebaseUser ->
            println("AuthViewModel: Firebase auth state changed - user: ${firebaseUser?.email ?: "null"}")

            if (firebaseUser != null) {
                // User is signed in with Firebase
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "",
                    emailVerified = firebaseUser.isEmailVerified,
                    profileImageUrl = firebaseUser.photoUrl?.toString()
                )

                // Update the repository's auth state to match Firebase
                authRepository.setAuthenticatedUser(user)
                println("AuthViewModel: Firebase user restored - ${user.email}")

                // Update user profile in Firestore and start sync
                try {
                    firebaseSyncService.createOrUpdateUserProfile(
                        email = user.email,
                        displayName = user.displayName,
                        photoUrl = user.profileImageUrl,
                        phoneNumber = firebaseUser.phoneNumber
                    )

                    // Start real-time sync
                    firebaseSyncService.startRealtimeSync()
                } catch (e: Exception) {
                    println("AuthViewModel: Error syncing user profile: ${e.message}")
                    // Don't fail authentication if sync fails
                }
            } else {
                println("AuthViewModel: Firebase user is null - user signed out from Firebase")
                // Only sign out from repository if this wasn't initiated by repository
                // This prevents infinite loops between Firebase and repository sign-out
            }
        }
    }

    // ============ MODERN GOOGLE SIGN-IN METHODS ============

    /**
     * Get Google Sign-In Client (modern Credential Manager wrapper)
     */
    fun getGoogleSignInClient(): GoogleSignInClientWrapper = googleSignInClient

    /**
     * Get Google Credential Request for modern API
     */
    fun getGoogleCredentialRequest(): GetCredentialRequest = googleSignInClient.getCredentialRequest()

    /**
     * Handle Google Sign-In with modern Credential Manager API
     */
    fun signInWithGoogleCredential(credential: GoogleIdTokenCredential) {
        viewModelScope.launch {
            _firebaseAuthState.value = _firebaseAuthState.value.copy(isLoading = true, error = null)

            try {
                // Extract ID token from the credential
                val idToken = credential.idToken

                // Create Firebase credential and sign in
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        viewModelScope.launch {
                            if (task.isSuccessful) {
                                _firebaseAuthState.value = _firebaseAuthState.value.copy(isLoading = false)
                                // Perform full sync after successful Google sign-in
                                firebaseSyncService.performFullSync()
                            } else {
                                _firebaseAuthState.value = _firebaseAuthState.value.copy(
                                    isLoading = false,
                                    error = task.exception?.message ?: "Google sign in failed"
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                _firebaseAuthState.value = _firebaseAuthState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Google sign in failed"
                )
            }
        }
    }

    /**
     * Handle Google Sign-In errors with OnePlus-specific messaging
     */
    fun handleGoogleSignInError(errorMessage: String) {
        // Check if this is an OnePlus-specific error
        val isOnePlusError = errorMessage.contains("Google Sign-In is limited on this device") ||
                            errorMessage.contains("Unknown calling package name") ||
                            errorMessage.contains("SecurityException")

        val userFriendlyMessage = if (isOnePlusError) {
            "Google Sign-In is not fully supported on your device. Please use email/password to sign in."
        } else {
            errorMessage
        }

        _firebaseAuthState.value = _firebaseAuthState.value.copy(
            isLoading = false,
            error = userFriendlyMessage
        )
    }

    /**
     * Simplified Google Sign-In method with OnePlus error handling
     */
    fun signInWithGoogle() {
        viewModelScope.launch {
            _firebaseAuthState.value = _firebaseAuthState.value.copy(isLoading = true, error = null)

            try {
                val credential = googleSignInClient.signIn()
                signInWithGoogleCredential(credential)
            } catch (e: OnePlusAuthException) {
                // Handle OnePlus-specific authentication limitations
                handleGoogleSignInError(e.message ?: "Google Sign-In is not fully supported on your device. Please use email/password to sign in.")
            } catch (e: GetCredentialException) {
                handleGoogleSignInError("Google sign-in failed: ${e.message}")
            } catch (e: SecurityException) {
                // This might be an OnePlus security exception
                handleGoogleSignInError("Google Sign-In is not fully supported on your device. Please use email/password to sign in.")
            } catch (e: Exception) {
                handleGoogleSignInError("Google sign-in failed: ${e.message}")
            }
        }
    }

    // ============ EMAIL/PASSWORD AUTHENTICATION ============

    fun signInWithEmailAndPassword(email: String, password: String, rememberDevice: Boolean = false) {
        viewModelScope.launch {
            _loginFormState.value = _loginFormState.value.copy(isLoading = true, error = null)

            try {
                // First try Firebase Auth
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        viewModelScope.launch {
                            if (task.isSuccessful) {
                                // Firebase sign-in successful
                                _loginFormState.value = _loginFormState.value.copy(isLoading = false)
                                // Perform full sync after successful login
                                firebaseSyncService.performFullSync()
                            } else {
                                // Fallback to local auth
                                val localResult = authRepository.signInWithEmailAndPassword(email, password, rememberDevice)
                                if (localResult.isSuccess) {
                                    _loginFormState.value = _loginFormState.value.copy(isLoading = false)
                                } else {
                                    _loginFormState.value = _loginFormState.value.copy(
                                        isLoading = false,
                                        error = task.exception?.message ?: "Sign in failed"
                                    )
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                _loginFormState.value = _loginFormState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Sign in failed"
                )
            }
        }
    }

    fun createUserWithEmailAndPassword(email: String, password: String, displayName: String? = null) {
        viewModelScope.launch {
            _signUpFormState.value = _signUpFormState.value.copy(isLoading = true, error = null)

            try {
                // Try Firebase Auth first
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        viewModelScope.launch {
                            if (task.isSuccessful) {
                                // Update display name if provided
                                displayName?.let { name ->
                                    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                        this.displayName = name
                                    }
                                    firebaseAuth.currentUser?.updateProfile(profileUpdates)
                                }
                                _signUpFormState.value = _signUpFormState.value.copy(isLoading = false)
                                // Start sync after account creation
                                firebaseSyncService.performFullSync()
                            } else {
                                // Fallback to local account creation
                                val localResult = authRepository.createUserWithEmailAndPassword(email, password, displayName)
                                if (localResult.isSuccess) {
                                    _signUpFormState.value = _signUpFormState.value.copy(isLoading = false)
                                } else {
                                    _signUpFormState.value = _signUpFormState.value.copy(
                                        isLoading = false,
                                        error = task.exception?.message ?: "Account creation failed"
                                    )
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                _signUpFormState.value = _signUpFormState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Account creation failed"
                )
            }
        }
    }

    // ============ FORM STATE MANAGEMENT ============

    fun toggleForm() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode
        )
    }

    fun updateEmail(email: String) {
        _loginFormState.value = _loginFormState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _loginFormState.value = _loginFormState.value.copy(password = password)
    }

    fun updateSignUpEmail(email: String) {
        _signUpFormState.value = _signUpFormState.value.copy(email = email)
    }

    fun updateSignUpPassword(password: String) {
        _signUpFormState.value = _signUpFormState.value.copy(password = password)
    }

    fun updateConfirmPassword(password: String) {
        _signUpFormState.value = _signUpFormState.value.copy(confirmPassword = password)
    }

    fun updateDisplayName(name: String) {
        _signUpFormState.value = _signUpFormState.value.copy(displayName = name)
    }

    fun signInWithEmail() {
        val formState = _loginFormState.value
        signInWithEmailAndPassword(formState.email, formState.password)
    }

    fun signUpWithEmail() {
        val formState = _signUpFormState.value
        createUserWithEmailAndPassword(formState.email, formState.password, formState.displayName)
    }

    // ============ NAVIGATION AND EMAIL VERIFICATION METHODS ============

    /**
     * Navigate back to login (toggle to login mode)
     */
    fun navigateBack() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = false,
            currentScreen = AuthScreen.LOGIN
        )
    }

    /**
     * Resend email verification
     */
    fun resendEmailVerification() {
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.sendEmailVerification()
                // Could add UI feedback here
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Check if email is verified and refresh user
     */
    fun checkEmailVerification() {
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.reload()
                // Auth state will be updated automatically through the auth state listener
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseAuth.signOut()
            googleSignInClient.signOut()
            authRepository.signOut()
            // Gracefully handle sync stopping - method may not exist in FirebaseSyncService yet
            try {
                // firebaseSyncService.stopRealtimeSync() // Comment out until method is implemented
            } catch (e: Exception) {
                // Handle gracefully if method doesn't exist yet
            }
        }
    }
}

// Extension function for Firebase auth state monitoring
private fun FirebaseAuth.authStateReactor(): Flow<com.google.firebase.auth.FirebaseUser?> = callbackFlow {
    val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(authStateListener)
    awaitClose { removeAuthStateListener(authStateListener) }
}

// Firebase-specific auth state
data class FirebaseAuthState(
    val isLoading: Boolean = false,
    val error: String? = null
)
