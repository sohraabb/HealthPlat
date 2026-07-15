package com.bonyad.healthplat.data.repository

import android.content.Context
import android.net.Uri
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.FoodAnalysisApiService
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.CreateDishRequest
import com.bonyad.healthplat.domain.model.CreateMealRequest
import com.bonyad.healthplat.domain.model.DailyCalorieSummary
import com.bonyad.healthplat.domain.model.DailySummaryData
import com.bonyad.healthplat.domain.model.DishData
import com.bonyad.healthplat.domain.model.DishRequest
import com.bonyad.healthplat.domain.model.FoodAnalysisData
import com.bonyad.healthplat.domain.model.FoodFactData
import com.bonyad.healthplat.domain.model.FoodItemUi
import com.bonyad.healthplat.domain.model.FoodTotalFacts
import com.bonyad.healthplat.domain.model.MealData
import com.bonyad.healthplat.domain.model.MealSummaryUi
import com.bonyad.healthplat.domain.model.MealType
import com.bonyad.healthplat.domain.model.UpdateDishRequest
import com.bonyad.healthplat.domain.model.UpdateMealRequest
import com.bonyad.healthplat.domain.model.groupByMealType
import com.bonyad.healthplat.domain.model.toFoodItems
import com.bonyad.healthplat.domain.model.totalConsumedCalories
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val foodApiService: HealthPlatApiService,
    private val foodAnalysisApiService: FoodAnalysisApiService,
    private val userPreferences: UserPreferencesDataStore,
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MAX_UPLOAD_IMAGES = 3
    }

    // ============ Get Operations ============

    /**
     * Get all meals for the current user
     */
    suspend fun getAllMeals(): AuthResult<List<MealData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = foodApiService.getAllMeals()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val meals = response.body()?.data ?: emptyList()
                    Timber.i("✅ Got ${meals.size} meals")
                    AuthResult.Success(meals)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: response.body()?.message
                        ?: "خطا در دریافت وعده‌ها"
                    Timber.w("❌ Get meals failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Get meals exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get meals for a specific date
     */
    suspend fun getMealsForDate(date: Date): AuthResult<List<MealData>> {
        return withContext(Dispatchers.IO) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateString = dateFormat.format(date)

                // Get meals for that single day (from start of day to end of day)
                val response = foodApiService.getMealsByDate(
                    dateFrom = dateString,
                    dateTo = dateString
                )

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val meals = response.body()?.data ?: emptyList()
                    Timber.i("✅ Got ${meals.size} meals for $dateString")
                    AuthResult.Success(meals)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در دریافت وعده‌ها"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Get meals for date exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get meals for a date range
     */
    suspend fun getMealsByDateRange(
        dateFrom: Date,
        dateTo: Date
    ): AuthResult<List<MealData>> {
        return withContext(Dispatchers.IO) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                val response = foodApiService.getMealsByDate(
                    dateFrom = dateFormat.format(dateFrom),
                    dateTo = dateFormat.format(dateTo)
                )

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val meals = response.body()?.data ?: emptyList()
                    Timber.i("✅ Got ${meals.size} meals for date range")
                    AuthResult.Success(meals)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در دریافت وعده‌ها"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Get meals by date range exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get a specific meal by ID
     */
    suspend fun getMeal(mealId: Int): AuthResult<MealData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = foodApiService.getMeal(mealId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val meal = response.body()?.data
                    if (meal != null) {
                        Timber.i("✅ Got meal: ${meal.mealName}")
                        AuthResult.Success(meal)
                    } else {
                        AuthResult.Error("وعده یافت نشد")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در دریافت وعده"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Get meal exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get daily summary from server (consumed + burned calories, meals)
     */
    suspend fun getDailySummary(date: Date): AuthResult<DailySummaryData> {
        return withContext(Dispatchers.IO) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateString = dateFormat.format(date)

                val response = foodApiService.getDailySummary(dateString)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val summary = response.body()?.data
                    if (summary != null) {
                        Timber.i("✅ Got daily summary for $dateString: consumed=${summary.totalGainedCal}, burned=${summary.totalActivityCal}")
                        AuthResult.Success(summary)
                    } else {
                        AuthResult.Error("خلاصه روزانه یافت نشد")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در دریافت خلاصه روزانه"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Get daily summary exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get daily calorie summary for a date.
     * Uses the GetDailySummary endpoint which returns consumed + burned calories from server.
     */
    suspend fun getDailyCalorieSummary(date: Date): AuthResult<DailyCalorieSummary> {
        return withContext(Dispatchers.IO) {
            when (val result = getDailySummary(date)) {
                is AuthResult.Success -> {
                    val data = result.data
                    val meals = data.meals
                    val groupedByType = meals.groupByMealType()

                    val mealSummaries = MealType.values().map { mealType ->
                        val items = groupedByType[mealType] ?: emptyList()
                        MealSummaryUi(
                            mealType = mealType,
                            items = items,
                            totalCaloriesMin = items.sumOf { it.caloriesMin },
                            totalCaloriesMax = items.sumOf { it.caloriesMax }
                        )
                    }

                    val summary = DailyCalorieSummary(
                        date = data.date,
                        consumedCalories = data.totalGainedCal.toInt(),
                        burnedCalories = data.totalActivityCal.toInt(),
                        goalCalories = 2000, // TODO: Get from user preferences
                        meals = mealSummaries
                    )

                    AuthResult.Success(summary)
                }
                is AuthResult.Error -> {
                    Timber.w("⚠️ getDailySummary failed: ${result.message}")
                    AuthResult.Error(result.message)
                }
            }
        }
    }

    // ============ Food Analysis (AI on port 8003) ============

    /**
     * Analyze a food image via the AI service.
     * @param imageName The image file name returned from createMealByPicture
     */
    suspend fun analyzeFood(imageName: String): AuthResult<FoodAnalysisData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = foodAnalysisApiService.analyzeFood(imageName)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        Timber.i("✅ Food analysis complete: ${data.itemFacts.size} items, quality=${data.mealQuality}")
                        AuthResult.Success(data)
                    } else {
                        AuthResult.Error("نتیجه تحلیل خالی بود")
                    }
                } else {
                    val err = response.body()?.errors
                    val errorMessage = mapFoodErrorCode(err?.code)
                        ?: err?.message
                        ?: "خطا در تحلیل تصویر"
                    Timber.w("❌ Food analysis failed: code=${err?.code}, message=$errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Food analysis exception")
                AuthResult.Error("خطا در ارتباط با سرور هوش مصنوعی")
            }
        }
    }

    /**
     * Map the AI food-analysis error codes to user-facing Persian messages.
     * Returns null for unknown codes so the caller can fall back to the raw message.
     */
    private fun mapFoodErrorCode(code: String?): String? = when (code) {
        "NO_FOOD" -> "غذایی در عکس نیست"
        "UNCLEAR_IMAGE" -> "عکس واضح نیست یا تار است"
        "NO_MATCH" -> "غذای موجود در عکس شناسایی نشد"
        "TOO_FAR" -> "غذا از دوربین دور است، نزدیک‌تر عکس بگیرید"
        "BLOCKED" -> "غذا با آیتم دیگری پوشانده شده است"
        "TOO_MANY_ITEMS" -> "بیش از ۴ آیتم غذایی در عکس است"
        else -> null
    }

    // ============ Food Facts Search ============

    /**
     * Search food facts database
     */
    suspend fun searchFoodFacts(
        query: String,
        page: Int = 1,
        pageSize: Int = 20
    ): AuthResult<List<FoodFactData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = foodApiService.searchFoodFacts(query, page, pageSize)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val facts = response.body()?.data ?: emptyList()
                    Timber.i("✅ Found ${facts.size} food facts for '$query'")
                    AuthResult.Success(facts)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در جستجوی مواد غذایی"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Search food facts exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    // ============ Create Operations ============

    /**
     * Create a meal by uploading pictures (step 1 of scan flow).
     * Only uploads the image and creates a skeleton meal — AI analysis is separate.
     */
    suspend fun createMealByPicture(imageUris: List<Uri>): AuthResult<MealData> {
        return withContext(Dispatchers.IO) {
            try {
                if (imageUris.isEmpty()) {
                    return@withContext AuthResult.Error("لطفاً حداقل یک تصویر انتخاب کنید")
                }
                if (imageUris.size > MAX_UPLOAD_IMAGES) {
                    return@withContext AuthResult.Error("حداکثر $MAX_UPLOAD_IMAGES تصویر مجاز است")
                }

                // Convert all Uris to Files
                val files = imageUris.mapNotNull { uriToFile(it) }
                if (files.isEmpty()) {
                    return@withContext AuthResult.Error("خطا در پردازش تصویر")
                }

                val multipartParts = files.map { file ->
                    val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("files", file.name, requestBody)
                }

                val response = foodApiService.createMealByPicture(multipartParts)

                // Clean up temp files
                files.forEach { it.delete() }

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val meal = response.body()?.data
                    if (meal != null) {
                        Timber.i("✅ Meal created from picture: id=${meal.id}, images=${meal.mealImages.size}, imageUrls=${meal.mealImages.map { it.imageUrl }}")
                        AuthResult.Success(meal)
                    } else {
                        AuthResult.Error("خطا در آپلود تصویر")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در آپلود تصویر"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Create meal by picture exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Create a meal manually
     */
    suspend fun createMeal(
        mealType: MealType,
        dishes: List<DishRequest>
    ): AuthResult<MealData> {
        return withContext(Dispatchers.IO) {
            try {
                val totalCaloriesMin = dishes.sumOf { it.caloriesMinKcal }
                val totalCaloriesMax = dishes.sumOf { it.caloriesMaxKcal }
                val totalCaloriesAvg = (totalCaloriesMin + totalCaloriesMax) / 2

                val request = CreateMealRequest(
                    mealName = mealType.apiName,
                    containsFood = true,
                    multipleDishes = dishes.size > 1,
                    dishCountEstimate = dishes.size,
                    totalCaloriesMinKcal = totalCaloriesMin,
                    totalCaloriesMaxKcal = totalCaloriesMax,
                    totalCaloriesAvgKcal = totalCaloriesAvg,
                    dishes = dishes
                )

                val response = foodApiService.createMeal(request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val meal = response.body()?.data
                    if (meal != null) {
                        Timber.i("✅ Meal created: ${meal.mealName}")
                        AuthResult.Success(meal)
                    } else {
                        AuthResult.Error("خطا در ایجاد وعده")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در ایجاد وعده"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Create meal exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Add a single food item to a meal type
     * This will either add to an existing meal or create a new one
     */
    suspend fun addFoodItem(
        mealType: MealType,
        dishName: String,
        caloriesMin: Int,
        caloriesMax: Int,
        amount: Int = 1,
        unit: String = "عدد"
    ): AuthResult<MealData> {
        return withContext(Dispatchers.IO) {
            // First, check if there's an existing meal for this type today
            val today = Date()
            val existingMeals = when (val result = getMealsForDate(today)) {
                is AuthResult.Success -> result.data
                is AuthResult.Error -> emptyList()
            }

            val existingMealForType = existingMeals.find {
                it.mealName.equals(mealType.apiName, ignoreCase = true) == true
            }

            if (existingMealForType != null) {
                // Add dish to existing meal
                addDishToMeal(
                    mealId = existingMealForType.id,
                    dishName = dishName,
                    caloriesMin = caloriesMin,
                    caloriesMax = caloriesMax,
                    amount = amount,
                    unit = unit
                )
            } else {
                // Create new meal with this dish
                val dishRequest = DishRequest(
                    dishName = dishName,
                    caloriesMinKcal = caloriesMin,
                    caloriesMaxKcal = caloriesMax,
                    amount = amount,
                    unit = unit
                )
                createMeal(mealType, listOf(dishRequest))
            }
        }
    }

    /**
     * Add a dish to an existing meal.
     * createDish now returns DishData instead of MealData, so we re-fetch the meal.
     */
    suspend fun addDishToMeal(
        mealId: Int,
        dishName: String,
        caloriesMin: Int,
        caloriesMax: Int,
        amount: Int = 1,
        unit: String = "عدد"
    ): AuthResult<MealData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = CreateDishRequest(
                    mealId = mealId,
                    dishName = dishName,
                    caloriesMinKcal = caloriesMin,
                    caloriesMaxKcal = caloriesMax,
                    amount = amount,
                    unit = unit
                )

                val response = foodApiService.createDish(request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val dish = response.body()?.data
                    if (dish != null) {
                        Timber.i("✅ Dish added: $dishName (dishId=${dish.dishId})")
                        // Re-fetch the full meal since createDish now returns DishData
                        getMeal(mealId)
                    } else {
                        AuthResult.Error("خطا در افزودن غذا")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در افزودن غذا"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Add dish exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Submit scan result: creates dishes for the meal and updates meal summary fields.
     * Called after AI analysis is reviewed/edited by the user.
     *
     * @param mealId The meal ID from createMealByPicture
     * @param mealImageId The image ID from mealImages[0]
     * @param dishes The (possibly edited) list of scanned dishes
     * @param mealQuality The AI-determined meal quality string
     * @param totalFacts The total nutritional facts from AI analysis
     * @param mealType The user-selected meal type
     */
    suspend fun submitScanResult(
        mealId: Int,
        mealImageId: Int,
        dishes: List<com.bonyad.healthplat.domain.model.ScannedDish>,
        mealQuality: String,
        totalFacts: FoodTotalFacts,
        mealType: MealType
    ): AuthResult<MealData> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("📤 submitScanResult: mealId=$mealId, mealImageId=$mealImageId, dishes=${dishes.size}, mealType=${mealType.apiName}")

                // 1. Create each dish
                for (dish in dishes) {
                    val calAvg = ((dish.caloriesMin + dish.caloriesMax) / 2).coerceAtLeast(dish.caloriesMin)
                    val request = CreateDishRequest(
                        mealId = mealId,
                        dishName = dish.name,
                        mealImageId = mealImageId,
                        amount = dish.amount,
                        unit = dish.unit,
                        caloriesMinKcal = dish.caloriesMin,
                        caloriesMaxKcal = dish.caloriesMax,
                        proteinLevel = dish.protein ?: 0.0,
                        fiberLevel = dish.fiber ?: 0.0,
                        fatLevel = dish.fat ?: 0.0,
                        carbLevel = dish.carb ?: 0.0,
                        sugarLevel = dish.sugar ?: 0.0
                    )

                    val dishResponse = foodApiService.createDish(request)
                    Timber.d("📥 CreateDish '${dish.name}' response: code=${dishResponse.code()}, isSuccess=${dishResponse.body()?.isSuccess}, errors=${dishResponse.body()?.errors}")
                    if (!dishResponse.isSuccessful || dishResponse.body()?.isSuccess != true) {
                        val err = dishResponse.body()?.errors?.firstOrNull() ?: dishResponse.errorBody()?.string() ?: "خطا در ایجاد غذای ${dish.name}"
                        Timber.w("❌ Failed to create dish '${dish.name}': $err")
                        // Continue with remaining dishes
                    } else {
                        Timber.d("✅ Created dish: ${dish.name}")
                    }
                }

                // 2. Update meal with summary fields
                val totalCalMin = dishes.sumOf { it.caloriesMin }
                val totalCalMax = dishes.sumOf { it.caloriesMax }
                val totalCalAvg = (totalCalMin + totalCalMax) / 2

                val updateRequest = UpdateMealRequest(
                    id = mealId,
                    mealName = mealType.apiName,
                    totalCaloriesMinKcal = totalCalMin,
                    totalCaloriesMaxKcal = totalCalMax,
                    totalCaloriesAvgKcal = totalCalAvg,
                    mealQuality = mealQuality,
                    containsFood = true,
                    multipleDishes = dishes.size > 1,
                    dishCountEstimate = dishes.size,
                    totalProteinLevel = totalFacts.protein,
                    totalFiberLevel = totalFacts.fiber,
                    totalFatLevel = totalFacts.fat,
                    totalCarbLevel = totalFacts.carb,
                    totalSugarLevel = totalFacts.sugar
                )

                Timber.d("📤 UpdateMeal request: id=$mealId, mealName=${mealType.apiName}, totalCalAvg=$totalCalAvg")
                val updateResponse = foodApiService.updateMeal(updateRequest)
                Timber.d("📥 UpdateMeal response: code=${updateResponse.code()}, isSuccess=${updateResponse.body()?.isSuccess}, errors=${updateResponse.body()?.errors}, errorBody=${updateResponse.errorBody()?.string()}")

                if (updateResponse.isSuccessful && updateResponse.body()?.isSuccess == true) {
                    val meal = updateResponse.body()?.data
                    if (meal != null) {
                        Timber.i("✅ Scan result submitted: mealId=$mealId, ${dishes.size} dishes")
                        AuthResult.Success(meal)
                    } else {
                        // Meal updated but response data is null — re-fetch
                        getMeal(mealId)
                    }
                } else {
                    val errorMessage = updateResponse.body()?.errors?.firstOrNull()
                        ?: "خطا در به‌روزرسانی وعده"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Submit scan result exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    // ============ Update Operations ============

    /**
     * Update a meal
     */
    suspend fun updateMeal(
        mealId: Int,
        mealName: String? = null,
        totalCaloriesMin: Int? = null,
        totalCaloriesMax: Int? = null
    ): AuthResult<MealData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UpdateMealRequest(
                    id = mealId,
                    mealName = mealName,
                    totalCaloriesMinKcal = totalCaloriesMin,
                    totalCaloriesMaxKcal = totalCaloriesMax,
                    totalCaloriesAvgKcal = if (totalCaloriesMin != null && totalCaloriesMax != null) {
                        (totalCaloriesMin + totalCaloriesMax) / 2
                    } else null
                )

                val response = foodApiService.updateMeal(request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val meal = response.body()?.data
                    if (meal != null) {
                        Timber.i("✅ Meal updated: ${meal.mealName}")
                        AuthResult.Success(meal)
                    } else {
                        AuthResult.Error("خطا در به‌روزرسانی وعده")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در به‌روزرسانی وعده"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Update meal exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Update a dish
     */
    suspend fun updateDish(
        dishId: Int,
        dishName: String? = null,
        caloriesMin: Int? = null,
        caloriesMax: Int? = null,
        amount: Int? = null,
        unit: String? = null
    ): AuthResult<MealData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UpdateDishRequest(
                    id = dishId,
                    dishName = dishName,
                    caloriesMinKcal = caloriesMin,
                    caloriesMaxKcal = caloriesMax,
                    amount = amount,
                    unit = unit
                )

                val response = foodApiService.updateDish(request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val meal = response.body()?.data
                    if (meal != null) {
                        Timber.i("✅ Dish updated")
                        AuthResult.Success(meal)
                    } else {
                        AuthResult.Error("خطا در به‌روزرسانی غذا")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در به‌روزرسانی غذا"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Update dish exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    // ============ Delete Operations ============

    /**
     * Delete a dish from a meal
     */
    suspend fun deleteDish(mealId: Int, dishId: Int): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = foodApiService.deleteDish(mealId, dishId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Timber.i("✅ Dish deleted: $dishId")
                    AuthResult.Success(Unit)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در حذف غذا"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Delete dish exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Delete an entire meal
     */
    suspend fun deleteMeal(mealId: Int): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = foodApiService.deleteMeal(mealId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Timber.i("✅ Meal deleted: $mealId")
                    AuthResult.Success(Unit)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در حذف وعده"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Delete meal exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    // ============ Helper Functions ============

    /**
     * Convert Uri to File for multipart upload
     */
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "food_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            file
        } catch (e: Exception) {
            Timber.e(e, "Failed to convert uri to file")
            null
        }
    }
}
