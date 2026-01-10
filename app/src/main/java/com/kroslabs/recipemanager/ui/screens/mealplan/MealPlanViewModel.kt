package com.kroslabs.recipemanager.ui.screens.mealplan

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kroslabs.recipemanager.data.local.PreferencesManager
import com.kroslabs.recipemanager.data.repository.MealPlanRepository
import com.kroslabs.recipemanager.data.repository.RecipeRepository
import com.kroslabs.recipemanager.domain.model.*
import com.kroslabs.recipemanager.util.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

data class MealPlanUiState(
    val mealPlans: Map<LocalDate, MealPlan> = emptyMap(),
    val recipes: List<Recipe> = emptyList(),
    val language: Language = Language.ENGLISH,
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val showRecipeSelector: Boolean = false,
    val selectingSlot: MealType? = null,
    val showShareDialog: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class MealPlanViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "MealPlanViewModel"
    }

    private val _uiState = MutableStateFlow(MealPlanUiState())
    val uiState: StateFlow<MealPlanUiState> = _uiState.asStateFlow()

    private fun calculateDateRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return today.minusDays(90) to today.plusDays(180)
    }

    private val dateRange = calculateDateRange()

    init {
        DebugLogger.d(TAG, "init: Starting MealPlanViewModel, date range: ${dateRange.first} to ${dateRange.second}")
        viewModelScope.launch {
            combine(
                mealPlanRepository.getMealPlansByDateRange(dateRange.first, dateRange.second),
                recipeRepository.getAllRecipes(),
                preferencesManager.language
            ) { mealPlans, recipes, language ->
                DebugLogger.d(TAG, "init: Loaded ${mealPlans.size} meal plans, ${recipes.size} recipes")
                val mealPlanMap = mealPlans.associateBy { LocalDate.parse(it.date) }
                _uiState.update { state ->
                    state.copy(
                        mealPlans = mealPlanMap,
                        recipes = recipes,
                        language = language,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun onDateSelect(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onSlotClick(date: LocalDate, mealType: MealType) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                selectingSlot = mealType,
                showRecipeSelector = true,
                searchQuery = ""
            )
        }
    }

    fun onRecipeSelect(recipe: Recipe) {
        val date = _uiState.value.selectedDate
        val mealType = _uiState.value.selectingSlot ?: return

        viewModelScope.launch {
            val existingPlan = _uiState.value.mealPlans[date]
                ?: mealPlanRepository.getOrCreateMealPlanForDate(date)

            val updatedPlan = when (mealType) {
                MealType.LUNCH -> existingPlan.copy(
                    lunchSlot = existingPlan.lunchSlot.copy(
                        recipeId = recipe.id,
                        recipeName = recipe.getTitle(_uiState.value.language)
                    )
                )
                MealType.DINNER -> existingPlan.copy(
                    dinnerSlot = existingPlan.dinnerSlot.copy(
                        recipeId = recipe.id,
                        recipeName = recipe.getTitle(_uiState.value.language)
                    )
                )
            }

            mealPlanRepository.updateMealPlan(updatedPlan)
            _uiState.update {
                it.copy(
                    showRecipeSelector = false,
                    selectingSlot = null
                )
            }
        }
    }

    fun onRemoveMeal(date: LocalDate, mealType: MealType) {
        viewModelScope.launch {
            val existingPlan = _uiState.value.mealPlans[date] ?: return@launch

            val updatedPlan = when (mealType) {
                MealType.LUNCH -> existingPlan.copy(
                    lunchSlot = existingPlan.lunchSlot.copy(recipeId = null, recipeName = null)
                )
                MealType.DINNER -> existingPlan.copy(
                    dinnerSlot = existingPlan.dinnerSlot.copy(recipeId = null, recipeName = null)
                )
            }

            mealPlanRepository.updateMealPlan(updatedPlan)
        }
    }

    fun onSwapMeals(date: LocalDate) {
        viewModelScope.launch {
            val existingPlan = _uiState.value.mealPlans[date] ?: return@launch

            val updatedPlan = existingPlan.copy(
                lunchSlot = existingPlan.lunchSlot.copy(
                    recipeId = existingPlan.dinnerSlot.recipeId,
                    recipeName = existingPlan.dinnerSlot.recipeName
                ),
                dinnerSlot = existingPlan.dinnerSlot.copy(
                    recipeId = existingPlan.lunchSlot.recipeId,
                    recipeName = existingPlan.lunchSlot.recipeName
                )
            )

            mealPlanRepository.updateMealPlan(updatedPlan)
        }
    }

    fun onDismissRecipeSelector() {
        _uiState.update {
            it.copy(
                showRecipeSelector = false,
                selectingSlot = null,
                searchQuery = ""
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun getFilteredRecipes(): List<Recipe> {
        val query = _uiState.value.searchQuery.lowercase()
        if (query.isBlank()) return _uiState.value.recipes

        return _uiState.value.recipes.filter { recipe ->
            recipe.titleEnglish.lowercase().contains(query) ||
            recipe.titleSwedish.lowercase().contains(query) ||
            recipe.titleRomanian.lowercase().contains(query)
        }
    }

    fun onShowShareDialog(show: Boolean) {
        _uiState.update { it.copy(showShareDialog = show) }
    }

    fun shareMealPlan(
        context: Context,
        startDate: LocalDate,
        endDate: LocalDate,
        includeShoppingList: Boolean
    ) {
        val language = _uiState.value.language
        val mealPlans = _uiState.value.mealPlans
        val recipes = _uiState.value.recipes.associateBy { it.id }

        val locale = when (language) {
            Language.ENGLISH -> Locale.ENGLISH
            Language.SWEDISH -> Locale("sv")
            Language.ROMANIAN -> Locale("ro")
        }

        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)

        val text = buildString {
            appendLine("Meal Plan: ${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} - ${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
            appendLine()

            var currentDate = startDate
            val ingredientCounts = mutableMapOf<String, Int>()

            while (!currentDate.isAfter(endDate)) {
                val plan = mealPlans[currentDate]

                appendLine(currentDate.format(dateFormatter))

                val lunchName = plan?.lunchSlot?.recipeId?.let { id ->
                    recipes[id]?.getTitle(language)
                } ?: plan?.lunchSlot?.recipeName ?: "-"

                val dinnerName = plan?.dinnerSlot?.recipeId?.let { id ->
                    recipes[id]?.getTitle(language)
                } ?: plan?.dinnerSlot?.recipeName ?: "-"

                appendLine("\uD83E\uDD57 Lunch: $lunchName")
                appendLine("\uD83C\uDF7D️ Dinner: $dinnerName")
                appendLine()

                if (includeShoppingList) {
                    plan?.lunchSlot?.recipeId?.let { id ->
                        recipes[id]?.getIngredients(language)?.forEach { ingredient ->
                            ingredientCounts[ingredient.name] = (ingredientCounts[ingredient.name] ?: 0) + 1
                        }
                    }
                    plan?.dinnerSlot?.recipeId?.let { id ->
                        recipes[id]?.getIngredients(language)?.forEach { ingredient ->
                            ingredientCounts[ingredient.name] = (ingredientCounts[ingredient.name] ?: 0) + 1
                        }
                    }
                }

                currentDate = currentDate.plusDays(1)
            }

            if (includeShoppingList && ingredientCounts.isNotEmpty()) {
                appendLine("---")
                appendLine("Shopping List:")
                ingredientCounts.entries.sortedBy { it.key }.forEach { (name, count) ->
                    if (count > 1) {
                        appendLine("- $name ($count recipes)")
                    } else {
                        appendLine("- $name")
                    }
                }
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Meal Plan"))

        _uiState.update { it.copy(showShareDialog = false) }
    }

    fun getDateRange(): Pair<LocalDate, LocalDate> = dateRange
}
