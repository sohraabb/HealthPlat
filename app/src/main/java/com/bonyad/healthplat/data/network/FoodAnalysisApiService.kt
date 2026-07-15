package com.bonyad.healthplat.data.network

import com.bonyad.healthplat.domain.model.AiApiResponse
import com.bonyad.healthplat.domain.model.FoodAnalysisData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface FoodAnalysisApiService {

    /**
     * Analyze food from a previously uploaded image.
     * GET /analyze-food/{imageName}
     *
     * @param imageName The image file name returned from createMealByPicture
     */
    @GET("analyze-food/{imageName}")
    suspend fun analyzeFood(
        @Path("imageName") imageName: String
    ): Response<AiApiResponse<FoodAnalysisData>>
}
