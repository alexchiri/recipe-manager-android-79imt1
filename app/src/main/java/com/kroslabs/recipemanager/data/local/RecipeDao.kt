package com.kroslabs.recipemanager.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeById(id: String): RecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: String)

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()

    @Query("""
        SELECT * FROM recipes
        WHERE titleEnglish LIKE '%' || :query || '%'
        OR titleSwedish LIKE '%' || :query || '%'
        OR titleRomanian LIKE '%' || :query || '%'
        OR ingredientsEnglish LIKE '%' || :query || '%'
        OR ingredientsSwedish LIKE '%' || :query || '%'
        OR ingredientsRomanian LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchRecipes(query: String): Flow<List<RecipeEntity>>
}
