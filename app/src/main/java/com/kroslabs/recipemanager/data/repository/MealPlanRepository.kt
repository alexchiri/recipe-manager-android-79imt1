package com.kroslabs.recipemanager.data.repository

import com.kroslabs.recipemanager.data.local.MealPlanDao
import com.kroslabs.recipemanager.data.local.toDomain
import com.kroslabs.recipemanager.data.local.toEntity
import com.kroslabs.recipemanager.domain.model.MealPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanRepository @Inject constructor(
    private val mealPlanDao: MealPlanDao
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getAllMealPlans(): Flow<List<MealPlan>> =
        mealPlanDao.getAllMealPlans().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getMealPlansByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MealPlan>> =
        mealPlanDao.getMealPlansByDateRange(
            startDate.format(dateFormatter),
            endDate.format(dateFormatter)
        ).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getMealPlanByDate(date: LocalDate): MealPlan? =
        mealPlanDao.getMealPlanByDate(date.format(dateFormatter))?.toDomain()

    suspend fun getMealPlanById(id: String): MealPlan? =
        mealPlanDao.getMealPlanById(id)?.toDomain()

    suspend fun insertMealPlan(mealPlan: MealPlan) {
        mealPlanDao.insertMealPlan(mealPlan.toEntity())
    }

    suspend fun insertMealPlans(mealPlans: List<MealPlan>) {
        mealPlanDao.insertMealPlans(mealPlans.map { it.toEntity() })
    }

    suspend fun updateMealPlan(mealPlan: MealPlan) {
        val updated = mealPlan.copy(updatedAt = java.time.Instant.now().toString())
        mealPlanDao.updateMealPlan(updated.toEntity())
    }

    suspend fun deleteMealPlan(mealPlan: MealPlan) {
        mealPlanDao.deleteMealPlan(mealPlan.toEntity())
    }

    suspend fun deleteMealPlanById(id: String) {
        mealPlanDao.deleteMealPlanById(id)
    }

    suspend fun deleteAllMealPlans() {
        mealPlanDao.deleteAllMealPlans()
    }

    suspend fun getOrCreateMealPlanForDate(date: LocalDate): MealPlan {
        val existing = getMealPlanByDate(date)
        if (existing != null) return existing

        val newMealPlan = MealPlan(date = date.format(dateFormatter))
        insertMealPlan(newMealPlan)
        return newMealPlan
    }
}
