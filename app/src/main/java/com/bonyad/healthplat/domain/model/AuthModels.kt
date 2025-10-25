package com.bonyad.healthplat.domain.model

sealed class AuthState  {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
    object PhoneSubmitted : AuthState()
    object OtpVerified : AuthState()
}

// Mock API responses (replace with real API later)
data class SendOtpRequest(val phoneNumber: String)
data class SendOtpResponse(
    val success: Boolean,
    val message: String,
    val expiresIn: Int? = null
)

data class VerifyOtpRequest(val phoneNumber: String, val otp: String)
data class VerifyOtpResponse(
    val success: Boolean,
    val message: String,
    val userId: Long? = null,
    val token: String? = null,
    val refreshToken: String? = null
)

data class RefreshTokenRequest(val refreshToken: String)
data class RefreshTokenResponse(
    val success: Boolean,
    val token: String? = null,
    val refreshToken: String? = null
)