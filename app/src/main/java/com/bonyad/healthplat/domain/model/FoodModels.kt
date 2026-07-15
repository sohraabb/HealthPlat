package com.bonyad.healthplat.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
    val totalProteinLevel: Double = 0.0,
    @SerialName("TotalFiberLevel")
    val totalFiberLevel: Double = 0.0,
    @SerialName("TotalProcessingLevel")
    val totalProcessingLevel: String = "low",
    @SerialName("TotalSugarLevel")
    val totalSugarLevel: Double = 0.0,
    @SerialName("TotalFatLevel")
    val totalFatLevel: Double = 0.0,
    @SerialName("TotalCarbLevel")
    val totalCarbLevel: Double = 0.0,
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
    val proteinLevel: Double = 0.0,
    @SerialName("FiberLevel")
    val fiberLevel: Double = 0.0,
    @SerialName("ProcessingLevel")
    val processingLevel: String = "low",
    @SerialName("SugarLevel")
    val sugarLevel: Double = 0.0,
    @SerialName("FatLevel")
    val fatLevel: Double = 0.0,
    @SerialName("CarbLevel")
    val carbLevel: Double = 0.0,
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
    @SerialName("MealImageId")
    val mealImageId: Int? = null,
    @SerialName("Amount")
    val amount: Int = 1,
    @SerialName("Unit")
    val unit: String = "عدد",
    @SerialName("CaloriesMinKcal")
    val caloriesMinKcal: Int = 0,
    @SerialName("CaloriesMaxKcal")
    val caloriesMaxKcal: Int = 0,
    @SerialName("ProteinLevel")
    val proteinLevel: Double = 0.0,
    @SerialName("FiberLevel")
    val fiberLevel: Double = 0.0,
    @SerialName("FatLevel")
    val fatLevel: Double = 0.0,
    @SerialName("CarbLevel")
    val carbLevel: Double = 0.0,
    @SerialName("SugarLevel")
    val sugarLevel: Double = 0.0
)

