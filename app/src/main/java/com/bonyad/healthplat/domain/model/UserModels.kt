package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UpdateUserRequest(
    @SerialName("UserId")
    val userId: String,
    @SerialName("Name")
    val name: String,
    @SerialName("BirthDate")
    val birthDate: String,  // ISO format: "2025-11-12T09:07:22.862Z"
    @SerialName("Gender")
    val gender: Int,  // 1 = Male, 2 = Female
    @SerialName("Height")
    val height: Int,
    @SerialName("Weight")
    val weight: Int,
    @SerialName("AcceptedTermsAndPolicy")
    val acceptedTermsAndPolicy: Boolean? = null,
    @SerialName("EnabledEmailMarketing")
    val enabledEmailMarketing: Boolean? = null
)

@Serializable
data class UserOverviewData(
    @SerialName("Id")
    val id: String,
    @SerialName("UserName")
    val userName: String,
    @SerialName("Email")
    val email: String?,
    @SerialName("EmailConfirmed")
    val emailConfirmed: Boolean,
    @SerialName("PhoneNumber")
    val phoneNumber: String,
    @SerialName("PhoneNumberConfirmed")
    val phoneNumberConfirmed: Boolean,
    @SerialName("TwoFactorEnabled")
    val twoFactorEnabled: Boolean,
    @SerialName("EnabledEmailMarketing")
    val enabledEmailMarketing: Boolean,
    @SerialName("AcceptedTermsAndPolicy")
    val acceptedTermsAndPolicy: Boolean,
    @SerialName("Name")
    val name: String?,
    @SerialName("BirthDate")
    val birthDate: String?,
    @SerialName("Gender")
    val gender: Int?,
    @SerialName("Height")
    val height: Int?,
    @SerialName("Weight")
    val weight: Int?,
    @SerialName("CreatedDate")
    val createdDate: String?,
    @SerialName("UserDevices")
    val userDevices: List<UserDeviceOverview>?
)

@Serializable
data class UserDeviceOverview(
    @SerialName("Id")
    val id: Int,
    @SerialName("DeviceMac")
    val deviceMac: String,
    @SerialName("DeviceName")
    val deviceName: String? = null,
    @SerialName("DeviceType")
    val deviceType: String,
    @SerialName("FirmwareVersion")
    val firmwareVersion: String? = null,
    @SerialName("IsActive")
    val isActive: Boolean
)



// Legacy Mode Models
data class UserProfileResponse(
    val success: Boolean,
    val data: UserProfile? = null
)

data class UserProfile(
    val userId: Long,
    val phoneNumber: String,
    val name: String? = null,
    val birthDate: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
    val gender: String? = null
)

data class UpdateUserProfileRequest(
    val name: String? = null,
    val birthDate: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
    val gender: String? = null
)

data class PersonalInfoRequest(
    val name: String,
    val birthDate: String,
    val height: Int,
    val weight: Int
)

data class PersonalInfoResponse(
    val success: Boolean,
    val message: String
)