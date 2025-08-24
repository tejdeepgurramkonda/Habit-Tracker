package com.example.habittrackerr.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.coroutines.launch
import com.example.habittrackerr.DynamicGradientBackground
import com.example.habittrackerr.LocalTimeBasedColors
import androidx.navigation.NavHostController
import android.content.Intent
import android.app.Activity
import com.example.habittrackerr.MainActivity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    navController: NavHostController? = null
) {
    val authState by viewModel.authState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val loginFormState by viewModel.loginFormState.collectAsState()
    val signUpFormState by viewModel.signUpFormState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Modern Google Sign-In handler using Credential Manager API
    val handleGoogleSignIn: () -> Unit = {
        scope.launch {
            try {
                val googleSignInClient = viewModel.getGoogleSignInClient()
                val request = viewModel.getGoogleCredentialRequest()
                val credential = googleSignInClient.signIn(request)
                viewModel.signInWithGoogleCredential(credential)
            } catch (e: GetCredentialException) {
                viewModel.handleGoogleSignInError("Google sign-in failed: ${e.message}")
            } catch (e: Exception) {
                viewModel.handleGoogleSignInError("Google sign-in failed: ${e.message}")
            }
        }
    }

    // Navigate to main screen when authenticated
    LaunchedEffect(authState.isAuthenticated) {
        println("LoginScreen: Authentication state changed to: ${authState.isAuthenticated}")
        if (authState.isAuthenticated) {
            println("LoginScreen: User authenticated, navigating to main app")

            // Primary navigation method using NavController
            navController?.let { nav ->
                // Since we're in UnauthenticatedNavigation, the parent AppNavigation
                // will automatically switch to AuthenticatedAppNavigation when authState.isAuthenticated becomes true
                // But we can still trigger a smooth transition here if needed
                println("LoginScreen: Navigation controller available, parent will handle routing")
            } ?: run {
                // Fallback: Activity navigation if NavController is not available
                println("LoginScreen: No NavController available, using Activity fallback")
                try {
                    val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("navigate_to_main", true)
                    }
                    context.startActivity(mainActivityIntent)

                    // Finish current activity if we're in an Activity context
                    if (context is Activity) {
                        context.finish()
                    }
                } catch (e: Exception) {
                    println("LoginScreen: Activity navigation failed: ${e.message}")
                }
            }
        }
    }

    DynamicGradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Background blur effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF667eea).copy(alpha = 0.3f),
                                Color(0xFF764ba2).copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            radius = 800f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo and Title
                AppHeader()

                Spacer(modifier = Modifier.height(32.dp))

                // Login/SignUp Form
                LoginFormCard(
                    uiState = uiState,
                    loginForm = loginFormState,
                    signUpForm = signUpFormState,
                    onToggleForm = viewModel::toggleForm,
                    onEmailChange = viewModel::updateEmail,
                    onPasswordChange = viewModel::updatePassword,
                    onSignUpEmailChange = viewModel::updateSignUpEmail,
                    onSignUpPasswordChange = viewModel::updateSignUpPassword,
                    onConfirmPasswordChange = viewModel::updateConfirmPassword,
                    onDisplayNameChange = viewModel::updateDisplayName,
                    onLoginClick = viewModel::signInWithEmail,
                    onSignUpClick = viewModel::signUpWithEmail,
                    onGoogleSignInClick = handleGoogleSignIn
                )
            }
        }
    }
}

