package com.example.habittrackerr.auth

/**
 * Exception thrown when OnePlus device limitations prevent Google Sign-In from working properly
 */
class OnePlusAuthException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
