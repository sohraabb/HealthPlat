package com.bonyad.healthplat.ui.dashboard.calory

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.FoodRepository
import com.bonyad.healthplat.domain.model.DailyCalorieSummary
import com.bonyad.healthplat.domain.model.DishRequest
import com.bonyad.healthplat.domain.model.FoodItemUi
import com.bonyad.healthplat.domain.model.FoodScanResult
import com.bonyad.healthplat.domain.model.HealthQuality
import com.bonyad.healthplat.domain.model.MealSummaryUi
import com.bonyad.healthplat.domain.model.MealType
import com.bonyad.healthplat.domain.model.ScannedDish
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

// ============ UI State Classes ============

sealed class CaloryUiState {
    object Loading : CaloryUiState()
    data class Success(val summary: DailyCalorieSummary) : CaloryUiState()
    data class Error(val message: String) : CaloryUiState()
}

data class DateItem(
    val dayNumber: String,
    val dayName: String,
    val date: Date,
    val isSelected: Boolean
)

// ============ ViewModel ============

@HiltViewModel
class CaloryViewModel @Inject constructor(
    private val foodRepository: FoodRepository
) : ViewModel() {

    // ============ UI State ============

    private val _uiState = MutableStateFlow<CaloryUiState>(CaloryUiState.Loading)
    val uiState: StateFlow<CaloryUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    private val _dateItems = MutableStateFlow<List<DateItem>>(emptyList())
    val dateItems: StateFlow<List<DateItem>> = _dateItems.asStateFlow()

    private val _currentMonth = MutableStateFlow(getCurrentPersianMonth())
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    // Meal summaries by type
    private val _mealSummaries = MutableStateFlow<Map<MealType, MealSummaryUi>>(emptyMap())
    val mealSummaries: StateFlow<Map<MealType, MealSummaryUi>> = _mealSummaries.asStateFlow()

    // Calorie totals
    private val _consumedCalories = MutableStateFlow(0)
    val consumedCalories: StateFlow<Int> = _consumedCalories.asStateFlow()

    private val _burnedCalories = MutableStateFlow(0)
    val burnedCalories: StateFlow<Int> = _burnedCalories.asStateFlow()

    private val _calorieGoal = MutableStateFlow(2000)
    val calorieGoal: StateFlow<Int> = _calorieGoal.asStateFlow()

    // Food items list (flattened from all meals)
    private val _allFoodItems = MutableStateFlow<List<FoodItemUi>>(emptyList())
    val allFoodItems: StateFlow<List<FoodItemUi>> = _allFoodItems.asStateFlow()

    // ============ Bottom Sheet State ============

    private val _showAddFoodSheet = MutableStateFlow(false)
    val showAddFoodSheet: StateFlow<Boolean> = _showAddFoodSheet.asStateFlow()

    private val _selectedMealType = MutableStateFlow<MealType?>(null)
    val selectedMealType: StateFlow<MealType?> = _selectedMealType.asStateFlow()

    // ============ Scan State ============

    private val _foodScanState = MutableStateFlow<FoodScanState>(FoodScanState.Idle)
    val foodScanState: StateFlow<FoodScanState> = _foodScanState.asStateFlow()

    // ============ Events ============

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _navigateToConsumedDetails = MutableSharedFlow<Unit>()
    val navigateToConsumedDetails: SharedFlow<Unit> = _navigateToConsumedDetails.asSharedFlow()

    private val _navigateToBurnedDetails = MutableSharedFlow<Unit>()
    val navigateToBurnedDetails: SharedFlow<Unit> = _navigateToBurnedDetails.asSharedFlow()

    private val _navigateToFoodScan = MutableSharedFlow<MealType>()
    val navigateToFoodScan: SharedFlow<MealType> = _navigateToFoodScan.asSharedFlow()

    // ============ Initialization ============

    init {
        initializeDateStrip()
        loadMealsForSelectedDate()
    }

    private fun initializeDateStrip() {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        val dates = mutableListOf<DateItem>()

        // Generate 7 days (6 days before + today)
        calendar.add(Calendar.DAY_OF_MONTH, -6)

        val persianDays = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")

        for (i in 0..6) {
            val date = calendar.time
            val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Convert to Persian week
            val persianDate = getPersianDay(date)

            dates.add(
                DateItem(
                    dayNumber = persianDate,
                    dayName = persianDays[dayOfWeek],
                    date = date,
                    isSelected = isSameDay(date, today)
                )
            )

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        _dateItems.value = dates
        _currentMonth.value = getCurrentPersianMonth()
    }

    // ============ Data Loading ============

    fun loadMealsForSelectedDate() {
        viewModelScope.launch {
            _uiState.value = CaloryUiState.Loading

            when (val result = foodRepository.getDailyCalorieSummary(_selectedDate.value)) {
                is AuthResult.Success -> {
                    val summary = result.data

                    // Update individual states
                    _consumedCalories.value = summary.consumedCalories
                    _burnedCalories.value = summary.burnedCalories
                    _calorieGoal.value = summary.goalCalories

                    // Create meal summaries map
                    val summariesMap = summary.meals.associateBy { it.mealType }
                    _mealSummaries.value = summariesMap

                    // Flatten all food items
                    _allFoodItems.value = summary.meals.flatMap { it.items }

                    _uiState.value = CaloryUiState.Success(summary)
                }
                is AuthResult.Error -> {
                    _uiState.value = CaloryUiState.Error(result.message)
                    // Load empty state for UI
                    loadEmptyState()
                }
            }
        }
    }

    private fun loadEmptyState() {
        val emptyMeals = MealType.values().associateWith { mealType ->
            MealSummaryUi(
                mealType = mealType,
                items = emptyList(),
                totalCaloriesMin = 0,
                totalCaloriesMax = 0
            )
        }
        _mealSummaries.value = emptyMeals
        _consumedCalories.value = 0
        _allFoodItems.value = emptyList()
    }

    fun refresh() {
        loadMealsForSelectedDate()
    }

    // ============ Date Selection ============

    fun onDateSelected(date: Date) {
        if (isSameDay(date, _selectedDate.value)) return

        _selectedDate.value = date

        // Update date items selection
        _dateItems.value = _dateItems.value.map {
            it.copy(isSelected = isSameDay(it.date, date))
        }

        loadMealsForSelectedDate()
    }

    // ============ Add Food Operations ============

    fun onAddFoodClick(mealType: MealType) {
        _selectedMealType.value = mealType
        _showAddFoodSheet.value = true
    }

    fun dismissAddFoodSheet() {
        _showAddFoodSheet.value = false
        _selectedMealType.value = null
    }

    fun addFoodItem(
        dishName: String,
        caloriesMin: Int,
        caloriesMax: Int,
        amount: Int = 1,
        unit: String = "عدد"
    ) {
        val mealType = _selectedMealType.value ?: return

        viewModelScope.launch {
            // Use createMeal instead of addFoodItem to avoid MealImage not found error
            // This works for both new and existing meals
            when (val result = foodRepository.createMeal(
                mealType = mealType,
                dishes = listOf(
                    DishRequest(
                        dishName = dishName,
                        caloriesMinKcal = caloriesMin,
                        caloriesMaxKcal = caloriesMax,
                        amount = amount,
                        unit = unit
                    )
                )
            )) {
                is AuthResult.Success -> {
                    Timber.i("✅ Food added: $dishName")
                    _toastMessage.emit("$dishName اضافه شد")
                    dismissAddFoodSheet()
                    loadMealsForSelectedDate() // Refresh data
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to add food: ${result.message}")
                    _toastMessage.emit(result.message)
                }
            }
        }
    }

    /**
     * Simplified add for backward compatibility
     */
    fun addFoodItem(name: String, calories: Int, mealType: MealType) {
        _selectedMealType.value = mealType
        addFoodItem(
            dishName = name,
            caloriesMin = (calories * 0.9).toInt(),
            caloriesMax = (calories * 1.1).toInt()
        )
    }

    // ============ Delete Operations ============

    fun deleteFoodItem(item: FoodItemUi) {
        viewModelScope.launch {
            when (val result = foodRepository.deleteDish(item.mealId, item.id)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Food deleted: ${item.name}")
                    _toastMessage.emit("${item.name} حذف شد")
                    loadMealsForSelectedDate() // Refresh data
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to delete food: ${result.message}")
                    _toastMessage.emit(result.message)
                }
            }
        }
    }

    // ============ Food Scan Operations ============

    fun onScanFoodClick(mealType: MealType) {
        viewModelScope.launch {
            _navigateToFoodScan.emit(mealType)
        }
    }

    fun scanFood(imageUri: Uri) {
        viewModelScope.launch {
            _foodScanState.value = FoodScanState.Loading

            when (val result = foodRepository.createMealByPicture(imageUri)) {
                is AuthResult.Success -> {
                    val meal = result.data
                    // Convert MealData to FoodScanResult for UI
                    val scanResult = FoodScanResult(
                        imagePath = imageUri.toString(),
                        mealName = meal.mealName,
                        dishes = meal.dishes.map { dish ->
                            ScannedDish(
                                name = dish.dishName,
                                caloriesMin = dish.caloriesMinKcal,
                                caloriesMax = dish.caloriesMaxKcal,
                                amount = dish.amount,
                                unit = dish.unit,
                                visibleComponents = dish.dishVisibleComponent,
                                assumedIngredients = dish.dishAssumedIngredient
                            )
                        },
                        healthQuality = HealthQuality.fromString(meal.mealQuality),
                        healthScore = meal.totalNutrientConfidence,
                        totalCaloriesMin = meal.totalCaloriesMinKcal,
                        totalCaloriesMax = meal.totalCaloriesMaxKcal,
                        proteinLevel = meal.totalProteinLevel,
                        fiberLevel = meal.totalFiberLevel,
                        fatLevel = meal.totalFatLevel,
                        carbLevel = meal.totalCarbLevel
                    )
                    _foodScanState.value = FoodScanState.Success(scanResult)
                }
                is AuthResult.Error -> {
                    _foodScanState.value = FoodScanState.Error(result.message)
                }
            }
        }
    }

    /**
     * Save scan result with edited dishes
     */
    fun saveScanResult(
        imageUri: Uri,
        mealType: MealType,
        dishes: List<ScannedDish>
    ) {
        viewModelScope.launch {
            // If we already have the meal from scan, just update
            // Otherwise create a new meal with the dishes
            val dishRequests = dishes.map { dish ->
                DishRequest(
                    dishName = dish.name,
                    caloriesMinKcal = dish.caloriesMin,
                    caloriesMaxKcal = dish.caloriesMax,
                    amount = dish.amount,
                    unit = dish.unit
                )
            }

            when (val result = foodRepository.createMeal(
                mealType = mealType,
                dishes = dishRequests
            )) {
                is AuthResult.Success -> {
                    Timber.i("✅ Scan result saved")
                    _toastMessage.emit("وعده ذخیره شد")
                    loadMealsForSelectedDate()
                    resetScanState()
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to save scan result: ${result.message}")
                    _toastMessage.emit(result.message)
                }
            }
        }
    }

    fun resetScanState() {
        _foodScanState.value = FoodScanState.Idle
    }

    // ============ Navigation Events ============

    fun onConsumedCaloriesClick() {
        viewModelScope.launch {
            _navigateToConsumedDetails.emit(Unit)
        }
    }

    fun onBurnedCaloriesClick() {
        viewModelScope.launch {
            _navigateToBurnedDetails.emit(Unit)
        }
    }

    // ============ Helper Functions ============

    private fun getCurrentPersianMonth(): String {
        val persianDate = PersianDateUtils.getCurrentPersianDateTime()
        // date format: "1404/09/05"
        val parts = persianDate.date.split("/")
        if (parts.size != 3) return "ماه"

        val monthNames = arrayOf(
            "فروردین", "اردیبهشت", "خرداد",
            "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر",
            "دی", "بهمن", "اسفند"
        )

        val month = parts[1].toIntOrNull()?.minus(1) ?: return "ماه"
        return if (month in monthNames.indices) {
            "${monthNames[month]} ماه"
        } else "ماه"
    }

    private fun getPersianDay(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        // Simple Jalali conversion for display
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Note: This is a simplified conversion. In production, use proper Jalali calendar
        return day.toString().map { char ->
            if (char.isDigit()) persianDigits[char.toString().toInt()] else char
        }.joinToString("")
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Get meal summary for a specific meal type
     */
    fun getMealSummary(mealType: MealType): MealSummaryUi {
        return _mealSummaries.value[mealType] ?: MealSummaryUi(
            mealType = mealType,
            items = emptyList(),
            totalCaloriesMin = 0,
            totalCaloriesMax = 0
        )
    }

    /**
     * Get food items for a specific meal type
     */
    fun getFoodItemsForMealType(mealType: MealType): List<FoodItemUi> {
        return _allFoodItems.value.filter { it.mealType == mealType }
    }
}