package com.kroslabs.recipemanager.data.remote

import android.util.Base64
import com.kroslabs.recipemanager.domain.model.Ingredient
import com.kroslabs.recipemanager.domain.model.Recipe
import com.kroslabs.recipemanager.util.DebugLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeExtractor @Inject constructor(
    private val claudeApiService: ClaudeApiService
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "RecipeExtractor"

        private const val EXTRACTION_PROMPT = """
You are a recipe extraction assistant. Extract the recipe information from the provided content and return it as a JSON object.

IMPORTANT: Return ONLY valid JSON, no additional text or explanation.

The JSON must have this exact structure:
{
  "titleEnglish": "Recipe title in English",
  "titleSwedish": "Recipe title in Swedish",
  "titleRomanian": "Recipe title in Romanian",
  "ingredientsEnglish": [
    {"text": "full ingredient text", "amount": "2", "unit": "cups", "name": "flour"}
  ],
  "ingredientsSwedish": [
    {"text": "full ingredient text in Swedish", "amount": "475", "unit": "ml", "name": "vetemjöl"}
  ],
  "ingredientsRomanian": [
    {"text": "full ingredient text in Romanian", "amount": "475", "unit": "ml", "name": "făină"}
  ],
  "instructionsEnglish": ["Step 1...", "Step 2..."],
  "instructionsSwedish": ["Steg 1...", "Steg 2..."],
  "instructionsRomanian": ["Pasul 1...", "Pasul 2..."],
  "servings": 4,
  "prepTime": "15 mins",
  "cookTime": "30 mins",
  "tags": ["vegetarian", "quickEasy"],
  "detectedLanguage": "English"
}

Rules:
1. Convert all measurements to metric (g, ml, kg, L)
2. Translate all content to English, Swedish, and Romanian
3. Detect and include relevant tags from: vegetarian, vegan, glutenFree, lactoseFree, nutFree, lowCarb, keto, paleo, breakfast, lunch, dinner, dessert, snack, beverage, appetizer, mainCourse, sideDish, soup, salad, pork, chicken, beef, lamb, turkey, fish, seafood, quickEasy, slowCook
4. Extract servings as integer, times as strings
5. Ensure all arrays have matching lengths across languages
"""

        private const val TRANSLATION_PROMPT = """
Translate the following recipe content to Swedish and Romanian. Return ONLY valid JSON with the translations.

Input recipe:
%s

Return JSON with this structure:
{
  "titleSwedish": "...",
  "titleRomanian": "...",
  "ingredientsSwedish": [{"text": "...", "amount": "...", "unit": "...", "name": "..."}],
  "ingredientsRomanian": [{"text": "...", "amount": "...", "unit": "...", "name": "..."}],
  "instructionsSwedish": ["..."],
  "instructionsRomanian": ["..."],
  "notesSwedish": "..." or null,
  "notesRomanian": "..." or null
}
"""
    }

    suspend fun extractFromImage(
        imageBytes: ByteArray,
        mimeType: String,
        apiKey: String
    ): Result<Recipe> = runCatching {
        DebugLogger.i(TAG, "extractFromImage: Starting, image size: ${imageBytes.size} bytes, mimeType: $mimeType")

        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        DebugLogger.d(TAG, "extractFromImage: Base64 encoded, length: ${base64Image.length}")

        val request = ClaudeRequest(
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = listOf(
                        ClaudeContent.Image(
                            source = ImageSource(
                                mediaType = mimeType,
                                data = base64Image
                            )
                        ),
                        ClaudeContent.Text(text = EXTRACTION_PROMPT)
                    )
                )
            )
        )

        DebugLogger.d(TAG, "extractFromImage: Sending request to Claude API...")
        val response = claudeApiService.createMessage(apiKey, request = request)
        DebugLogger.i(TAG, "extractFromImage: Received response, parsing...")

        val recipe = parseRecipeResponse(response)
        DebugLogger.i(TAG, "extractFromImage: Successfully parsed recipe: ${recipe.titleEnglish}")
        recipe
    }

    suspend fun extractFromText(
        text: String,
        apiKey: String
    ): Result<Recipe> = runCatching {
        DebugLogger.i(TAG, "extractFromText: Starting, text length: ${text.length}")
        DebugLogger.d(TAG, "extractFromText: Text preview: ${text.take(200)}...")

        val request = ClaudeRequest(
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = listOf(
                        ClaudeContent.Text(
                            text = "$EXTRACTION_PROMPT\n\nRecipe content:\n$text"
                        )
                    )
                )
            )
        )

        DebugLogger.d(TAG, "extractFromText: Sending request to Claude API...")
        val response = claudeApiService.createMessage(apiKey, request = request)
        DebugLogger.i(TAG, "extractFromText: Received response, parsing...")

        val recipe = parseRecipeResponse(response)
        DebugLogger.i(TAG, "extractFromText: Successfully parsed recipe: ${recipe.titleEnglish}")
        recipe
    }

    suspend fun extractFromUrl(
        url: String,
        htmlContent: String,
        apiKey: String
    ): Result<Recipe> = runCatching {
        DebugLogger.i(TAG, "extractFromUrl: Starting, URL: $url, content length: ${htmlContent.length}")
        DebugLogger.d(TAG, "extractFromUrl: HTML preview: ${htmlContent.take(500)}...")

        val request = ClaudeRequest(
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = listOf(
                        ClaudeContent.Text(
                            text = "$EXTRACTION_PROMPT\n\nURL: $url\n\nPage content:\n$htmlContent"
                        )
                    )
                )
            )
        )

        DebugLogger.d(TAG, "extractFromUrl: Sending request to Claude API...")
        val response = claudeApiService.createMessage(apiKey, request = request)
        DebugLogger.i(TAG, "extractFromUrl: Received response, parsing...")

        val recipe = parseRecipeResponse(response)
        DebugLogger.i(TAG, "extractFromUrl: Successfully parsed recipe: ${recipe.titleEnglish}")
        recipe
    }

    suspend fun translateRecipe(
        recipe: Recipe,
        apiKey: String
    ): Result<Recipe> = runCatching {
        val recipeJson = json.encodeToString(Recipe.serializer(), recipe)
        val prompt = TRANSLATION_PROMPT.format(recipeJson)

        val request = ClaudeRequest(
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = listOf(ClaudeContent.Text(text = prompt))
                )
            )
        )

        val response = claudeApiService.createMessage(apiKey, request = request)
        val responseText = response.content.firstOrNull { it.type == "text" }?.text
            ?: throw Exception("No text response from API")

        val translationJson = extractJsonFromResponse(responseText)
        val translation = json.parseToJsonElement(translationJson).jsonObject

        recipe.copy(
            titleSwedish = translation["titleSwedish"]?.jsonPrimitive?.content ?: recipe.titleSwedish,
            titleRomanian = translation["titleRomanian"]?.jsonPrimitive?.content ?: recipe.titleRomanian,
            ingredientsSwedish = parseIngredientArray(translation["ingredientsSwedish"]),
            ingredientsRomanian = parseIngredientArray(translation["ingredientsRomanian"]),
            instructionsSwedish = parseStringArray(translation["instructionsSwedish"]),
            instructionsRomanian = parseStringArray(translation["instructionsRomanian"]),
            notesSwedish = translation["notesSwedish"]?.jsonPrimitive?.contentOrNull,
            notesRomanian = translation["notesRomanian"]?.jsonPrimitive?.contentOrNull
        )
    }

    private fun parseRecipeResponse(response: ClaudeResponse): Recipe {
        DebugLogger.d(TAG, "parseRecipeResponse: Starting to parse response")

        val responseText = response.content.firstOrNull { it.type == "text" }?.text
            ?: run {
                DebugLogger.e(TAG, "parseRecipeResponse: No text response from API")
                throw Exception("No text response from API")
            }

        DebugLogger.d(TAG, "parseRecipeResponse: Response text length: ${responseText.length}")
        DebugLogger.d(TAG, "parseRecipeResponse: Response preview: ${responseText.take(500)}...")

        val recipeJson = extractJsonFromResponse(responseText)
        DebugLogger.d(TAG, "parseRecipeResponse: Extracted JSON length: ${recipeJson.length}")

        try {
            val jsonObject = json.parseToJsonElement(recipeJson).jsonObject

            val recipe = Recipe(
                id = UUID.randomUUID().toString(),
                titleEnglish = jsonObject["titleEnglish"]?.jsonPrimitive?.content ?: "",
                titleSwedish = jsonObject["titleSwedish"]?.jsonPrimitive?.content ?: "",
                titleRomanian = jsonObject["titleRomanian"]?.jsonPrimitive?.content ?: "",
                ingredientsEnglish = parseIngredientArray(jsonObject["ingredientsEnglish"]),
                ingredientsSwedish = parseIngredientArray(jsonObject["ingredientsSwedish"]),
                ingredientsRomanian = parseIngredientArray(jsonObject["ingredientsRomanian"]),
                instructionsEnglish = parseStringArray(jsonObject["instructionsEnglish"]),
                instructionsSwedish = parseStringArray(jsonObject["instructionsSwedish"]),
                instructionsRomanian = parseStringArray(jsonObject["instructionsRomanian"]),
                servings = jsonObject["servings"]?.jsonPrimitive?.intOrNull,
                prepTime = jsonObject["prepTime"]?.jsonPrimitive?.contentOrNull,
                cookTime = jsonObject["cookTime"]?.jsonPrimitive?.contentOrNull,
                tags = parseStringArray(jsonObject["tags"]),
                detectedLanguage = jsonObject["detectedLanguage"]?.jsonPrimitive?.contentOrNull
            )

            DebugLogger.i(TAG, "parseRecipeResponse: Parsed recipe - title: ${recipe.titleEnglish}, ingredients: ${recipe.ingredientsEnglish.size}, instructions: ${recipe.instructionsEnglish.size}")
            return recipe
        } catch (e: Exception) {
            DebugLogger.e(TAG, "parseRecipeResponse: Failed to parse JSON: ${e.message}", e)
            DebugLogger.e(TAG, "parseRecipeResponse: JSON content: $recipeJson")
            throw e
        }
    }

    private fun extractJsonFromResponse(text: String): String {
        // Try to find JSON in the response
        val jsonStart = text.indexOf('{')
        val jsonEnd = text.lastIndexOf('}')
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return text.substring(jsonStart, jsonEnd + 1)
        }
        return text
    }

    private fun parseIngredientArray(element: kotlinx.serialization.json.JsonElement?): List<Ingredient> {
        if (element == null) return emptyList()
        return try {
            element.jsonArray.map { item ->
                val obj = item.jsonObject
                Ingredient(
                    id = UUID.randomUUID().toString(),
                    text = obj["text"]?.jsonPrimitive?.content ?: "",
                    amount = obj["amount"]?.jsonPrimitive?.contentOrNull,
                    unit = obj["unit"]?.jsonPrimitive?.contentOrNull,
                    name = obj["name"]?.jsonPrimitive?.content ?: ""
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseStringArray(element: kotlinx.serialization.json.JsonElement?): List<String> {
        if (element == null) return emptyList()
        return try {
            element.jsonArray.map { it.jsonPrimitive.content }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
