package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============ Meal/Food Request Models ============

@Serializable
data class CreateMealRequest(
    @SerialName("MealName")
    val mealName: String,
    @SerialName("ContainsFood")
    val containsFood: Boolean = true,
    @SerialName("FoodConfidence")
    val foodConfidence: Int = 0,
    @SerialName("MultipleDishes")
    val multipleDishes: Boolean = false,
    @SerialName("DishCountEstimate")
    val dishCountEstimate: Int = 1,
    @SerialName("TotalCaloriesMinKcal")
    val totalCaloriesMinKcal: Int = 0,
    @SerialName("TotalCaloriesMaxKcal")
    val totalCaloriesMaxKcal: Int = 0,
    @SerialName("TotalCaloriesAvgKcal")
    val totalCaloriesAvgKcal: Int = 0,
    @SerialName("Blurry")
    val blurry: Boolean = false,
    @SerialName("LowLight")
    val lowLight: Boolean = false,
    @SerialName("Occluded")
    val occluded: Boolean = false,
    @SerialName("ScaleReferencePresent")
    val scaleReferencePresent: Boolean = false,
    @SerialName("PackagingTextReadable")
    val packagingTextReadable: Boolean = false,
    @SerialName("MealQuality")
    val mealQuality: String = "good",
    @SerialName("TotalProteinLevel")
    val totalProteinLevel: String = "medium",
    @SerialName("TotalFiberLevel")
    val totalFiberLevel: String = "medium",
    @SerialName("TotalProcessingLevel")
    val totalProcessingLevel: String = "low",
    @SerialName("TotalAddedSugarLevel")
    val totalAddedSugarLevel: String = "low",
    @SerialName("TotalFatLevel")
    val totalFatLevel: String = "medium",
    @SerialName("TotalCarbLevel")
    val totalCarbLevel: String = "medium",
    @SerialName("TotalNutrientConfidence")
    val totalNutrientConfidence: Int = 80,
    @SerialName("Dishes")
    val dishes: List<DishRequest> = emptyList()
)

@Serializable
data class DishRequest(
    @SerialName("DishName")
    val dishName: String,
    @SerialName("MealImageId")
    val mealImageId: Int = 0,
    @SerialName("Amount")
    val amount: Int = 1,
    @SerialName("Unit")
    val unit: String = "عدد",
    @SerialName("PortionEstimate")
    val portionEstimate: String = "medium",
    @SerialName("PortionConfidence")
    val portionConfidence: Int = 80,
    @SerialName("CaloriesMinKcal")
    val caloriesMinKcal: Int = 0,
    @SerialName("CaloriesMaxKcal")
    val caloriesMaxKcal: Int = 0,
    @SerialName("CaloriesConfidence")
    val caloriesConfidence: Int = 80,
    @SerialName("CaloriesBasis")
    val caloriesBasis: String = "estimate",
    @SerialName("ProteinLevel")
    val proteinLevel: String = "medium",
    @SerialName("FiberLevel")
    val fiberLevel: String = "medium",
    @SerialName("ProcessingLevel")
    val processingLevel: String = "low",
    @SerialName("AddedSugarLevel")
    val addedSugarLevel: String = "low",
    @SerialName("FatLevel")
    val fatLevel: String = "medium",
    @SerialName("CarbLevel")
    val carbLevel: String = "medium",
    @SerialName("NutrientConfidence")
    val nutrientConfidence: Int = 80,
    @SerialName("NutrientBasis")
    val nutrientBasis: String = "estimate",
    @SerialName("DishNameCandidate")
    val dishNameCandidate: List<String> = emptyList(),
    @SerialName("DishVisibleComponent")
    val dishVisibleComponent: List<String> = emptyList(),
    @SerialName("DishAssumedIngredient")
    val dishAssumedIngredient: List<String> = emptyList(),
    @SerialName("DishPortionAssumption")
    val dishPortionAssumption: List<String> = emptyList(),
    @SerialName("DishNote")
    val dishNote: List<String> = emptyList()
)

@Serializable
data class CreateDishRequest(
    @SerialName("MealId")
    val mealId: Int,
    @SerialName("DishName")
    val dishName: String,
    @SerialName("Amount")
    val amount: Int = 1,
    @SerialName("Unit")
    val unit: String = "عدد",
    @SerialName("CaloriesMinKcal")
    val caloriesMinKcal: Int = 0,
    @SerialName("CaloriesMaxKcal")
    val caloriesMaxKcal: Int = 0
)

@Serializable
data class UpdateMealRequest(
    @SerialName("Id")
    val id: Int,
    @SerialName("MealName")
    val mealName: String? = null,
    @SerialName("TotalCaloriesMinKcal")
    val totalCaloriesMinKcal: Int? = null,
    @SerialName("TotalCaloriesMaxKcal")
    val totalCaloriesMaxKcal: Int? = null,
    @SerialName("TotalCaloriesAvgKcal")
    val totalCaloriesAvgKcal: Int? = null,
    @SerialName("MealQuality")
    val mealQuality: String? = null
)

