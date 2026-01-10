package com.kroslabs.recipemanager.ui.screens.addrecipe

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kroslabs.recipemanager.data.local.PreferencesManager
import com.kroslabs.recipemanager.data.remote.RecipeExtractor
import com.kroslabs.recipemanager.data.repository.RecipeRepository
import com.kroslabs.recipemanager.domain.model.Ingredient
import com.kroslabs.recipemanager.domain.model.Language
import com.kroslabs.recipemanager.domain.model.Recipe
import com.kroslabs.recipemanager.util.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject

enum class InputMode {
    CAMERA,
    PHOTOS,
    TEXT,
    URL,
    YOUTUBE
}

data class AddRecipeUiState(
    val inputMode: InputMode? = null,
    val textInput: String = "",
    val urlInput: String = "",
    val isExtracting: Boolean = false,
    val extractedRecipe: Recipe? = null,
    val editingRecipe: Recipe? = null,
    val language: Language = Language.ENGLISH,
    val error: String? = null,
    val savedRecipeId: String? = null,
    val isEditMode: Boolean = false
)

@HiltViewModel
class AddRecipeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val recipeExtractor: RecipeExtractor,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "AddRecipeViewModel"
    }

    private val recipeId: String? = savedStateHandle["recipeId"]

    private val _uiState = MutableStateFlow(AddRecipeUiState())
    val uiState: StateFlow<AddRecipeUiState> = _uiState.asStateFlow()

    init {
        DebugLogger.d(TAG, "init: recipeId=$recipeId")
        viewModelScope.launch {
            preferencesManager.language.collect { language ->
                DebugLogger.d(TAG, "Language changed to: $language")
                _uiState.update { it.copy(language = language) }
            }
        }

        if (recipeId != null) {
            DebugLogger.i(TAG, "Loading recipe for edit: $recipeId")
            loadRecipeForEdit(recipeId)
        }
    }

    private fun loadRecipeForEdit(id: String) {
        viewModelScope.launch {
            val recipe = recipeRepository.getRecipeById(id)
            if (recipe != null) {
                _uiState.update {
                    it.copy(
                        editingRecipe = recipe,
                        extractedRecipe = recipe,
                        isEditMode = true
                    )
                }
            }
        }
    }

    fun onInputModeSelect(mode: InputMode) {
        _uiState.update { it.copy(inputMode = mode, error = null) }
    }

    fun onTextInputChange(text: String) {
        _uiState.update { it.copy(textInput = text) }
    }

    fun onUrlInputChange(url: String) {
        _uiState.update { it.copy(urlInput = url) }
    }

    fun extractFromText() {
        val text = _uiState.value.textInput
        DebugLogger.i(TAG, "extractFromText: Starting extraction, text length: ${text.length}")

        if (text.isBlank()) {
            DebugLogger.w(TAG, "extractFromText: Text is blank")
            _uiState.update { it.copy(error = "Please enter recipe text") }
            return
        }

        val apiKey = preferencesManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            DebugLogger.w(TAG, "extractFromText: No API key configured")
            _uiState.update { it.copy(error = "Please add your Claude API key in Settings") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExtracting = true, error = null) }

            DebugLogger.d(TAG, "extractFromText: Calling RecipeExtractor...")
            recipeExtractor.extractFromText(text, apiKey)
                .onSuccess { recipe ->
                    DebugLogger.i(TAG, "extractFromText: Successfully extracted recipe: ${recipe.titleEnglish}")
                    _uiState.update {
                        it.copy(
                            extractedRecipe = recipe,
                            editingRecipe = recipe,
                            isExtracting = false
                        )
                    }
                }
                .onFailure { e ->
                    DebugLogger.e(TAG, "extractFromText: Failed: ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Failed to extract recipe",
                            isExtracting = false
                        )
                    }
                }
        }
    }

    fun extractFromUrl() {
        val url = _uiState.value.urlInput
        DebugLogger.i(TAG, "extractFromUrl: Starting extraction for URL: $url")

        if (url.isBlank()) {
            DebugLogger.w(TAG, "extractFromUrl: URL is blank")
            _uiState.update { it.copy(error = "Please enter a URL") }
            return
        }

        val apiKey = preferencesManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            DebugLogger.w(TAG, "extractFromUrl: No API key configured")
            _uiState.update { it.copy(error = "Please add your Claude API key in Settings") }
            return
        }

        DebugLogger.d(TAG, "extractFromUrl: API key present, starting fetch")

        viewModelScope.launch {
            _uiState.update { it.copy(isExtracting = true, error = null) }

            try {
                DebugLogger.d(TAG, "extractFromUrl: Fetching URL content...")
                val htmlContent = fetchUrl(url)
                DebugLogger.i(TAG, "extractFromUrl: Fetched ${htmlContent.length} characters from URL")
                DebugLogger.d(TAG, "extractFromUrl: First 500 chars: ${htmlContent.take(500)}")

                DebugLogger.d(TAG, "extractFromUrl: Sending to RecipeExtractor...")
                recipeExtractor.extractFromUrl(url, htmlContent, apiKey)
                    .onSuccess { recipe ->
                        DebugLogger.i(TAG, "extractFromUrl: Successfully extracted recipe: ${recipe.titleEnglish}")
                        _uiState.update {
                            it.copy(
                                extractedRecipe = recipe,
                                editingRecipe = recipe,
                                isExtracting = false
                            )
                        }
                    }
                    .onFailure { e ->
                        DebugLogger.e(TAG, "extractFromUrl: RecipeExtractor failed: ${e.message}", e)
                        _uiState.update {
                            it.copy(
                                error = e.message ?: "Failed to extract recipe",
                                isExtracting = false
                            )
                        }
                    }
            } catch (e: Exception) {
                DebugLogger.e(TAG, "extractFromUrl: Failed to fetch URL: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Failed to fetch URL: ${e.message}",
                        isExtracting = false
                    )
                }
            }
        }
    }

    fun extractFromImage(context: Context, imageUri: Uri) {
        DebugLogger.i(TAG, "extractFromImage: Starting extraction for URI: $imageUri")

        val apiKey = preferencesManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            DebugLogger.w(TAG, "extractFromImage: No API key configured")
            _uiState.update { it.copy(error = "Please add your Claude API key in Settings") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExtracting = true, error = null) }

            try {
                DebugLogger.d(TAG, "extractFromImage: Reading image bytes...")
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Failed to read image")
                inputStream.close()

                val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                DebugLogger.d(TAG, "extractFromImage: Image size: ${bytes.size} bytes, mimeType: $mimeType")

                DebugLogger.d(TAG, "extractFromImage: Calling RecipeExtractor...")
                recipeExtractor.extractFromImage(bytes, mimeType, apiKey)
                    .onSuccess { recipe ->
                        DebugLogger.i(TAG, "extractFromImage: Successfully extracted recipe: ${recipe.titleEnglish}")
                        _uiState.update {
                            it.copy(
                                extractedRecipe = recipe,
                                editingRecipe = recipe,
                                isExtracting = false
                            )
                        }
                    }
                    .onFailure { e ->
                        DebugLogger.e(TAG, "extractFromImage: Failed: ${e.message}", e)
                        _uiState.update {
                            it.copy(
                                error = e.message ?: "Failed to extract recipe",
                                isExtracting = false
                            )
                        }
                    }
            } catch (e: Exception) {
                DebugLogger.e(TAG, "extractFromImage: Failed to process image: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Failed to process image: ${e.message}",
                        isExtracting = false
                    )
                }
            }
        }
    }

    private suspend fun fetchUrl(urlString: String): String = withContext(Dispatchers.IO) {
        DebugLogger.d(TAG, "fetchUrl: Starting fetch for: $urlString")

        var currentUrl = urlString
        var redirectCount = 0
        val maxRedirects = 5

        while (redirectCount < maxRedirects) {
            val url = URL(currentUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false

            // Set comprehensive headers to avoid HTTP 400 errors
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("Connection", "keep-alive")
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1")

            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            DebugLogger.d(TAG, "fetchUrl: Connecting to: $currentUrl")

            try {
                connection.connect()
                val responseCode = connection.responseCode
                DebugLogger.d(TAG, "fetchUrl: Response code: $responseCode")

                when (responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val contentType = connection.contentType ?: ""
                        DebugLogger.d(TAG, "fetchUrl: Content-Type: $contentType")

                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                        val content = reader.readText()
                        reader.close()
                        connection.disconnect()

                        DebugLogger.i(TAG, "fetchUrl: Successfully fetched ${content.length} characters")
                        return@withContext content
                    }
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307, 308 -> {
                        val location = connection.getHeaderField("Location")
                        connection.disconnect()

                        if (location.isNullOrBlank()) {
                            DebugLogger.e(TAG, "fetchUrl: Redirect without Location header")
                            throw Exception("Redirect without Location header")
                        }

                        currentUrl = if (location.startsWith("http")) {
                            location
                        } else {
                            URL(url, location).toString()
                        }
                        DebugLogger.d(TAG, "fetchUrl: Redirecting to: $currentUrl")
                        redirectCount++
                    }
                    else -> {
                        val errorStream = connection.errorStream
                        val errorBody = errorStream?.bufferedReader()?.readText() ?: "No error body"
                        connection.disconnect()

                        DebugLogger.e(TAG, "fetchUrl: HTTP error $responseCode: $errorBody")
                        throw Exception("HTTP error $responseCode: ${connection.responseMessage}")
                    }
                }
            } catch (e: Exception) {
                connection.disconnect()
                DebugLogger.e(TAG, "fetchUrl: Exception during fetch: ${e.message}", e)
                throw e
            }
        }

        DebugLogger.e(TAG, "fetchUrl: Too many redirects")
        throw Exception("Too many redirects")
    }

    fun updateTitle(language: Language, title: String) {
        _uiState.update { state ->
            val recipe = state.editingRecipe ?: return@update state
            val updated = when (language) {
                Language.ENGLISH -> recipe.copy(titleEnglish = title)
                Language.SWEDISH -> recipe.copy(titleSwedish = title)
                Language.ROMANIAN -> recipe.copy(titleRomanian = title)
            }
            state.copy(editingRecipe = updated)
        }
    }

    fun updateServings(servings: Int?) {
        _uiState.update { state ->
            val recipe = state.editingRecipe ?: return@update state
            state.copy(editingRecipe = recipe.copy(servings = servings))
        }
    }

    fun updatePrepTime(time: String?) {
        _uiState.update { state ->
            val recipe = state.editingRecipe ?: return@update state
            state.copy(editingRecipe = recipe.copy(prepTime = time?.takeIf { it.isNotBlank() }))
        }
    }

    fun updateCookTime(time: String?) {
        _uiState.update { state ->
            val recipe = state.editingRecipe ?: return@update state
            state.copy(editingRecipe = recipe.copy(cookTime = time?.takeIf { it.isNotBlank() }))
        }
    }

    fun addIngredient(language: Language, text: String) {
        if (text.isBlank()) return
        _uiState.update { state ->
            val recipe = state.editingRecipe ?: return@update state
            val ingredient = Ingredient(
                id = UUID.randomUUID().toString(),
                text = text,
                name = text
            )
            val updated = when (language) {
                Language.ENGLISH -> recipe.copy(ingredientsEnglish = recipe.ingredientsEnglish + ingredient)
                Language.SWEDISH -> recipe.copy(ingredientsSwedish = recipe.ingredientsSwedish + ingredient)
                Language.ROMANIAN -> recipe.copy(ingredientsRomanian = recipe.ingredientsRomanian + ingredient)
            }
            state.copy(editingRecipe = updated)
        }
    }

    fun removeIngredient(language: Language, ingredientId: String) {
        _uiState.update { state ->
            val recipe = state.editingRecipe ?: return@update state
            val updated = when (language) {
                Language.ENGLISH -> recipe.copy(
                    ingredientsEnglish = recipe.ingredientsEnglish.filter { it.id != ingredientId }
                )
                Language.SWEDISH -> recipe.copy(
                    ingredientsSwedish = recipe.ingredientsSwedish.filter { it.id != ingredientId }
                )
                Language.ROMANIAN -> recipe.copy(
                    ingredientsRomanian = recipe.ingredientsRomanian.filter { it.id != ingredientId }
                )
            }
            state.copy(editingRecipe = updated)
        }
    }

    fun addInstruction(language: Language, text: String) {
        if (text.isBlank()) return
        _uiState.update { state ->
            val recipe = state.editingRecipe ?: return@update state
            val updated = when (language) {
                Language.ENGLISH -> recipe.copy(instructionsEnglish = recipe.instructionsEnglish + text)
                Language.SWEDISH -> recipe.copy(instructionsSwedish = recipe.instructionsSwedish + text)
                Language.ROMANIAN -> recipe.copy(instructionsRomanian = recipe.instructionsRomanian + text)
            }
            state.copy(editingRecipe = updated)
        }
    }

    fun removeInstruction(language: Language, index: Int) {
        _uiState.update { state ->
            val recipe = state.editingRecipe ?: return@update state
            val updated = when (language) {
                Language.ENGLISH -> recipe.copy(
                    instructionsEnglish = recipe.instructionsEnglish.filterIndexed { i, _ -> i != index }
                )
                Language.SWEDISH -> recipe.copy(
                    instructionsSwedish = recipe.instructionsSwedish.filterIndexed { i, _ -> i != index }
                )
                Language.ROMANIAN -> recipe.copy(
                    instructionsRomanian = recipe.instructionsRomanian.filterIndexed { i, _ -> i != index }
                )
            }
            state.copy(editingRecipe = updated)
        }
    }

    fun toggleTag(tag: String) {
        _uiState.update { state ->
            val recipe = state.editingRecipe ?: return@update state
            val newTags = if (recipe.tags.contains(tag)) {
                recipe.tags - tag
            } else {
                recipe.tags + tag
            }
            state.copy(editingRecipe = recipe.copy(tags = newTags))
        }
    }

    fun updateNotes(language: Language, notes: String) {
        _uiState.update { state ->
            val recipe = state.editingRecipe ?: return@update state
            val notesValue = notes.takeIf { it.isNotBlank() }
            val updated = when (language) {
                Language.ENGLISH -> recipe.copy(notesEnglish = notesValue)
                Language.SWEDISH -> recipe.copy(notesSwedish = notesValue)
                Language.ROMANIAN -> recipe.copy(notesRomanian = notesValue)
            }
            state.copy(editingRecipe = updated)
        }
    }

    fun saveRecipe() {
        DebugLogger.i(TAG, "saveRecipe: Starting save operation")

        val recipe = _uiState.value.editingRecipe
        if (recipe == null) {
            DebugLogger.w(TAG, "saveRecipe: No recipe to save")
            _uiState.update { it.copy(error = "No recipe to save") }
            return
        }

        DebugLogger.d(TAG, "saveRecipe: Recipe ID: ${recipe.id}, title: ${recipe.titleEnglish}")

        if (recipe.titleEnglish.isBlank()) {
            DebugLogger.w(TAG, "saveRecipe: Title is blank")
            _uiState.update { it.copy(error = "Recipe title is required") }
            return
        }

        if (recipe.ingredientsEnglish.isEmpty()) {
            DebugLogger.w(TAG, "saveRecipe: No ingredients")
            _uiState.update { it.copy(error = "At least one ingredient is required") }
            return
        }

        if (recipe.instructionsEnglish.isEmpty()) {
            DebugLogger.w(TAG, "saveRecipe: No instructions")
            _uiState.update { it.copy(error = "At least one instruction is required") }
            return
        }

        viewModelScope.launch {
            try {
                if (_uiState.value.isEditMode) {
                    DebugLogger.d(TAG, "saveRecipe: Updating existing recipe")
                    recipeRepository.updateRecipe(recipe)
                } else {
                    DebugLogger.d(TAG, "saveRecipe: Inserting new recipe")
                    recipeRepository.insertRecipe(recipe)
                }
                DebugLogger.i(TAG, "saveRecipe: Recipe saved successfully")
                _uiState.update { it.copy(savedRecipeId = recipe.id) }
            } catch (e: Exception) {
                DebugLogger.e(TAG, "saveRecipe: Failed to save recipe: ${e.message}", e)
                _uiState.update { it.copy(error = "Failed to save recipe: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun createManualRecipe() {
        val newRecipe = Recipe(
            id = UUID.randomUUID().toString(),
            titleEnglish = "",
            titleSwedish = "",
            titleRomanian = ""
        )
        _uiState.update {
            it.copy(
                extractedRecipe = newRecipe,
                editingRecipe = newRecipe
            )
        }
    }
}
