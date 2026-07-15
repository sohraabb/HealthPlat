package com.bonyad.healthplat.data.network

import com.bonyad.healthplat.domain.model.AiApiResponse
import com.bonyad.healthplat.domain.model.ArrhythmiaPredictionData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ArrhythmiaApiService {

    @GET("predict-arrhythmia/{user_id}")
    suspend fun predictArrhythmia(
        @Path("user_id") userId: String,
        @Query("datefrom") dateFrom: String,
        @Query("dateto") dateTo: String
    ): Response<AiApiResponse<List<ArrhythmiaPredictionData>>>
}