@Serializable
data class UpdateMealRequest(
    @SerialName("MealId")
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
    val mealQuality: String? = null,
    @SerialName("ContainsFood")
    val containsFood: Boolean? = null,
    @SerialName("MultipleDishes")
    val multipleDishes: Boolean? = null,
    @SerialName("DishCountEstimate")
    val dishCountEstimate: Int? = null,
    @SerialName("TotalProteinLevel")
    val totalProteinLevel: Double? = null,
    @SerialName("TotalFiberLevel")
    val totalFiberLevel: Double? = null,
    @SerialName("TotalFatLevel")
    val totalFatLevel: Double? = null,
    @SerialName("TotalCarbLevel")
    val totalCarbLevel: Double? = null,
    @SerialName("TotalSugarLevel")
    val totalSugarLevel: Double? = null
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
data class MealImageData(
    @SerialName("Id")
    val id: Int,
    @SerialName("MealId")
    val mealId: Int,
    @SerialName("ImageUrl")
    val imageUrl: String,
    @SerialName("DisplayOrder")
    val displayOrder: Int = 0
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MealData(
    @SerialName("Id")
    @JsonNames("MealId")
    val id: Int = 0,
    @SerialName("UserId")
    val userId: String? = null,
    @SerialName("MealName")
    val mealName: String? = null,
    @SerialName("MealImages")
    val mealImages: List<MealImageData> = emptyList(),
    @SerialName("ContainsFood")
    val containsFood: Boolean = true,
    @SerialName("FoodConfidence")
    val foodConfidence: Double = 0.0,
    @SerialName("MultipleDishes")
    val multipleDishes: Boolean = false,
    @SerialName("DishCountEstimate")
    val dishCountEstimate: Double = 1.0,
    @SerialName("TotalCaloriesMinKcal")
    val totalCaloriesMinKcal: Double = 0.0,
    @SerialName("TotalCaloriesMaxKcal")
    val totalCaloriesMaxKcal: Double = 0.0,
    @SerialName("TotalCaloriesAvgKcal")
    val totalCaloriesAvgKcal: Double = 0.0,
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
    val totalProteinLevel: Double = 0.0,
    @SerialName("TotalFiberLevel")
    val totalFiberLevel: Double = 0.0,
    @SerialName("TotalProcessingLevel")
    val totalProcessingLevel: String = "low",
    @SerialName("TotalSugarLevel")
    val totalSugarLevel: Double = 0.0,
    @SerialName("TotalFatLevel")
    val totalFatLevel: Double = 0.0,
    @SerialName("TotalCarbLevel")
    val totalCarbLevel: Double = 0.0,
    @SerialName("TotalNutrientConfidence")
    val totalNutrientConfidence: Double = 80.0,
    @SerialName("TotalNutrientBasis")
    val totalNutrientBasis: String? = null,
    @SerialName("CreatedDate")
    val createdDate: String? = null,
    @SerialName("Dishes")
    val dishes: List<DishData> = emptyList()
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DishData(
    @SerialName("DishId")
    @JsonNames("Id")
    val dishId: Int = 0,
    @SerialName("MealId")
    val mealId: Int = 0,
    @SerialName("DishName")
    val dishName: String = "",
    @SerialName("MealImageId")
    val mealImageId: Int? = null,
    @SerialName("Amount")
    val amount: Double = 1.0,
    @SerialName("Unit")
    val unit: String = "عدد",
    @SerialName("PortionEstimate")
    val portionEstimate: String = "medium",
    @SerialName("PortionConfidence")
    val portionConfidence: Double = 80.0,
    @SerialName("CaloriesMinKcal")
    val caloriesMinKcal: Double = 0.0,
    @SerialName("CaloriesMaxKcal")
    val caloriesMaxKcal: Double = 0.0,
    @SerialName("CaloriesConfidence")
    val caloriesConfidence: Double = 80.0,
    @SerialName("CaloriesBasis")
    val caloriesBasis: String = "estimate",
    @SerialName("ProteinLevel")
    val proteinLevel: Double = 0.0,
    @SerialName("FiberLevel")
    val fiberLevel: Double = 0.0,
    @SerialName("ProcessingLevel")
    val processingLevel: String = "low",
    @SerialName("SugarLevel")
    val sugarLevel: Double = 0.0,
    @SerialName("FatLevel")
    val fatLevel: Double = 0.0,
    @SerialName("CarbLevel")
    val carbLevel: Double = 0.0,
    @SerialName("NutrientConfidence")
    val nutrientConfidence: Double = 80.0,
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

// ============ Daily Summary Response ============

@Serializable
data class DailySummaryData(
    @SerialName("Date")
    val date: String,
    @SerialName("TotalActivityDuration")
    val totalActivityDuration: Double = 0.0,
    @SerialName("TotalActivityCal")
    val totalActivityCal: Double = 0.0,
    @SerialName("TotalGainedCal")
    val totalGainedCal: Double = 0.0,
    @SerialName("Meals")
    val meals: List<MealData> = emptyList()
)

// ============ Food Analysis Models (AI service on port 8003) ============

@Serializable
data class FoodAnalysisData(
    @SerialName("total_facts")
    val totalFacts: FoodTotalFacts,
    @SerialName("item_facts")
    val itemFacts: List<FoodItemFact>,
    @SerialName("meal_quality")
    val mealQuality: String
)

// Nutrient fields are nullable: null means the server reported no value for that
// nutrient (hide it in the UI), whereas 0.0 is a real measured value (show it).
@Serializable
data class FoodTotalFacts(
    @SerialName("Cal")
    val cal: Double = 0.0,
    @SerialName("Fat")
    val fat: Double? = null,
    @SerialName("Protein")
    val protein: Double? = null,
    @SerialName("Carb")
    val carb: Double? = null,
    @SerialName("Fiber")
    val fiber: Double? = null,
    @SerialName("Sugar")
    val sugar: Double? = null
)

@Serializable
data class FoodItemFact(
    @SerialName("food_name")
    val foodName: String,
    @SerialName("selected_unit")
    val selectedUnit: String,
    @SerialName("portion")
    val portion: Double,
    @SerialName("Cal")
    val cal: Double = 0.0,
    @SerialName("Fat")
    val fat: Double? = null,
    @SerialName("Protein")
    val protein: Double? = null,
    @SerialName("Carb")
    val carb: Double? = null,
    @SerialName("Fiber")
    val fiber: Double? = null,
    @SerialName("Sugar")
    val sugar: Double? = null,
    @SerialName("food_fact_id")
    val foodFactId: Int? = null
)

// ============ Food Facts Search ============

@Serializable
data class FoodFactData(
    @SerialName("Id")
    val id: Int,
    @SerialName("Name")
    val name: String,
    @SerialName("Unit")
    val unit: String,
    @SerialName("Amount")
    val amount: Int,
    @SerialName("Cal")
    val cal: Double = 0.0,
    @SerialName("Fat")
    val fat: Double? = null,
    @SerialName("Protein")
    val protein: Double? = null,
    @SerialName("Carb")
    val carb: Double? = null,
    @SerialName("Fiber")
    val fiber: Double? = null,
    @SerialName("Sugar")
    val sugar: Double? = null,
    @SerialName("Source")
    val source: String? = null
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
                "moderate", "medium", "fair" -> MODERATE
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
    val mealId: Int,
    val mealImageId: Int,
    val dishes: List<ScannedDish>,
    val healthQuality: HealthQuality,
    val healthScore: Int, // 0-100
    val totalFacts: FoodTotalFacts
)

data class ScannedDish(
    val name: String,
    val caloriesMin: Int,
    val caloriesMax: Int,
    val amount: Int = 1,
    val unit: String = "عدد",
    val portion: Double = 1.0,
    val fat: Double? = null,
    val protein: Double? = null,
    val carb: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val foodFactId: Int? = null,
    val visibleComponents: List<String> = emptyList(),
    val assumedIngredients: List<String> = emptyList()
)

// ============ Extension Functions ============

/**
 * Convert API MealData to UI model
 */
fun MealData.toFoodItems(): List<FoodItemUi> {
    val mealType = MealType.fromApiName(mealName ?: "")
    return dishes.map { dish ->
        FoodItemUi(
            id = dish.dishId,
            mealId = id,
            name = dish.dishName,
            calories = ((dish.caloriesMinKcal + dish.caloriesMaxKcal) / 2).toInt(),
            caloriesMin = dish.caloriesMinKcal.toInt(),
            caloriesMax = dish.caloriesMaxKcal.toInt(),
            amount = dish.amount.toInt(),
            unit = dish.unit,
            mealType = mealType,
            imageUrl = mealImages.firstOrNull()?.imageUrl
        )
    }
}

/**
 * Convert API MealData to MealSummaryUi
 */
fun MealData.toMealSummary(): MealSummaryUi {
    val mealType = MealType.fromApiName(mealName ?: "")
    return MealSummaryUi(
        mealType = mealType,
        items = toFoodItems(),
        totalCaloriesMin = totalCaloriesMinKcal.toInt(),
        totalCaloriesMax = totalCaloriesMaxKcal.toInt()
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
    return sumOf { it.totalCaloriesAvgKcal.toInt() }
}
