package com.bonyad.healthplat.domain.model

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