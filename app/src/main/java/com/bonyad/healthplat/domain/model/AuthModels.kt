package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============ Auth State ============

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
    object PhoneSubmitted : AuthState()
    object OtpVerified : AuthState()
}

// ============ API Request Models ============

@Serializable
data class RequestPhoneVerificationRequest(
    @SerialName("PhoneNumber")
    val phoneNumber: String,
    @SerialName("CodeExpirationMinutes")
    val codeExpirationMinutes: Int = 2
)

@Serializable
data class LoginByPhoneRequest(
    @SerialName("PhoneNumber")
    val phoneNumber: String,
    @SerialName("VerificationCode")
    val verificationCode: String
)

@Serializable
data class RegisterByPhoneRequest(
    @SerialName("PhoneNumber")
    val phoneNumber: String,
    @SerialName("VerificationCode")
    val verificationCode: String
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("AccessToken")
    val accessToken: String,
    @SerialName("RefreshToken")
    val refreshToken: String
)


// ============ Legacy Models (Keep for compatibility) ============

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
    val refreshToken: String? = null,
    val isNewUser: Boolean = true
)