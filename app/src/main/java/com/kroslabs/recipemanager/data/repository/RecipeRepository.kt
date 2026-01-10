package com.kroslabs.recipemanager.data.repository

import com.kroslabs.recipemanager.data.local.RecipeDao
import com.kroslabs.recipemanager.data.local.toDomain
import com.kroslabs.recipemanager.data.local.toEntity
import com.kroslabs.recipemanager.domain.model.Recipe
import com.kroslabs.recipemanager.domain.model.SortOption
import com.kroslabs.recipemanager.util.DebugLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao
) {
    companion object {
        private const val TAG = "RecipeRepository"
    }

    fun getAllRecipes(): Flow<List<Recipe>> =
        recipeDao.getAllRecipes().map { entities ->
            entities.map { it.toDomain() }
        }.onEach { recipes ->
            DebugLogger.d(TAG, "getAllRecipes: Loaded ${recipes.size} recipes")
        }

    fun searchRecipes(query: String): Flow<List<Recipe>> {
        DebugLogger.d(TAG, "searchRecipes: Searching for '$query'")
        return recipeDao.searchRecipes(query).map { entities ->
            entities.map { it.toDomain() }
        }.onEach { recipes ->
            DebugLogger.d(TAG, "searchRecipes: Found ${recipes.size} recipes for '$query'")
        }
    }

    suspend fun getRecipeById(id: String): Recipe? {
        DebugLogger.d(TAG, "getRecipeById: Looking up recipe ID: $id")
        val recipe = recipeDao.getRecipeById(id)?.toDomain()
        DebugLogger.d(TAG, "getRecipeById: Found=${recipe != null}, title=${recipe?.titleEnglish}")
        return recipe
    }

    suspend fun insertRecipe(recipe: Recipe) {
        DebugLogger.i(TAG, "insertRecipe: Inserting recipe ID: ${recipe.id}, title: ${recipe.titleEnglish}")
        recipeDao.insertRecipe(recipe.toEntity())
        DebugLogger.d(TAG, "insertRecipe: Recipe inserted successfully")
    }

    suspend fun insertRecipes(recipes: List<Recipe>) {
        DebugLogger.i(TAG, "insertRecipes: Inserting ${recipes.size} recipes")
        recipeDao.insertRecipes(recipes.map { it.toEntity() })
        DebugLogger.d(TAG, "insertRecipes: Recipes inserted successfully")
    }

    suspend fun updateRecipe(recipe: Recipe) {
        DebugLogger.i(TAG, "updateRecipe: Updating recipe ID: ${recipe.id}, title: ${recipe.titleEnglish}")
        val updated = recipe.copy(updatedAt = java.time.Instant.now().toString())
        recipeDao.updateRecipe(updated.toEntity())
        DebugLogger.d(TAG, "updateRecipe: Recipe updated successfully")
    }

    suspend fun deleteRecipe(recipe: Recipe) {
        DebugLogger.i(TAG, "deleteRecipe: Deleting recipe ID: ${recipe.id}, title: ${recipe.titleEnglish}")
        recipeDao.deleteRecipe(recipe.toEntity())
        DebugLogger.d(TAG, "deleteRecipe: Recipe deleted successfully")
    }

    suspend fun deleteRecipeById(id: String) {
        DebugLogger.i(TAG, "deleteRecipeById: Deleting recipe ID: $id")
        recipeDao.deleteRecipeById(id)
        DebugLogger.d(TAG, "deleteRecipeById: Recipe deleted successfully")
    }

    suspend fun deleteAllRecipes() {
        DebugLogger.w(TAG, "deleteAllRecipes: Deleting ALL recipes!")
        recipeDao.deleteAllRecipes()
        DebugLogger.d(TAG, "deleteAllRecipes: All recipes deleted")
    }

    fun filterAndSortRecipes(
        recipes: List<Recipe>,
        searchQuery: String,
        selectedTags: Set<String>,
        sortOption: SortOption
    ): List<Recipe> {
        var filtered = recipes

        // Filter by search query
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            filtered = filtered.filter { recipe ->
                recipe.titleEnglish.lowercase().contains(query) ||
                recipe.titleSwedish.lowercase().contains(query) ||
                recipe.titleRomanian.lowercase().contains(query) ||
                recipe.ingredientsEnglish.any { it.name.lowercase().contains(query) } ||
                recipe.ingredientsSwedish.any { it.name.lowercase().contains(query) } ||
                recipe.ingredientsRomanian.any { it.name.lowercase().contains(query) }
            }
        }

        // Filter by tags (AND logic)
        if (selectedTags.isNotEmpty()) {
            filtered = filtered.filter { recipe ->
                selectedTags.all { tag -> recipe.tags.contains(tag) }
            }
        }

        // Sort
        filtered = when (sortOption) {
            SortOption.DATE_ADDED -> filtered.sortedByDescending { it.createdAt }
            SortOption.RATING -> filtered.sortedWith(
                compareByDescending<Recipe> { it.rating ?: 0 }
                    .thenByDescending { it.createdAt }
            )
        }

        return filtered
    }
}
