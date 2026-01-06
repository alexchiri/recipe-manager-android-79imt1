package com.kroslabs.recipemanager.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plans ORDER BY date ASC")
    fun getAllMealPlans(): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plans WHERE date = :date")
    suspend fun getMealPlanByDate(date: String): MealPlanEntity?

    @Query("SELECT * FROM meal_plans WHERE id = :id")
    suspend fun getMealPlanById(id: String): MealPlanEntity?

    @Query("SELECT * FROM meal_plans WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getMealPlansByDateRange(startDate: String, endDate: String): Flow<List<MealPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlans(mealPlans: List<MealPlanEntity>)

    @Update
    suspend fun updateMealPlan(mealPlan: MealPlanEntity)

    @Delete
    suspend fun deleteMealPlan(mealPlan: MealPlanEntity)

    @Query("DELETE FROM meal_plans WHERE id = :id")
    suspend fun deleteMealPlanById(id: String)

    @Query("DELETE FROM meal_plans")
    suspend fun deleteAllMealPlans()
}
