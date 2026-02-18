package com.bonyad.healthplat.data.network

import com.bonyad.healthplat.domain.model.AddCaregiverByScanRequest
import com.bonyad.healthplat.domain.model.AddCaregiverRequest
import com.bonyad.healthplat.domain.model.AddUserDeviceRequest
import com.bonyad.healthplat.domain.model.ApiResponse
import com.bonyad.healthplat.domain.model.CaregiverData
import com.bonyad.healthplat.domain.model.CreateDishRequest
import com.bonyad.healthplat.domain.model.CreateMealRequest
import com.bonyad.healthplat.domain.model.DiseaseData
import com.bonyad.healthplat.domain.model.LoginByPhoneRequest
import com.bonyad.healthplat.domain.model.LoginResponse
import com.bonyad.healthplat.domain.model.MealData
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.MetricRequest
import com.bonyad.healthplat.domain.model.PhoneVerificationData
import com.bonyad.healthplat.domain.model.RefreshTokenRequest
import com.bonyad.healthplat.domain.model.RefreshTokenResponse
import com.bonyad.healthplat.domain.model.RegisterByPhoneRequest
import com.bonyad.healthplat.domain.model.RequestPhoneVerificationRequest
import com.bonyad.healthplat.domain.model.SleepMetricRequest
import com.bonyad.healthplat.domain.model.UpdateCaregiverPermissionsRequest
import com.bonyad.healthplat.domain.model.UpdateDeviceRequest
import com.bonyad.healthplat.domain.model.UpdateDishRequest
import com.bonyad.healthplat.domain.model.UpdateMealRequest
import com.bonyad.healthplat.domain.model.UpdateUserProfileRequest
import com.bonyad.healthplat.domain.model.UpdateUserRequest
import com.bonyad.healthplat.domain.model.UserData
import com.bonyad.healthplat.domain.model.UserDeviceData
import com.bonyad.healthplat.domain.model.UserOverviewData
import com.bonyad.healthplat.domain.model.UserProfile
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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
    suspend fun refreshToken(
        @Header("Authorization") expiredTokenWithBearer: String, // 👈 REQUIRED by your backend
        @Query("accessToken") accessToken: String, // 👈 REQUIRED as Query
        @Query("refreshToken") refreshToken: String // 👈 REQUIRED as Query
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

    /**
     * Get user overview (includes user info + devices)
     * GET /api/User/GetUserOverview
     */
    @GET("User/GetUserOverview")
    suspend fun getUserOverview(): Response<ApiResponse<UserOverviewData>>

    /**
     * Get all available diseases
     * GET /api/User/GetDiseases
     */
    @GET("User/GetDiseases")
    suspend fun getDiseases(): Response<ApiResponse<List<DiseaseData>>>


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
    ): Response<ApiResponse<Boolean>>


