package com.kroslabs.recipemanager.ui.screens.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kroslabs.recipemanager.data.local.PreferencesManager
import com.kroslabs.recipemanager.data.repository.RecipeRepository
import com.kroslabs.recipemanager.domain.model.Language
import com.kroslabs.recipemanager.domain.model.Recipe
import com.kroslabs.recipemanager.domain.model.SortOption
import com.kroslabs.recipemanager.util.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeListUiState(
    val recipes: List<Recipe> = emptyList(),
    val filteredRecipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val sortOption: SortOption = SortOption.DATE_ADDED,
    val language: Language = Language.ENGLISH,
    val isLoading: Boolean = true,
    val showFilterDialog: Boolean = false,
    val showSortDialog: Boolean = false
)

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "RecipeListViewModel"
    }

    private val _uiState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    init {
        DebugLogger.d(TAG, "init: Starting RecipeListViewModel")
        viewModelScope.launch {
            combine(
                recipeRepository.getAllRecipes(),
                preferencesManager.language
            ) { recipes, language ->
                DebugLogger.d(TAG, "init: Loaded ${recipes.size} recipes, language: $language")
                _uiState.update { state ->
                    val filtered = recipeRepository.filterAndSortRecipes(
                        recipes = recipes,
                        searchQuery = state.searchQuery,
                        selectedTags = state.selectedTags,
                        sortOption = state.sortOption
                    )
                    DebugLogger.d(TAG, "init: Filtered to ${filtered.size} recipes")
                    state.copy(
                        recipes = recipes,
                        filteredRecipes = filtered,
                        language = language,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = recipeRepository.filterAndSortRecipes(
                recipes = state.recipes,
                searchQuery = query,
                selectedTags = state.selectedTags,
                sortOption = state.sortOption
            )
            state.copy(
                searchQuery = query,
                filteredRecipes = filtered
            )
        }
    }

    fun onTagToggle(tag: String) {
        _uiState.update { state ->
            val newTags = if (state.selectedTags.contains(tag)) {
                state.selectedTags - tag
            } else {
                state.selectedTags + tag
            }
            val filtered = recipeRepository.filterAndSortRecipes(
                recipes = state.recipes,
                searchQuery = state.searchQuery,
                selectedTags = newTags,
                sortOption = state.sortOption
            )
            state.copy(
                selectedTags = newTags,
                filteredRecipes = filtered
            )
        }
    }

    fun onClearFilters() {
        _uiState.update { state ->
            val filtered = recipeRepository.filterAndSortRecipes(
                recipes = state.recipes,
                searchQuery = state.searchQuery,
                selectedTags = emptySet(),
                sortOption = state.sortOption
            )
            state.copy(
                selectedTags = emptySet(),
                filteredRecipes = filtered
            )
        }
    }

    fun onSortOptionChange(sortOption: SortOption) {
        _uiState.update { state ->
            val filtered = recipeRepository.filterAndSortRecipes(
                recipes = state.recipes,
                searchQuery = state.searchQuery,
                selectedTags = state.selectedTags,
                sortOption = sortOption
            )
            state.copy(
                sortOption = sortOption,
                filteredRecipes = filtered,
                showSortDialog = false
            )
        }
    }

    fun onShowFilterDialog(show: Boolean) {
        _uiState.update { it.copy(showFilterDialog = show) }
    }

    fun onShowSortDialog(show: Boolean) {
        _uiState.update { it.copy(showSortDialog = show) }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            recipeRepository.deleteRecipe(recipe)
        }
    }
}
