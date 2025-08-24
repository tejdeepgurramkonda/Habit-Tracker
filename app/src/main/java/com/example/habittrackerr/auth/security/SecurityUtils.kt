package com.example.habittrackerr.auth.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.habittrackerr.auth.*
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val phoneNumberUtil = PhoneNumberUtil.getInstance()
    private val emailPattern = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    )

    /**
     * Simple email validation
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && emailPattern.matcher(email).matches()
    }

    /**
     * Simple password validation
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Hash password with salt
     */
    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val passwordBytes = (password + salt).toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(passwordBytes)
        return "${bytesToHex(hashedBytes)}:$salt"
    }

    /**
     * Generate random salt
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return bytesToHex(salt)
    }

    /**
     * Convert bytes to hex string
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Get device information for security tracking
     */
    fun getDeviceInfo(): DeviceInfo {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        return DeviceInfo(
            id = deviceId ?: "unknown",
            name = "${Build.MANUFACTURER} ${Build.MODEL}",
            type = "Android",
            model = Build.MODEL,
            osVersion = Build.VERSION.RELEASE,
            appVersion = getAppVersion(),
            isCurrentDevice = true
        )
    }

    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}

enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG,
    VERY_STRONG
}