@Serializable
data class UpdateDishRequest(
    @SerialName("Id")
    val id: Int,
    @SerialName("DishName")
    val dishName: String? = null,
    @SerialName("Amount")
    val amount: Int? = null,
    @SerialName("Unit")
    val unit: String? = null,
    @SerialName("CaloriesMinKcal")
    val caloriesMinKcal: Int? = null,
    @SerialName("CaloriesMaxKcal")
    val caloriesMaxKcal: Int? = null
)

// ============ Meal/Food Response Models ============

@Serializable
data class MealData(
    @SerialName("Id")
    val id: Int,
    @SerialName("UserId")
    val userId: String? = null,
    @SerialName("MealName")
    val mealName: String,
    @SerialName("MealImageId")
    val mealImageId: Int? = null,
    @SerialName("MealImageName")
    val mealImageName: String? = null,
    @SerialName("ContainsFood")
    val containsFood: Boolean = true,
    @SerialName("FoodConfidence")
    val foodConfidence: Int = 0,
    @SerialName("MultipleDishes")
    val multipleDishes: Boolean = false,
    @SerialName("DishCountEstimate")
    val dishCountEstimate: Int = 1,
    @SerialName("TotalCaloriesMinKcal")
    val totalCaloriesMinKcal: Int = 0,
    @SerialName("TotalCaloriesMaxKcal")
    val totalCaloriesMaxKcal: Int = 0,
    @SerialName("TotalCaloriesAvgKcal")
    val totalCaloriesAvgKcal: Int = 0,
    @SerialName("Blurry")
    val blurry: Boolean = false,
    @SerialName("LowLight")
    val lowLight: Boolean = false,
    @SerialName("Occluded")
    val occluded: Boolean = false,
    @SerialName("ScaleReferencePresent")
    val scaleReferencePresent: Boolean = false,
    @SerialName("PackagingTextReadable")
    val packagingTextReadable: Boolean = false,
    @SerialName("MealQuality")
    val mealQuality: String = "good",
    @SerialName("TotalProteinLevel")
    val totalProteinLevel: String = "medium",
    @SerialName("TotalFiberLevel")
    val totalFiberLevel: String = "medium",
    @SerialName("TotalProcessingLevel")
    val totalProcessingLevel: String = "low",
    @SerialName("TotalAddedSugarLevel")
    val totalAddedSugarLevel: String = "low",
    @SerialName("TotalFatLevel")
    val totalFatLevel: String = "medium",
    @SerialName("TotalCarbLevel")
    val totalCarbLevel: String = "medium",
    @SerialName("TotalNutrientConfidence")
    val totalNutrientConfidence: Int = 80,
    @SerialName("CreatedDate")
    val createdDate: String? = null,
    @SerialName("Dishes")
    val dishes: List<DishData> = emptyList()
)

@Serializable
data class DishData(
    @SerialName("Id")
    val id: Int,
    @SerialName("MealId")
    val mealId: Int,
    @SerialName("DishName")
    val dishName: String,
    @SerialName("MealImageId")
    val mealImageId: Int? = null,
    @SerialName("Amount")
    val amount: Int = 1,
    @SerialName("Unit")
    val unit: String = "عدد",
    @SerialName("PortionEstimate")
    val portionEstimate: String = "medium",
    @SerialName("PortionConfidence")
    val portionConfidence: Int = 80,
    @SerialName("CaloriesMinKcal")
    val caloriesMinKcal: Int = 0,
    @SerialName("CaloriesMaxKcal")
    val caloriesMaxKcal: Int = 0,
    @SerialName("CaloriesConfidence")
    val caloriesConfidence: Int = 80,
    @SerialName("CaloriesBasis")
    val caloriesBasis: String = "estimate",
    @SerialName("ProteinLevel")
    val proteinLevel: String = "medium",
    @SerialName("FiberLevel")
    val fiberLevel: String = "medium",
    @SerialName("ProcessingLevel")
    val processingLevel: String = "low",
    @SerialName("AddedSugarLevel")
    val addedSugarLevel: String = "low",
    @SerialName("FatLevel")
    val fatLevel: String = "medium",
    @SerialName("CarbLevel")
    val carbLevel: String = "medium",
    @SerialName("NutrientConfidence")
    val nutrientConfidence: Int = 80,
    @SerialName("NutrientBasis")
    val nutrientBasis: String = "estimate",
    @SerialName("DishNameCandidate")
    val dishNameCandidate: List<String> = emptyList(),
    @SerialName("DishVisibleComponent")
    val dishVisibleComponent: List<String> = emptyList(),
    @SerialName("DishAssumedIngredient")
    val dishAssumedIngredient: List<String> = emptyList(),
    @SerialName("DishPortionAssumption")
    val dishPortionAssumption: List<String> = emptyList(),
    @SerialName("DishNote")
    val dishNote: List<String> = emptyList(),
    @SerialName("CreatedDate")
    val createdDate: String? = null
)

