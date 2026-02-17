package com.bonyad.healthplat.data.repository

import android.content.Context
import android.net.Uri
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.CreateDishRequest
import com.bonyad.healthplat.domain.model.CreateMealRequest
import com.bonyad.healthplat.domain.model.DailyCalorieSummary
import com.bonyad.healthplat.domain.model.DishRequest
import com.bonyad.healthplat.domain.model.FoodItemUi
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val foodApiService: HealthPlatApiService,
    private val userPreferences: UserPreferencesDataStore,
    @ApplicationContext private val context: Context
) {

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
     * Get daily calorie summary for a date
     */
    suspend fun getDailyCalorieSummary(date: Date): AuthResult<DailyCalorieSummary> {
        return withContext(Dispatchers.IO) {
            when (val result = getMealsForDate(date)) {
                is AuthResult.Success -> {
                    val meals = result.data
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

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val summary = DailyCalorieSummary(
                        date = dateFormat.format(date),
                        consumedCalories = meals.totalConsumedCalories(),
                        burnedCalories = 0, // TODO: Get from activity/steps data
                        goalCalories = 2000, // TODO: Get from user preferences
                        meals = mealSummaries
                    )

                    AuthResult.Success(summary)
                }
                is AuthResult.Error -> {
                    AuthResult.Error(result.message)
                }
            }
        }
    }

    // ============ Create Operations ============

    /**
     * Create a meal by uploading a picture for AI analysis
     */
    suspend fun createMealByPicture(imageUri: Uri): AuthResult<MealData> {
        return withContext(Dispatchers.IO) {
            try {
                // Convert Uri to File
                val file = uriToFile(imageUri)
                if (file == null) {
                    return@withContext AuthResult.Error("خطا در پردازش تصویر")
                }

                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    requestBody
                )

                val response = foodApiService.createMealByPicture(multipartBody)

                // Clean up temp file
                file.delete()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val meal = response.body()?.data
                    if (meal != null) {
                        Timber.i("✅ Meal created from picture: ${meal.mealName}")
                        AuthResult.Success(meal)
                    } else {
                        AuthResult.Error("خطا در تحلیل تصویر")
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
                it.mealName.equals(mealType.apiName, ignoreCase = true)
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
     * Add a dish to an existing meal
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
                    val meal = response.body()?.data
                    if (meal != null) {
                        Timber.i("✅ Dish added: $dishName")
                        AuthResult.Success(meal)
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