package com.kroslabs.recipemanager.data.repository

import com.kroslabs.recipemanager.data.local.RecipeDao
import com.kroslabs.recipemanager.data.local.toDomain
import com.kroslabs.recipemanager.data.local.toEntity
import com.kroslabs.recipemanager.domain.model.Recipe
import com.kroslabs.recipemanager.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao
) {
    fun getAllRecipes(): Flow<List<Recipe>> =
        recipeDao.getAllRecipes().map { entities ->
            entities.map { it.toDomain() }
        }

    fun searchRecipes(query: String): Flow<List<Recipe>> =
        recipeDao.searchRecipes(query).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getRecipeById(id: String): Recipe? =
        recipeDao.getRecipeById(id)?.toDomain()

    suspend fun insertRecipe(recipe: Recipe) {
        recipeDao.insertRecipe(recipe.toEntity())
    }

    suspend fun insertRecipes(recipes: List<Recipe>) {
        recipeDao.insertRecipes(recipes.map { it.toEntity() })
    }

    suspend fun updateRecipe(recipe: Recipe) {
        val updated = recipe.copy(updatedAt = java.time.Instant.now().toString())
        recipeDao.updateRecipe(updated.toEntity())
    }

    suspend fun deleteRecipe(recipe: Recipe) {
        recipeDao.deleteRecipe(recipe.toEntity())
    }

    suspend fun deleteRecipeById(id: String) {
        recipeDao.deleteRecipeById(id)
    }

    suspend fun deleteAllRecipes() {
        recipeDao.deleteAllRecipes()
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
