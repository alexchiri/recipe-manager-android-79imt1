package com.kroslabs.recipemanager.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kroslabs.recipemanager.domain.model.MealPlan
import com.kroslabs.recipemanager.domain.model.MealSlot
import com.kroslabs.recipemanager.domain.model.MealType

@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey
    val id: String,
    val date: String,
    val createdAt: String,
    val updatedAt: String,

    // Lunch slot
    val lunchSlotId: String,
    val lunchRecipeId: String?,
    val lunchRecipeName: String?,

    // Dinner slot
    val dinnerSlotId: String,
    val dinnerRecipeId: String?,
    val dinnerRecipeName: String?
)

fun MealPlanEntity.toDomain(): MealPlan = MealPlan(
    id = id,
    date = date,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lunchSlot = MealSlot(
        id = lunchSlotId,
        mealType = MealType.LUNCH,
        recipeId = lunchRecipeId,
        recipeName = lunchRecipeName
    ),
    dinnerSlot = MealSlot(
        id = dinnerSlotId,
        mealType = MealType.DINNER,
        recipeId = dinnerRecipeId,
        recipeName = dinnerRecipeName
    )
)

fun MealPlan.toEntity(): MealPlanEntity = MealPlanEntity(
    id = id,
    date = date,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lunchSlotId = lunchSlot.id,
    lunchRecipeId = lunchSlot.recipeId,
    lunchRecipeName = lunchSlot.recipeName,
    dinnerSlotId = dinnerSlot.id,
    dinnerRecipeId = dinnerSlot.recipeId,
    dinnerRecipeName = dinnerSlot.recipeName
)
