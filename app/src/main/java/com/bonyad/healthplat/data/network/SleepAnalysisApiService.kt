package com.bonyad.healthplat.data.network

import com.bonyad.healthplat.domain.model.AiApiResponse
import com.bonyad.healthplat.domain.model.SleepAnalysisData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface SleepAnalysisApiService {

    @GET("sleep-analysis/{user_id}/{date}")
    suspend fun getSleepAnalysis(
        @Path("user_id") userId: String,
        @Path("date") date: String
    ): Response<AiApiResponse<SleepAnalysisData>>
}
