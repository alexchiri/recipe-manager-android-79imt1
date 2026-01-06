package com.kroslabs.recipemanager.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class MealPlan(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // ISO 8601 date string
    val createdAt: String = java.time.Instant.now().toString(),
    val updatedAt: String = java.time.Instant.now().toString(),
    val lunchSlot: MealSlot = MealSlot(mealType = MealType.LUNCH),
    val dinnerSlot: MealSlot = MealSlot(mealType = MealType.DINNER)
)

@Serializable
data class MealSlot(
    val id: String = UUID.randomUUID().toString(),
    val mealType: MealType,
    val recipeId: String? = null,
    val recipeName: String? = null
)

@Serializable
enum class MealType {
    LUNCH,
    DINNER
}