// ============ UI Models ============

/**
 * Local meal type enum matching the design
 */
enum class MealType(val persianName: String, val apiName: String) {
    BREAKFAST("صبحانه", "breakfast"),
    LUNCH("نهار", "lunch"),
    DINNER("شام", "dinner"),
    SNACK("میان‌وعده", "snack");

    companion object {
        fun fromApiName(name: String): MealType {
            return values().find { it.apiName.equals(name, ignoreCase = true) }
                ?: SNACK
        }

        fun fromPersianName(name: String): MealType {
            return values().find { it.persianName == name }
                ?: SNACK
        }
    }
}

/**
 * UI model for displaying food items in the list
 */
data class FoodItemUi(
    val id: Int,
    val mealId: Int,
    val name: String,
    val calories: Int,
    val caloriesMin: Int = 0,
    val caloriesMax: Int = 0,
    val amount: Int = 1,
    val unit: String = "عدد",
    val mealType: MealType,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * UI model for meal summary card on main screen
 */
data class MealSummaryUi(
    val mealType: MealType,
    val items: List<FoodItemUi>,
    val totalCaloriesMin: Int,
    val totalCaloriesMax: Int
) {
    val itemCount: Int get() = items.size
    val hasItems: Boolean get() = items.isNotEmpty()
}

/**
 * UI model for daily calorie summary
 */
data class DailyCalorieSummary(
    val date: String,
    val consumedCalories: Int,
    val burnedCalories: Int,
    val goalCalories: Int = 2000,
    val meals: List<MealSummaryUi>
) {
    val netCalories: Int get() = consumedCalories - burnedCalories
    val remainingCalories: Int get() = goalCalories - netCalories
    val progressPercent: Float get() = (consumedCalories.toFloat() / goalCalories).coerceIn(0f, 1f)
}

/**
 * Health quality rating from AI analysis
 */
enum class HealthQuality(val persianName: String, val score: Int) {
    LIMITED("محدود", 1),
    MODERATE("متوسط", 2),
    GOOD("خوب", 3),
    NUTRITIOUS("مغذی", 4);

    companion object {
        fun fromString(quality: String): HealthQuality {
            return when (quality.lowercase()) {
                "limited", "poor" -> LIMITED
                "moderate", "medium" -> MODERATE
                "good" -> GOOD
                "nutritious", "excellent" -> NUTRITIOUS
                else -> MODERATE
            }
        }
    }
}

/**
 * Food scan result from camera/AI
 */
data class FoodScanResult(
    val imagePath: String,
    val mealName: String,
    val dishes: List<ScannedDish>,
    val healthQuality: HealthQuality,
    val healthScore: Int, // 0-100
    val totalCaloriesMin: Int,
    val totalCaloriesMax: Int,
    val proteinLevel: String,
    val fiberLevel: String,
    val fatLevel: String,
    val carbLevel: String
)

data class ScannedDish(
    val name: String,
    val caloriesMin: Int,
    val caloriesMax: Int,
    val amount: Int = 1,
    val unit: String = "عدد",
    val visibleComponents: List<String> = emptyList(),
    val assumedIngredients: List<String> = emptyList()
)

// ============ Extension Functions ============

/**
 * Convert API MealData to UI model
 */
fun MealData.toFoodItems(): List<FoodItemUi> {
    val mealType = MealType.fromApiName(mealName)
    return dishes.map { dish ->
        FoodItemUi(
            id = dish.id,
            mealId = id,
            name = dish.dishName,
            calories = (dish.caloriesMinKcal + dish.caloriesMaxKcal) / 2,
            caloriesMin = dish.caloriesMinKcal,
            caloriesMax = dish.caloriesMaxKcal,
            amount = dish.amount,
            unit = dish.unit,
            mealType = mealType,
            imageUrl = mealImageName
        )
    }
}

/**
 * Convert API MealData to MealSummaryUi
 */
fun MealData.toMealSummary(): MealSummaryUi {
    val mealType = MealType.fromApiName(mealName)
    return MealSummaryUi(
        mealType = mealType,
        items = toFoodItems(),
        totalCaloriesMin = totalCaloriesMinKcal,
        totalCaloriesMax = totalCaloriesMaxKcal
    )
}

/**
 * Group meals by type for display
 */
fun List<MealData>.groupByMealType(): Map<MealType, List<FoodItemUi>> {
    return flatMap { it.toFoodItems() }
        .groupBy { it.mealType }
}

/**
 * Calculate total consumed calories from meals
 */
fun List<MealData>.totalConsumedCalories(): Int {
    return sumOf { it.totalCaloriesAvgKcal }
}