package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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