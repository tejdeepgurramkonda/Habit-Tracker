package com.example.habittrackerr.ui.stats

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.example.habittrackerr.LocalTimeBasedColors

@Composable
fun GoogleFitAuthCard(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val context = LocalContext.current
    val timeBasedColors = LocalTimeBasedColors.current

    // Google Fit fitness options
    val fitnessOptions = remember {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .build()
    }

    // Check current auth status
    val currentAccount = GoogleSignIn.getLastSignedInAccount(context)
    val hasPermissions = currentAccount?.let {
        GoogleSignIn.hasPermissions(it, fitnessOptions)
    } ?: false

    // Activity launcher for Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                onAuthSuccess()
            } else {
                onAuthError("Failed to grant Google Fit permissions")
            }
        } else {
            onAuthError("Google Sign-In was cancelled")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeBasedColors.cardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = "Google Fit",
                tint = timeBasedColors.accentColor,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (hasPermissions) {
                Text(
                    text = "Google Fit Connected",
                    style = MaterialTheme.typography.titleMedium,
                    color = timeBasedColors.textPrimaryColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your fitness data is syncing from Google Fit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = timeBasedColors.textSecondaryColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        // Sign out from Google Fit
                        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .signOut()
                    }
                ) {
                    Text("Disconnect")
                }
            } else {
                Text(
                    text = "Connect Google Fit",
                    style = MaterialTheme.typography.titleMedium,
                    color = timeBasedColors.textPrimaryColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Connect your Google Fit account to view your fitness data in the statistics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = timeBasedColors.textSecondaryColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        try {
                            if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(context), fitnessOptions)) {
                                val signInIntent = GoogleSignIn.getClient(context,
                                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .addExtension(fitnessOptions)
                                        .build()
                                ).signInIntent

                                googleSignInLauncher.launch(signInIntent)
                            }
                        } catch (e: Exception) {
                            onAuthError("Failed to start Google Sign-In: ${e.message}")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Google Fit")
                }
            }
        }
    }
}
