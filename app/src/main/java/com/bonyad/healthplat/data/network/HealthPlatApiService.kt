package com.bonyad.healthplat.data.network

import com.bonyad.healthplat.domain.model.ApiResponse
import com.bonyad.healthplat.domain.model.LoginByPhoneRequest
import com.bonyad.healthplat.domain.model.LoginResponse
import com.bonyad.healthplat.domain.model.RequestPhoneVerificationRequest
import com.bonyad.healthplat.domain.model.UpdateUserProfileRequest
import com.bonyad.healthplat.domain.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface HealthPlatApiService {

    // ============ Authentication APIs ============

    /**
     * Request phone verification (send OTP)
     * POST /api/Auth/RequestPhoneVerification
     */
    @POST("Auth/RequestPhoneVerification")
    @Headers("Content-Type: application/json; ver=1.0")
    suspend fun requestPhoneVerification(
        @Body request: RequestPhoneVerificationRequest
    ): Response<ApiResponse<Boolean>>

    /**
     * Login by phone (verify OTP)
     * POST /api/Auth/LoginByPhone
     */
    @POST("Auth/LoginByPhone")
    @Headers("Content-Type: application/json; ver=1.0")
    suspend fun loginByPhone(
        @Body request: LoginByPhoneRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * Logout
     * POST /api/Auth/logout
     * Requires Bearer token
     */
    @POST("Auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    // ============ User APIs (TODO: Implement when needed) ============

    /**
     * Get user profile
     * GET /api/User
     */
    @GET("User")
    suspend fun getUserProfile(): Response<ApiResponse<UserProfile>>

    /**
     * Update user profile
     * PUT /api/User/{id}
     */
    @PUT("User/{id}")
    suspend fun updateUserProfile(
        @Path("id") userId: String,
        @Body request: UpdateUserProfileRequest
    ): Response<ApiResponse<UserProfile>>
}