package com.kroslabs.recipemanager.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Recipe(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: String = java.time.Instant.now().toString(),
    val updatedAt: String = java.time.Instant.now().toString(),

    // Multi-Language Content
    val titleEnglish: String = "",
    val titleSwedish: String = "",
    val titleRomanian: String = "",

    val ingredientsEnglish: List<Ingredient> = emptyList(),
    val ingredientsSwedish: List<Ingredient> = emptyList(),
    val ingredientsRomanian: List<Ingredient> = emptyList(),

    val instructionsEnglish: List<String> = emptyList(),
    val instructionsSwedish: List<String> = emptyList(),
    val instructionsRomanian: List<String> = emptyList(),

    val notesEnglish: String? = null,
    val notesSwedish: String? = null,
    val notesRomanian: String? = null,
    val notes: String? = null, // Legacy field

    // Metadata
    val tags: List<String> = emptyList(),
    val servings: Int? = null,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val rating: Int? = null,
    val detectedLanguage: String? = null
) {
    fun getTitle(language: Language): String = when (language) {
        Language.ENGLISH -> titleEnglish
        Language.SWEDISH -> titleSwedish
        Language.ROMANIAN -> titleRomanian
    }

    fun getIngredients(language: Language): List<Ingredient> = when (language) {
        Language.ENGLISH -> ingredientsEnglish
        Language.SWEDISH -> ingredientsSwedish
        Language.ROMANIAN -> ingredientsRomanian
    }

    fun getInstructions(language: Language): List<String> = when (language) {
        Language.ENGLISH -> instructionsEnglish
        Language.SWEDISH -> instructionsSwedish
        Language.ROMANIAN -> instructionsRomanian
    }

    fun getNotes(language: Language): String? = when (language) {
        Language.ENGLISH -> notesEnglish
        Language.SWEDISH -> notesSwedish
        Language.ROMANIAN -> notesRomanian
    } ?: notes
}

@Serializable
data class Ingredient(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val amount: String? = null,
    val unit: String? = null,
    val name: String
)

enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    SWEDISH("sv", "Svenska"),
    ROMANIAN("ro", "Română");

    companion object {
        fun fromCode(code: String): Language = entries.find { it.code == code } ?: ENGLISH
    }
}

enum class RecipeTag(val key: String, val emoji: String) {
    // Dietary Restrictions
    VEGETARIAN("vegetarian", "\uD83C\uDF31"),
    VEGAN("vegan", "\uD83E\uDD57"),
    GLUTEN_FREE("glutenFree", "\uD83C\uDF3E"),
    LACTOSE_FREE("lactoseFree", "\uD83E\uDD5B"),
    NUT_FREE("nutFree", "\uD83E\uDD5C"),

    // Diet Styles
    LOW_CARB("lowCarb", "\uD83E\uDD66"),
    KETO("keto", "\uD83E\uDD51"),
    PALEO("paleo", "\uD83E\uDD69"),

    // Meal Types
    BREAKFAST("breakfast", "\uD83C\uDF73"),
    LUNCH("lunch", "\uD83C\uDF5C"),
    DINNER("dinner", "\uD83C\uDF5D"),
    DESSERT("dessert", "\uD83C\uDF70"),
    SNACK("snack", "\uD83C\uDF6A"),
    BEVERAGE("beverage", "\uD83C\uDF79"),

    // Courses
    APPETIZER("appetizer", "\uD83E\uDD59"),
    MAIN_COURSE("mainCourse", "\uD83C\uDF56"),
    SIDE_DISH("sideDish", "\uD83E\uDD54"),
    SOUP("soup", "\uD83C\uDF5C"),
    SALAD("salad", "\uD83E\uDD57"),

    // Protein Types
    PORK("pork", "\uD83D\uDC37"),
    CHICKEN("chicken", "\uD83D\uDC14"),
    BEEF("beef", "\uD83D\uDC2E"),
    LAMB("lamb", "\uD83D\uDC11"),
    TURKEY("turkey", "\uD83E\uDD83"),
    FISH("fish", "\uD83D\uDC1F"),
    SEAFOOD("seafood", "\uD83E\uDD90"),

    // Cooking Style
    QUICK_EASY("quickEasy", "\u26A1"),
    SLOW_COOK("slowCook", "\uD83E\uDD58");

    companion object {
        fun fromKey(key: String): RecipeTag? = entries.find { it.key == key }

        val dietaryRestrictions = listOf(VEGETARIAN, VEGAN, GLUTEN_FREE, LACTOSE_FREE, NUT_FREE)
        val dietStyles = listOf(LOW_CARB, KETO, PALEO)
        val mealTypes = listOf(BREAKFAST, LUNCH, DINNER, DESSERT, SNACK, BEVERAGE)
        val courses = listOf(APPETIZER, MAIN_COURSE, SIDE_DISH, SOUP, SALAD)
        val proteinTypes = listOf(PORK, CHICKEN, BEEF, LAMB, TURKEY, FISH, SEAFOOD)
        val cookingStyles = listOf(QUICK_EASY, SLOW_COOK)
    }
}

enum class SortOption {
    DATE_ADDED,
    RATING
}
