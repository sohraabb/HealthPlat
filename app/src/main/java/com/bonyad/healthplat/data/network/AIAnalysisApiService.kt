package com.bonyad.healthplat.data.network

import com.bonyad.healthplat.domain.model.AiApiResponse
import com.bonyad.healthplat.domain.model.HealthReportResponse
import com.bonyad.healthplat.domain.model.ReadinessDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AIAnalysisApiService {

    /**
     * Generates a detailed health report.
     * URL: /generate-healthreport/{user_id}/{record_date}
     *
     * @param userId The User's GUID.
     * @param recordDate Full timestamp string.
     * Example: "2025-12-06 00:00:00.0000000"
     */
    @GET("generate-healthreport/{user_id}/{record_date}")
    suspend fun getHealthReport(
        @Path("user_id") userId: String,
        @Path("record_date") recordDate: String
    ): Response<AiApiResponse<HealthReportResponse>>

    /**
     * Gets the simple readiness score.
     * URL: /readiness-score/{user_id}/{record_date}
     *
     * @param userId The User's GUID.
     * @param recordDate Simple date string.
     * Example: "2025-12-06"
     */
    @GET("readiness-score/{user_id}/{record_date}")
    suspend fun getReadinessScore(
        @Path("user_id") userId: String,
        @Path("record_date") recordDate: String
    ): Response<AiApiResponse<ReadinessDto>>

}