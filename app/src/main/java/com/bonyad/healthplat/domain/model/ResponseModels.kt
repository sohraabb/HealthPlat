package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============ Generic API Response Wrapper ============
@Serializable
data class ApiResponse<T>(
    @SerialName("Data")
    val data: T? = null,
    @SerialName("Pagination")
    val pagination: Pagination? = null,
    @SerialName("IsSuccess")
    val isSuccess: Boolean,
    @SerialName("Message")
    val message: String? = null,
    @SerialName("Errors")
    val errors: List<String> = emptyList()
)

@Serializable
data class Pagination(
    @SerialName("CurrentPage")
    val currentPage: Int? = null,
    @SerialName("TotalPages")
    val totalPages: Int? = null,
    @SerialName("PageSize")
    val pageSize: Int? = null,
    @SerialName("TotalCount")
    val totalCount: Int? = null
)

// ============ Auth Response Models ============

// Response from RequestPhoneVerification
@Serializable
data class PhoneVerificationData(
    @SerialName("UserId")
    val userId: String?,  // null for new users
    @SerialName("Code")
    val code: String      // OTP code (returned in dev/test mode)
)

// Response from LoginByPhone and RegisterByPhone
@Serializable
data class LoginResponse(
    @SerialName("RequiresTwoFactor")
    val requiresTwoFactor: Boolean,
    @SerialName("UserId")
    val userId: String,
    @SerialName("AccessToken")
    val accessToken: String,
    @SerialName("RefreshToken")
    val refreshToken: String,
    @SerialName("ExpDate")
    val expDate: String
)

// ============ User Response Models ============

@Serializable
data class UserData(
    @SerialName("Id")
    val id: String,
    @SerialName("UserName")
    val userName: String,
    @SerialName("Email")
    val email: String? = null,
    @SerialName("EmailConfirmed")
    val emailConfirmed: Boolean,
    @SerialName("PhoneNumber")
    val phoneNumber: String,
    @SerialName("PhoneNumberConfirmed")
    val phoneNumberConfirmed: Boolean,
    @SerialName("TwoFactorEnabled")
    val twoFactorEnabled: Boolean,
    @SerialName("LockoutEnabled")
    val lockoutEnabled: Boolean,
    @SerialName("LockoutEnd")
    val lockoutEnd: String? = null,
    @SerialName("AccessFailedCount")
    val accessFailedCount: Int,
    @SerialName("EnabledEmailMarketing")
    val enabledEmailMarketing: Boolean,
    @SerialName("AcceptedTermsAndPolicy")
    val acceptedTermsAndPolicy: Boolean,
    @SerialName("UserInfo")
    val userInfo: UserInfoData?
)

@Serializable
data class UserInfoData(
    @SerialName("Name")
    val name: String,
    @SerialName("BirthDate")
    val birthDate: String,
    @SerialName("Gender")
    val gender: Int,  // 1 = Male, 2 = Female
    @SerialName("Height")
    val height: Int,
    @SerialName("Weight")
    val weight: Int,
    @SerialName("CreatedDate")
    val createdDate: String
)

// ============ Device Response Models ============

@Serializable
data class UserDeviceData(
    @SerialName("Id")
    val id: Int,
    @SerialName("UserId")
    val userId: String,
    @SerialName("DeviceMac")
    val deviceMac: String,
    @SerialName("DeviceName")
    val deviceName: String?,
    @SerialName("DeviceType")
    val deviceType: String,
    @SerialName("FirmwareVersion")
    val firmwareVersion: String?,
    @SerialName("IsActive")
    val isActive: Boolean,
    @SerialName("CreatedDate")
    val createdDate: String
)