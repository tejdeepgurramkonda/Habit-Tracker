package com.example.habittrackerr.auth

import android.content.Context
import com.example.habittrackerr.auth.biometric.BiometricAuthManager
import com.example.habittrackerr.auth.oneplus.OnePlusCompatibilityManager
import com.example.habittrackerr.auth.security.SecurityUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideOnePlusCompatibilityManager(
        @ApplicationContext context: Context
    ): OnePlusCompatibilityManager = OnePlusCompatibilityManager(context)

    @Provides
    @Singleton
    fun provideGoogleSignInClient(
        @ApplicationContext context: Context,
        onePlusCompatibilityManager: OnePlusCompatibilityManager
    ): GoogleSignInClientWrapper = GoogleSignInClientWrapper(context, onePlusCompatibilityManager)

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        securityUtils: SecurityUtils,
        biometricManager: BiometricAuthManager
    ): AuthRepository = AuthRepository(context, securityUtils, biometricManager)
}
