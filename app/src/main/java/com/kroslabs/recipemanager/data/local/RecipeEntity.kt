package com.kroslabs.recipemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.kroslabs.recipemanager.domain.model.Ingredient
import com.kroslabs.recipemanager.domain.model.Language
import com.kroslabs.recipemanager.domain.model.Recipe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "recipes")
@TypeConverters(RecipeConverters::class)
data class RecipeEntity(
    @PrimaryKey
    val id: String,
    val createdAt: String,
    val updatedAt: String,

    val titleEnglish: String,
    val titleSwedish: String,
    val titleRomanian: String,

    val ingredientsEnglish: List<Ingredient>,
    val ingredientsSwedish: List<Ingredient>,
    val ingredientsRomanian: List<Ingredient>,

    val instructionsEnglish: List<String>,
    val instructionsSwedish: List<String>,
    val instructionsRomanian: List<String>,

    val notesEnglish: String?,
    val notesSwedish: String?,
    val notesRomanian: String?,
    val notes: String?,

    val tags: List<String>,
    val servings: Int?,
    val prepTime: String?,
    val cookTime: String?,
    val rating: Int?,
    val detectedLanguage: String?
)

fun RecipeEntity.toDomain(): Recipe = Recipe(
    id = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    titleEnglish = titleEnglish,
    titleSwedish = titleSwedish,
    titleRomanian = titleRomanian,
    ingredientsEnglish = ingredientsEnglish,
    ingredientsSwedish = ingredientsSwedish,
    ingredientsRomanian = ingredientsRomanian,
    instructionsEnglish = instructionsEnglish,
    instructionsSwedish = instructionsSwedish,
    instructionsRomanian = instructionsRomanian,
    notesEnglish = notesEnglish,
    notesSwedish = notesSwedish,
    notesRomanian = notesRomanian,
    notes = notes,
    tags = tags,
    servings = servings,
    prepTime = prepTime,
    cookTime = cookTime,
    rating = rating,
    detectedLanguage = detectedLanguage
)

fun Recipe.toEntity(): RecipeEntity = RecipeEntity(
    id = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    titleEnglish = titleEnglish,
    titleSwedish = titleSwedish,
    titleRomanian = titleRomanian,
    ingredientsEnglish = ingredientsEnglish,
    ingredientsSwedish = ingredientsSwedish,
    ingredientsRomanian = ingredientsRomanian,
    instructionsEnglish = instructionsEnglish,
    instructionsSwedish = instructionsSwedish,
    instructionsRomanian = instructionsRomanian,
    notesEnglish = notesEnglish,
    notesSwedish = notesSwedish,
    notesRomanian = notesRomanian,
    notes = notes,
    tags = tags,
    servings = servings,
    prepTime = prepTime,
    cookTime = cookTime,
    rating = rating,
    detectedLanguage = detectedLanguage
)

class RecipeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromIngredientList(value: List<Ingredient>): String = json.encodeToString(value)

    @TypeConverter
    fun toIngredientList(value: String): List<Ingredient> = json.decodeFromString(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)
}