// ============ Metrics APIs ============


    @POST("Metrics/HeartRate")
    suspend fun uploadHeartRate(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

    @POST("Metrics/Steps")
    suspend fun uploadSteps(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

    @POST("Metrics/Sleep")
    suspend fun uploadSleep(@Body request: SleepMetricRequest): Response<ApiResponse<MetricData>>

    @POST("Metrics/Spo2")
    suspend fun uploadSpo2(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

    @POST("Metrics/Stress")
    suspend fun uploadStress(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

    @POST("Metrics/Hrv")
    suspend fun uploadHrv(@Body request: MetricRequest): Response<ApiResponse<MetricData>>

    /**
     * Get Heart Rate metrics for a date range
     * GET /api/Metrics/HeartRate?dateFrom=2025-12-26&dateTo=2025-12-26
     * Requires Bearer token
     */
    @GET("Metrics/HeartRate")
    suspend fun getHeartRateMetrics(
        @Query("dateFrom") dateFrom: String,  // Format: yyyy-MM-dd
        @Query("dateTo") dateTo: String       // Format: yyyy-MM-dd
    ): Response<ApiResponse<List<MetricData>>>

    /**
     * Get Steps metrics for a date range
     * GET /api/Metrics/Steps?dateFrom=2025-12-26&dateTo=2025-12-26
     */
    @GET("Metrics/Steps")
    suspend fun getStepsMetrics(
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<ApiResponse<List<MetricData>>>

    /**
     * Get Sleep metrics for a date range
     * GET /api/Metrics/Sleep?dateFrom=2025-12-26&dateTo=2025-12-26
     */
    @GET("Metrics/Sleep")
    suspend fun getSleepMetrics(
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<ApiResponse<List<MetricData>>>

    /**
     * Get SpO2 metrics for a date range
     * GET /api/Metrics/Spo2?dateFrom=2025-12-26&dateTo=2025-12-26
     */
    @GET("Metrics/Spo2")
    suspend fun getSpo2Metrics(
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<ApiResponse<List<MetricData>>>

    /**
     * Get Stress metrics for a date range
     * GET /api/Metrics/Stress?dateFrom=2025-12-26&dateTo=2025-12-26
     */
    @GET("Metrics/Stress")
    suspend fun getStressMetrics(
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<ApiResponse<List<MetricData>>>

    /**
     * Get HRV metrics for a date range
     * GET /api/Metrics/Hrv?dateFrom=2025-12-26&dateTo=2025-12-26
     */
    @GET("Metrics/Hrv")
    suspend fun getHrvMetrics(
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<ApiResponse<List<MetricData>>>


    // ============ Care/Caregiver APIs (UPDATED) ============

    /**
     * Add a caregiver by phone number
     * POST /api/Caregiver/AddCareGiverByPhoneNumber
     */
    @POST("Caregiver/AddCareGiverByPhoneNumber")
    @Headers("Content-Type: application/json; ver=1.0")
    suspend fun addCaregiverByPhoneNumber(
        @Body request: AddCaregiverRequest
    ): Response<ApiResponse<CaregiverData>>

    /**
     * Add a caregiver by scanning QR code (userId)
     * POST /api/Caregiver/AddCareGiverByScan
     */
    @POST("Caregiver/AddCareGiverByScan")
    @Headers("Content-Type: application/json; ver=1.0")
    suspend fun addCaregiverByScan(
        @Body request: AddCaregiverByScanRequest
    ): Response<ApiResponse<CaregiverData>>

    /**
     * Accept a caregiver request
     * PUT /api/Caregiver/Accept/{CareId}
     */
    @PUT("Caregiver/Accept/{careId}")
    suspend fun acceptCaregiverRequest(
        @Path("careId") careId: Int
    ): Response<ApiResponse<CaregiverData>>

    /**
     * Get my caregivers (people taking care of me)
     * GET /api/Caregiver/GetMyCaregivers
     */
    @GET("Caregiver/GetMyCaregivers")
    suspend fun getMyCaregivers(): Response<ApiResponse<List<CaregiverData>>>

    /**
     * Get patients I'm taking care of
     * GET /api/Caregiver/GetMyPatients
     */
    @GET("Caregiver/GetMyPatients")
    suspend fun getMyPatients(): Response<ApiResponse<List<CaregiverData>>>

    /**
     * Update caregiver permissions (no PhoneNumber in body)
     * PUT /api/Caregiver/Update/{id}
     */
    @PUT("Caregiver/Update/{careId}")
    @Headers("Content-Type: application/json; ver=1.0")
    suspend fun updateCaregiverPermissions(
        @Path("careId") careId: Int,
        @Body request: UpdateCaregiverPermissionsRequest
    ): Response<ApiResponse<CaregiverData>>

    /**
     * Delete a caregiver relationship
     * DELETE /api/Caregiver/Delete/{CareId}
     */
    @DELETE("Caregiver/Delete/{careId}")
    suspend fun deleteCaregiver(
        @Path("careId") careId: Int
    ): Response<ApiResponse<Boolean>>

    // ============ Caregiver Metric APIs (NEW) ============

    /**
     * Get patient's heart rate data (as caregiver)
     * GET /api/Caregiver/HeartRate?patientUserId=...&dateFrom=...&dateTo=...
     */
    @GET("Caregiver/HeartRate")
    suspend fun getCaregiverHeartRate(
        @Query("patientUserId") patientUserId: String,
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<ApiResponse<List<MetricData>>>

    /**
     * Get patient's sleep data (as caregiver)
     * GET /api/Caregiver/Sleep?patientUserId=...&dateFrom=...&dateTo=...
     */
    @GET("Caregiver/Sleep")
    suspend fun getCaregiverSleep(
        @Query("patientUserId") patientUserId: String,
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<ApiResponse<List<MetricData>>>

    /**
     * Get patient's SpO2 data (as caregiver)
     * GET /api/Caregiver/Spo2?patientUserId=...&dateFrom=...&dateTo=...
     */
    @GET("Caregiver/Spo2")
    suspend fun getCaregiverSpo2(
        @Query("patientUserId") patientUserId: String,
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<ApiResponse<List<MetricData>>>

    /**
     * Get patient's stress data (as caregiver)
     * GET /api/Caregiver/Stress?patientUserId=...&dateFrom=...&dateTo=...
     */
    @GET("Caregiver/Stress")
    suspend fun getCaregiverStress(
        @Query("patientUserId") patientUserId: String,
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<ApiResponse<List<MetricData>>>


    // ============ Calory APIs ============

    /**
     * Get all meals for the current user
     * GET /api/Food/GetAllMeals
     */
    @GET("Food/GetAllMeals")
    suspend fun getAllMeals(): Response<ApiResponse<List<MealData>>>

    /**
     * Get meals by date range
     * GET /api/Food/GetMealsByDate?dateFrom=2025-10-10&dateTo=2026-01-01
     */
    @GET("Food/GetMealsByDate")
    suspend fun getMealsByDate(
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<ApiResponse<List<MealData>>>

    /**
     * Get a specific meal by ID
     * GET /api/Food/GetMeal/{mealId}
     */
    @GET("Food/GetMeal/{mealId}")
    suspend fun getMeal(
        @Path("mealId") mealId: Int
    ): Response<ApiResponse<MealData>>

    /**
     * Get meal by image name
     * GET /api/Food/GetMealByImageName?imageName=sp
     */
    @GET("Food/GetMealByImageName")
    suspend fun getMealByImageName(
        @Query("imageName") imageName: String
    ): Response<ApiResponse<MealData>>

    // ============ POST Endpoints ============

    /**
     * Create a meal by uploading a picture (AI analysis)
     * POST /api/Food/CreateMealByPicture
     * This endpoint uploads an image and returns AI-analyzed meal data
     */
    @Multipart
    @POST("Food/CreateMealByPicture")
    suspend fun createMealByPicture(
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<MealData>>

    /**
     * Create a meal manually
     * POST /api/Food/CreateMeal
     */
    @POST("Food/CreateMeal")
    @Headers("Content-Type: application/json; ver=1.0")
    suspend fun createMeal(
        @Body request: CreateMealRequest
    ): Response<ApiResponse<MealData>>

    /**
     * Create a dish (add to existing meal)
     * POST /api/Food/CreateDish
     */
    @POST("Food/CreateDish")
    @Headers("Content-Type: application/json; ver=1.0")
    suspend fun createDish(
        @Body request: CreateDishRequest
    ): Response<ApiResponse<MealData>>

    // ============ PUT Endpoints ============

    /**
     * Update a meal
     * PUT /api/Food/UpdateMeal
     */
    @PUT("Food/UpdateMeal")
    @Headers("Content-Type: application/json; ver=1.0")
    suspend fun updateMeal(
        @Body request: UpdateMealRequest
    ): Response<ApiResponse<MealData>>

    /**
     * Update a dish
     * PUT /api/Food/UpdateDish
     */
    @PUT("Food/UpdateDish")
    @Headers("Content-Type: application/json; ver=1.0")
    suspend fun updateDish(
        @Body request: UpdateDishRequest
    ): Response<ApiResponse<MealData>>

    // ============ DELETE Endpoints ============

    /**
     * Delete a dish from a meal
     * DELETE /api/Food/Meal/{mealId}/DeleteDish/{dishId}
     */
    @DELETE("Food/Meal/{mealId}/DeleteDish/{dishId}")
    suspend fun deleteDish(
        @Path("mealId") mealId: Int,
        @Path("dishId") dishId: Int
    ): Response<ApiResponse<Boolean>>

    /**
     * Delete an entire meal
     * DELETE /api/Food/DeleteMeal/{mealId}
     */
    @DELETE("Food/DeleteMeal/{mealId}")
    suspend fun deleteMeal(
        @Path("mealId") mealId: Int
    ): Response<ApiResponse<Boolean>>
}