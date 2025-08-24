package com.example.habittrackerr.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmailVerificationScreen(
    authViewModel: AuthViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Verify Your Email",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We've sent a verification email to your address. Please check your inbox and click the verification link.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Simulate resend email verification
                // In real implementation: authViewModel.resendEmailVerification()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Resend Verification Email")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Simulate check email verification
                // In real implementation: authViewModel.checkEmailVerification()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I've Verified My Email")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                authViewModel.signOut()
            }
        ) {
            Text("Back to Login")
        }
    }
}