@Composable
private fun AppHeader() {
    val timeBasedColors = LocalTimeBasedColors.current
    var scale by remember { mutableStateOf(0.8f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ -> scale = value }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            timeBasedColors.cardContentColor,
                            timeBasedColors.cardContentColor.copy(alpha = 0.7f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.TrackChanges,
                contentDescription = "Habit Tracker",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Habit Tracker",
            style = MaterialTheme.typography.headlineMedium,
            color = timeBasedColors.textPrimaryColor,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Build Better Habits",
            style = MaterialTheme.typography.bodyLarge,
            color = timeBasedColors.textSecondaryColor,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginFormCard(
    uiState: AuthUiState,
    loginForm: LoginFormState,
    signUpForm: SignUpFormState,
    onToggleForm: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignUpEmailChange: (String) -> Unit,
    onSignUpPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    val timeBasedColors = LocalTimeBasedColors.current
    val isSignUp = uiState.isSignUpMode

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Form toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { if (isSignUp) onToggleForm() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (!isSignUp) timeBasedColors.accentColor else timeBasedColors.textSecondaryColor
                    )
                ) {
                    Text("Sign In", fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Normal)
                }

                Spacer(modifier = Modifier.width(24.dp))

                TextButton(
                    onClick = { if (!isSignUp) onToggleForm() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isSignUp) timeBasedColors.accentColor else timeBasedColors.textSecondaryColor
                    )
                ) {
                    Text("Sign Up", fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Normal)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form fields
            AnimatedVisibility(
                visible = !isSignUp,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                LoginForm(
                    loginForm = loginForm,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onLoginClick = onLoginClick,
                    onGoogleSignInClick = onGoogleSignInClick,
                    timeBasedColors = timeBasedColors
                )
            }

            AnimatedVisibility(
                visible = isSignUp,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                SignUpForm(
                    signUpForm = signUpForm,
                    onEmailChange = onSignUpEmailChange,
                    onPasswordChange = onSignUpPasswordChange,
                    onConfirmPasswordChange = onConfirmPasswordChange,
                    onDisplayNameChange = onDisplayNameChange,
                    onSignUpClick = onSignUpClick,
                    onGoogleSignInClick = onGoogleSignInClick,
                    timeBasedColors = timeBasedColors
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    loginForm: LoginFormState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    timeBasedColors: com.example.habittrackerr.TimeBasedColors
) {
    val focusManager = LocalFocusManager.current

    Column {
        // Email field
        OutlinedTextField(
            value = loginForm.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = timeBasedColors.accentColor,
                focusedLabelColor = timeBasedColors.accentColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        var showPassword by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = loginForm.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onLoginClick() }
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = timeBasedColors.accentColor,
                focusedLabelColor = timeBasedColors.accentColor
            )
        )

        if (loginForm.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = loginForm.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign In button
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loginForm.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = timeBasedColors.accentColor
            )
        ) {
            if (loginForm.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign In")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                "or",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = timeBasedColors.textSecondaryColor
            )
            Divider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign In button
        OutlinedButton(
            onClick = onGoogleSignInClick,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, timeBasedColors.accentColor.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue with Google")
        }
    }
}

@Composable
private fun SignUpForm(
    signUpForm: SignUpFormState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    timeBasedColors: com.example.habittrackerr.TimeBasedColors
) {
    val focusManager = LocalFocusManager.current

    Column {
        // Display Name field
        OutlinedTextField(
            value = signUpForm.displayName,
            onValueChange = onDisplayNameChange,
            label = { Text("Display Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = timeBasedColors.accentColor,
                focusedLabelColor = timeBasedColors.accentColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email field
        OutlinedTextField(
            value = signUpForm.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = timeBasedColors.accentColor,
                focusedLabelColor = timeBasedColors.accentColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        var showPassword by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = signUpForm.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = timeBasedColors.accentColor,
                focusedLabelColor = timeBasedColors.accentColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password field
        var showConfirmPassword by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = signUpForm.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(
                        if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSignUpClick() }
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = timeBasedColors.accentColor,
                focusedLabelColor = timeBasedColors.accentColor
            )
        )

        if (signUpForm.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = signUpForm.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Up button
        Button(
            onClick = onSignUpClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !signUpForm.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = timeBasedColors.accentColor
            )
        ) {
            if (signUpForm.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                "or",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = timeBasedColors.textSecondaryColor
            )
            Divider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign In button
        OutlinedButton(
            onClick = onGoogleSignInClick,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, timeBasedColors.accentColor.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue with Google")
        }
    }
}
