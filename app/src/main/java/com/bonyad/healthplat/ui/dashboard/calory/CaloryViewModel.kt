package com.bonyad.healthplat.ui.dashboard.calory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CaloryViewModel @Inject constructor(
    // TODO: Inject CaloryRepository when backend is ready
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(getCurrentPersianDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _calorieGoal = MutableStateFlow(2000) // Daily goal
    val calorieGoal: StateFlow<Int> = _calorieGoal.asStateFlow()

    private val _foodItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodItems: StateFlow<List<FoodItem>> = _foodItems.asStateFlow()

    private val _showAddFoodSheet = MutableStateFlow(false)
    val showAddFoodSheet: StateFlow<Boolean> = _showAddFoodSheet.asStateFlow()

    private val _selectedMealType = MutableStateFlow<MealType?>(null)
    val selectedMealType: StateFlow<MealType?> = _selectedMealType.asStateFlow()

    // Calculated values
    val consumedCalories: StateFlow<Int> = MutableStateFlow(0).apply {
        viewModelScope.launch {
            _foodItems.collect { items ->
                value = items.sumOf { it.calories }
            }
        }
    }

    val burnedCalories: StateFlow<Int> = MutableStateFlow(300) // Mock value from activity

    val totalCalories: StateFlow<Int> = MutableStateFlow(0).apply {
        viewModelScope.launch {
            consumedCalories.collect { consumed ->
                value = consumed - burnedCalories.value
            }
        }
    }

    init {
        loadMockData()
    }

    fun onAddFoodClick(mealType: MealType) {
        _selectedMealType.value = mealType
        _showAddFoodSheet.value = true
    }

    fun dismissAddFoodSheet() {
        _showAddFoodSheet.value = false
        _selectedMealType.value = null
    }

    fun addFoodItem(name: String, calories: Int, mealType: MealType) {
        viewModelScope.launch {
            try {
                val newItem = FoodItem(
                    name = name,
                    calories = calories,
                    mealType = mealType
                )

                _foodItems.value = _foodItems.value + newItem
                Timber.i("Added food item: $name, $calories cal, $mealType")

                // TODO: Save to backend/database
            } catch (e: Exception) {
                Timber.e(e, "Failed to add food item")
            }
        }
    }

    fun deleteFoodItem(item: FoodItem) {
        viewModelScope.launch {
            try {
                _foodItems.value = _foodItems.value.filter { it.id != item.id }
                Timber.i("Deleted food item: ${item.name}")

                // TODO: Delete from backend/database
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete food item")
            }
        }
    }

    fun onDateSelected(date: String) {
        _selectedDate.value = date
        // TODO: Load food items for selected date
    }

    private fun getCurrentPersianDate(): String {
        val persianDate = PersianDateUtils.getCurrentPersianDateTime()
        // Format: "۲۲ مهر ۱۴۰۴"
        val monthName = getPersianMonthName(persianDate.date)
        return monthName
    }

    private fun getPersianMonthName(date: String): String {
        // date format: "1404/09/05"
        val parts = date.split("/")
        if (parts.size != 3) return date

        val monthNames = arrayOf(
            "فروردین", "اردیبهشت", "خرداد",
            "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر",
            "دی", "بهمن", "اسفند"
        )

        val day = parts[2].toIntOrNull() ?: return date
        val month = parts[1].toIntOrNull()?.minus(1) ?: return date

        return if (month in monthNames.indices) {
            "${convertToPersianNumber(day)} ${monthNames[month]}"
        } else {
            date
        }
    }

    private fun convertToPersianNumber(number: Int): String {
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        return number.toString().map { char ->
            if (char.isDigit()) persianDigits[char.toString().toInt()] else char
        }.joinToString("")
    }

    private fun loadMockData() {
        // Mock data for testing
        _foodItems.value = listOf(
            FoodItem(
                name = "صبحانه",
                calories = 455,
                mealType = MealType.BREAKFAST
            ),
            FoodItem(
                name = "نهار",
                calories = 950,
                mealType = MealType.LUNCH
            )
        )
    }
}