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



object GenderConverter {
    const val MALE = 1
    const val FEMALE = 2

    fun toApiValue(gender: String): Int {
        return when (gender.lowercase()) {
            "male", "مرد" -> MALE
            "female", "زن" -> FEMALE
            else -> MALE
        }
    }

    fun fromApiValue(value: Int): String {
        return when (value) {
            MALE -> "مرد"
            FEMALE -> "زن"
            else -> "نامشخص"
        }
    }
}



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