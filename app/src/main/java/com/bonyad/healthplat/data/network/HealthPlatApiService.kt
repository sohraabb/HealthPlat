package com.bonyad.healthplat.data.network

import com.bonyad.healthplat.domain.model.AddUserDeviceRequest
import com.bonyad.healthplat.domain.model.ApiResponse
import com.bonyad.healthplat.domain.model.LoginByPhoneRequest
import com.bonyad.healthplat.domain.model.LoginResponse
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.MetricRequest
import com.bonyad.healthplat.domain.model.PhoneVerificationData
import com.bonyad.healthplat.domain.model.RefreshTokenRequest
import com.bonyad.healthplat.domain.model.RefreshTokenResponse
import com.bonyad.healthplat.domain.model.RegisterByPhoneRequest
import com.bonyad.healthplat.domain.model.RequestPhoneVerificationRequest
import com.bonyad.healthplat.domain.model.UpdateDeviceRequest
import com.bonyad.healthplat.domain.model.UpdateUserProfileRequest
import com.bonyad.healthplat.domain.model.UpdateUserRequest
import com.bonyad.healthplat.domain.model.UserData
import com.bonyad.healthplat.domain.model.UserDeviceData
import com.bonyad.healthplat.domain.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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
    @Headers("Content-Type: application/json")
    suspend fun requestPhoneVerification(
        @Body request: RequestPhoneVerificationRequest
    ): Response<ApiResponse<PhoneVerificationData>>

    /**
     * Login by phone (verify OTP)
     * POST /api/Auth/LoginByPhone
     */
    @POST("Auth/LoginByPhone")
    @Headers("Content-Type: application/json")
    suspend fun loginByPhone(
        @Body request: LoginByPhoneRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * Register by phone (verify OTP)
     * POST /api/Auth/LoginByPhone
     */
    @POST("Auth/RegisterByPhone")
    @Headers("Content-Type: application/json")
    suspend fun registerByPhone(
        @Body request: RegisterByPhoneRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * Refresh access token
     * POST /api/Auth/refresh
     */
    @POST("Auth/refresh")
    @Headers("Content-Type: application/json")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<ApiResponse<RefreshTokenResponse>>

    /**
     * Logout
     * POST /api/Auth/logout
     * Requires Bearer token
     */
    @POST("Auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    // ============ User APIs (TODO: Implement when needed) ============

    /**
     * Add user profile
     * Add /api/User/Add
     */
    @PUT("User/Update")
    @Headers("Content-Type: application/json")
    suspend fun updateUserInfo(
        @Body request: UpdateUserRequest
    ): Response<ApiResponse<UserData>>

    /**
     * Get user profile
     * GET /api/User
     */
    @GET("User")
    suspend fun getUserProfile(): Response<ApiResponse<UserData>>

    /**
     * Update user profile
     * PUT /api/User/{id}
     */
    @PUT("User/{id}")
    suspend fun updateUserProfile(
        @Path("id") userId: String,
        @Body request: UpdateUserProfileRequest
    ): Response<ApiResponse<UserProfile>>


    // ============ Device Management APIs ============

    @POST("UserDevice/Add")
    @Headers("Content-Type: application/json")
    suspend fun addUserDevice(
        @Body request: AddUserDeviceRequest
    ): Response<ApiResponse<UserDeviceData>>

    @GET("UserDevice/GetByUserId/{userId}")
    suspend fun getUserDevices(
        @Path("userId") userId: String
    ): Response<ApiResponse<List<UserDeviceData>>>

    @PUT("UserDevice/Update/{id}")
    @Headers("Content-Type: application/json; ver=1.0")
    suspend fun updateDevice(
        @Path("id") deviceId: Int,
        @Body request: UpdateDeviceRequest
    ): Response<ApiResponse<UserDeviceData>>

    @PUT("UserDevice/Deactivate/{id}")
    suspend fun deactivateDevice(
        @Path("id") deviceId: Int
    ): Response<ApiResponse<Unit>>

    @DELETE("UserDevice/Delete/{id}")
    suspend fun deleteDevice(
        @Path("id") deviceId: Int
    ): Response<ApiResponse<Unit>>


// ============ Metrics APIs ============


    @POST("Metrics/HeartRate")
    suspend fun uploadHeartRate(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

    @POST("Metrics/Steps")
    suspend fun uploadSteps(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

    @POST("Metrics/Sleep")
    suspend fun uploadSleep(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

    @POST("Metrics/Spo2")
    suspend fun uploadSpo2(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

    @POST("Metrics/Stress")
    suspend fun uploadStress(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

    @POST("Metrics/Hrv")
    suspend fun uploadHrv(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

}