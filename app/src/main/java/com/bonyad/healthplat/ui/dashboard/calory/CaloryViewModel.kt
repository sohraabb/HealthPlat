package com.bonyad.healthplat.ui.dashboard.calory

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.ActivityRepository
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.FoodRepository
import com.bonyad.healthplat.domain.model.ActivityFact
import com.bonyad.healthplat.domain.model.DailyCalorieSummary
import com.bonyad.healthplat.domain.model.DishRequest
import com.bonyad.healthplat.domain.model.FoodFactData
import com.bonyad.healthplat.domain.model.FoodItemUi
import com.bonyad.healthplat.domain.model.FoodScanResult
import com.bonyad.healthplat.domain.model.FoodTotalFacts
import com.bonyad.healthplat.domain.model.HealthQuality
import com.bonyad.healthplat.domain.model.MealSummaryUi
import com.bonyad.healthplat.domain.model.MealType
import com.bonyad.healthplat.domain.model.ScannedDish
import com.bonyad.healthplat.domain.model.groupByMealType
import com.bonyad.healthplat.domain.model.totalConsumedCalories
import com.bonyad.healthplat.domain.model.UserActivity
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
    private val foodRepository: FoodRepository,
    private val activityRepository: ActivityRepository,
    private val userPreferences: UserPreferencesDataStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ============ UI State ============

    private val _uiState = MutableStateFlow<CaloryUiState>(CaloryUiState.Loading)
    val uiState: StateFlow<CaloryUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(
        savedStateHandle.get<String>("date")?.let { dateStr ->
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
        } ?: Date()
    )
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

    // ============ Save State ============

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ============ Edit-Dish State (unit dropdown options from food-facts DB) ============

    private val _editUnitOptions = MutableStateFlow<List<FoodFactData>>(emptyList())
    val editUnitOptions: StateFlow<List<FoodFactData>> = _editUnitOptions.asStateFlow()

    private val _isEditUnitsLoading = MutableStateFlow(false)
    val isEditUnitsLoading: StateFlow<Boolean> = _isEditUnitsLoading.asStateFlow()

    // ============ Events ============

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _navigateToConsumedDetails = MutableSharedFlow<Unit>()
    val navigateToConsumedDetails: SharedFlow<Unit> = _navigateToConsumedDetails.asSharedFlow()

    private val _navigateToBurnedDetails = MutableSharedFlow<Unit>()
    val navigateToBurnedDetails: SharedFlow<Unit> = _navigateToBurnedDetails.asSharedFlow()

    private val _navigateAfterSave = MutableSharedFlow<Unit>()
    val navigateAfterSave: SharedFlow<Unit> = _navigateAfterSave.asSharedFlow()

    private val _navigateToFoodScan = MutableSharedFlow<MealType>()
    val navigateToFoodScan: SharedFlow<MealType> = _navigateToFoodScan.asSharedFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ============ Activity State ============

    private val _loggedActivities = MutableStateFlow<List<UserActivity>>(emptyList())
    val loggedActivities: StateFlow<List<UserActivity>> = _loggedActivities.asStateFlow()

    // Search results for the add-activity bottom sheet (paginated)
    private val _activitySearchResults = MutableStateFlow<List<ActivityFact>>(emptyList())
    val activitySearchResults: StateFlow<List<ActivityFact>> = _activitySearchResults.asStateFlow()

    private val _isActivitySearchLoading = MutableStateFlow(false)
    val isActivitySearchLoading: StateFlow<Boolean> = _isActivitySearchLoading.asStateFlow()

    private val _hasMoreActivityPages = MutableStateFlow(false)
    val hasMoreActivityPages: StateFlow<Boolean> = _hasMoreActivityPages.asStateFlow()

    // The activity fact the user tapped — moves bottom sheet to duration-picker phase
    private val _selectedActivityFact = MutableStateFlow<ActivityFact?>(null)
    val selectedActivityFact: StateFlow<ActivityFact?> = _selectedActivityFact.asStateFlow()

    private var activitySearchPage = 1
    private var currentActivityQuery = ""
    private var userWeightKg = 70 // fallback; loaded from DataStore in init

    // Pending scan context — stored between upload+analysis and user submit
    private var pendingMealId: Int? = null
    private var pendingMealImageId: Int? = null
    private var pendingTotalFacts: FoodTotalFacts? = null
    private var pendingMealQuality: String? = null

    // ============ Initialization ============

    init {
        initializeDateStrip()
        loadMealsForSelectedDate()
        loadActivitiesForSelectedDate()
        loadUserWeight()
    }

    private fun loadUserWeight() {
        viewModelScope.launch {
            userWeightKg = userPreferences.getUserWeight().first() ?: 70
            Timber.d("⚖️ User weight loaded: ${userWeightKg}kg")
        }
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
                    // NOTE: Do NOT set _burnedCalories here — it is owned by
                    // loadActivitiesForSelectedDate(). Setting it from both
                    // coroutines causes a race where the meals API (returning 0)
                    // overwrites the correct value from the activities API.
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

    /**
     * Load full meal data (with dishes) for the consumed calories detail screen.
     * Uses getMealsForDate instead of getDailySummary since the latter doesn't include dishes.
     */
    fun loadFullMealsForSelectedDate() {
        viewModelScope.launch {
            _uiState.value = CaloryUiState.Loading

            when (val result = foodRepository.getMealsForDate(_selectedDate.value)) {
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

                    _consumedCalories.value = meals.totalConsumedCalories()
                    _calorieGoal.value = 2000

                    val summariesMap = mealSummaries.associateBy { it.mealType }
                    _mealSummaries.value = summariesMap
                    _allFoodItems.value = mealSummaries.flatMap { it.items }

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    _uiState.value = CaloryUiState.Success(
                        DailyCalorieSummary(
                            date = dateFormat.format(_selectedDate.value),
                            consumedCalories = _consumedCalories.value,
                            burnedCalories = 0,
                            goalCalories = _calorieGoal.value,
                            meals = mealSummaries
                        )
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = CaloryUiState.Error(result.message)
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


    fun loadActivitiesForSelectedDate() {
        viewModelScope.launch {
            when (val result = activityRepository.getActivitiesForDate(_selectedDate.value)) {
                is AuthResult.Success -> {
                    _loggedActivities.value = result.data
                    // Sum ActivityCal from all logged activities for the burned total
                    _burnedCalories.value = result.data.sumOf { it.activityCal }.toInt()
                    Timber.d("✅ Loaded ${result.data.size} activities, burned=${_burnedCalories.value} kcal")
                }
                is AuthResult.Error -> {
                    Timber.w("⚠️ loadActivitiesForSelectedDate: ${result.message}")
                    _loggedActivities.value = emptyList()
                    _burnedCalories.value = 0
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadMealsForSelectedDate()
                loadActivitiesForSelectedDate()
            } finally {
                _isRefreshing.value = false
            }
        }
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
        loadActivitiesForSelectedDate()
    }

    // ============ Activity Operations ============

    /**
     * Start a new search (resets pagination to page 1).
     * Called when the user types in the search field of the add-activity sheet.
     */
    fun searchActivities(query: String) {
        if (query.isBlank()) {
            _activitySearchResults.value = emptyList()
            _hasMoreActivityPages.value = false
            return
        }
        currentActivityQuery = query
        activitySearchPage = 1
        viewModelScope.launch {
            _isActivitySearchLoading.value = true
            when (val result = activityRepository.searchActivityFacts(query, page = 1)) {
                is AuthResult.Success -> {
                    _activitySearchResults.value = result.data
                    _hasMoreActivityPages.value = result.data.size == 20
                }
                is AuthResult.Error -> {
                    Timber.w("⚠️ searchActivities: ${result.message}")
                    _toastMessage.emit(result.message)
                }
            }
            _isActivitySearchLoading.value = false
        }
    }

    /**
     * Load the next page of search results (infinite scroll).
     * No-op if there are no more pages or a load is already in progress.
     */
    fun loadMoreActivities() {
        if (!_hasMoreActivityPages.value || _isActivitySearchLoading.value) return
        activitySearchPage++
        viewModelScope.launch {
            _isActivitySearchLoading.value = true
            when (val result = activityRepository.searchActivityFacts(currentActivityQuery, activitySearchPage)) {
                is AuthResult.Success -> {
                    _activitySearchResults.value = _activitySearchResults.value + result.data
                    _hasMoreActivityPages.value = result.data.size == 20
                }
                is AuthResult.Error -> {
                    Timber.w("⚠️ loadMoreActivities: ${result.message}")
                    activitySearchPage-- // revert on failure so user can retry
                }
            }
            _isActivitySearchLoading.value = false
        }
    }

    /** Called when the user taps an activity fact in the search list. */
    fun selectActivityFact(fact: ActivityFact) {
        _selectedActivityFact.value = fact
    }

    /** Called when the user goes back from duration picker to search. */
    fun clearActivityFact() {
        _selectedActivityFact.value = null
    }

    /**
     * Log the selected activity with the given duration.
     * [durationHours] = hours + (minutes / 60.0)
     *
     * Burned calories use the standard MET formula:
     *     kcal = MET × 3.5 × weightKg × durationMinutes / 200
     * where MET is the activity's [ActivityFact.cal] value from the server and
     * duration is converted from hours to minutes.
     */
    fun logActivity(durationHours: Double) {
        val fact = _selectedActivityFact.value ?: return
        val durationMinutes = durationHours * 60.0
        val activityCal = fact.cal * 3.5 * userWeightKg * durationMinutes / 200.0

        viewModelScope.launch {
            when (val result = activityRepository.createActivity(fact, durationHours, activityCal)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Activity logged: ${fact.name}")
                    _toastMessage.emit("${fact.name} ثبت شد")
                    _selectedActivityFact.value = null
                    _activitySearchResults.value = emptyList()
                    loadActivitiesForSelectedDate()
                }
                is AuthResult.Error -> {
                    Timber.e("❌ logActivity: ${result.message}")
                    _toastMessage.emit(result.message)
                }
            }
        }
    }

    /**
     * Delete a logged activity by ID and refresh the list.
     */
    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            when (val result = activityRepository.deleteActivity(id)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Activity deleted: id=$id")
                    _toastMessage.emit("فعالیت حذف شد")
                    loadActivitiesForSelectedDate()
                }
                is AuthResult.Error -> {
                    Timber.e("❌ deleteActivity: ${result.message}")
                    _toastMessage.emit(result.message)
                }
            }
        }
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
        scanFood(listOf(imageUri))
    }

    /**
     * Two-step food scan flow:
     * 1. Upload image via createMealByPicture (port 7005) → get skeleton meal + image name
     * 2. Analyze food via analyzeFood (port 8003) → get nutritional data
     */
    fun scanFood(imageUris: List<Uri>) {
        viewModelScope.launch {
            _foodScanState.value = FoodScanState.Loading

            // Step 1: Upload image
            val uploadResult = foodRepository.createMealByPicture(imageUris)
            if (uploadResult is AuthResult.Error) {
                _foodScanState.value = FoodScanState.Error(uploadResult.message)
                return@launch
            }

            val meal = (uploadResult as AuthResult.Success).data
            val mealImage = meal.mealImages.firstOrNull()
            if (mealImage == null) {
                _foodScanState.value = FoodScanState.Error("تصویر آپلود شد اما اطلاعات تصویر دریافت نشد")
                return@launch
            }

            // Extract image name from imageUrl (file name part)
            val imageName = mealImage.imageUrl.substringAfterLast("/")

            // Step 2: AI analysis
            val analysisResult = foodRepository.analyzeFood(imageName)
            if (analysisResult is AuthResult.Error) {
                _foodScanState.value = FoodScanState.Error(analysisResult.message)
                return@launch
            }

            val analysis = (analysisResult as AuthResult.Success).data

            // Store pending context for submit
            pendingMealId = meal.id
            pendingMealImageId = mealImage.id
            pendingTotalFacts = analysis.totalFacts
            pendingMealQuality = analysis.mealQuality

            // Convert analysis to FoodScanResult for UI
            val healthQuality = HealthQuality.fromString(analysis.mealQuality)
            val scanResult = FoodScanResult(
                imagePath = imageUris.firstOrNull()?.toString() ?: "",
                mealName = meal.mealName?.ifBlank { "وعده غذایی" } ?: "وعده غذایی",
                mealId = meal.id,
                mealImageId = mealImage.id,
                dishes = analysis.itemFacts.map { item ->
                    val calInt = item.cal.toInt()
                    ScannedDish(
                        name = item.foodName,
                        caloriesMin = (calInt * 0.9).toInt(),
                        caloriesMax = (calInt * 1.1).toInt(),
                        amount = item.portion.toInt().coerceAtLeast(1),
                        unit = item.selectedUnit,
                        portion = item.portion,
                        fat = item.fat,
                        protein = item.protein,
                        carb = item.carb,
                        fiber = item.fiber,
                        sugar = item.sugar,
                        foodFactId = item.foodFactId
                    )
                },
                healthQuality = healthQuality,
                healthScore = healthQuality.score * 25, // 1-4 → 25-100
                totalFacts = analysis.totalFacts
            )
            _foodScanState.value = FoodScanState.Success(scanResult)
        }
    }

    /**
     * Save scan result with edited dishes.
     * Uses the pending context stored during scanFood() to submit via the two-step API.
     */
    fun saveScanResult(
        mealType: MealType,
        dishes: List<ScannedDish>
    ) {
        val mealId = pendingMealId
        val mealImageId = pendingMealImageId
        val totalFacts = pendingTotalFacts
        val mealQuality = pendingMealQuality

        if (mealId == null || mealImageId == null || totalFacts == null || mealQuality == null) {
            viewModelScope.launch {
                _toastMessage.emit("اطلاعات اسکن ناقص است، لطفاً دوباره اسکن کنید")
            }
            return
        }

        // Recompute totals from the (possibly edited) dishes so the saved meal stays
        // consistent with any amount/unit edits the user made on the result screen.
        val computedTotals = FoodTotalFacts(
            cal = dishes.sumOf { (it.caloriesMin + it.caloriesMax) / 2.0 },
            fat = dishes.sumOf { it.fat ?: 0.0 },
            protein = dishes.sumOf { it.protein ?: 0.0 },
            carb = dishes.sumOf { it.carb ?: 0.0 },
            fiber = dishes.sumOf { it.fiber ?: 0.0 },
            sugar = dishes.sumOf { it.sugar ?: 0.0 }
        )

        viewModelScope.launch {
            _isSaving.value = true

            when (val result = foodRepository.submitScanResult(
                mealId = mealId,
                mealImageId = mealImageId,
                dishes = dishes,
                mealQuality = mealQuality,
                totalFacts = computedTotals,
                mealType = mealType
            )) {
                is AuthResult.Success -> {
                    Timber.i("✅ Scan result saved")
                    _toastMessage.emit("وعده ذخیره شد")
                    resetScanState()
                    _isSaving.value = false
                    _navigateAfterSave.emit(Unit)
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to save scan result: ${result.message}")
                    _toastMessage.emit(result.message)
                    _isSaving.value = false
                }
            }
        }
    }

    fun resetScanState() {
        _foodScanState.value = FoodScanState.Idle
        pendingMealId = null
        pendingMealImageId = null
        pendingTotalFacts = null
        pendingMealQuality = null
    }

    // ============ Edit-Dish (amount/unit) ============

    /**
     * Load the available unit rows for a scanned dish so the edit bottom sheet can
     * offer a real unit dropdown and recompute nutrition exactly.
     *
     * The food-facts search is text-based (by name), so we search by the dish name and
     * anchor on [ScannedDish.foodFactId] to filter the results down to the exact food the
     * AI matched (the same name can map to several distinct foods). The resulting rows —
     * one per unit (گرم / پرس / لیوان …) — become the dropdown options.
     */
    fun loadUnitOptionsFor(dish: ScannedDish) {
        viewModelScope.launch {
            _isEditUnitsLoading.value = true
            _editUnitOptions.value = emptyList()
            when (val result = foodRepository.searchFoodFacts(dish.name)) {
                is AuthResult.Success -> {
                    val rows = result.data
                    // Anchor to the exact food via food_fact_id; fall back to name match.
                    val anchorName = rows.firstOrNull { it.id == dish.foodFactId }?.name ?: dish.name
                    _editUnitOptions.value = rows.filter { it.name == anchorName }
                    Timber.d("✏️ Loaded ${_editUnitOptions.value.size} unit options for '${dish.name}'")
                }
                is AuthResult.Error -> {
                    Timber.w("⚠️ loadUnitOptionsFor failed: ${result.message}")
                    _editUnitOptions.value = emptyList()
                }
            }
            _isEditUnitsLoading.value = false
        }
    }

    fun clearUnitOptions() {
        _editUnitOptions.value = emptyList()
        _isEditUnitsLoading.value = false
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