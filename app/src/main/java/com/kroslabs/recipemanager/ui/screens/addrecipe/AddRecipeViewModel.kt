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

    private val recipeId: String? = savedStateHandle["recipeId"]

    private val _uiState = MutableStateFlow(AddRecipeUiState())
    val uiState: StateFlow<AddRecipeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.language.collect { language ->
                _uiState.update { it.copy(language = language) }
            }
        }

        if (recipeId != null) {
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
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "Please enter recipe text") }
            return
        }

        val apiKey = preferencesManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Please add your Claude API key in Settings") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExtracting = true, error = null) }

            recipeExtractor.extractFromText(text, apiKey)
                .onSuccess { recipe ->
                    _uiState.update {
                        it.copy(
                            extractedRecipe = recipe,
                            editingRecipe = recipe,
                            isExtracting = false
                        )
                    }
                }
                .onFailure { e ->
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
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a URL") }
            return
        }

        val apiKey = preferencesManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Please add your Claude API key in Settings") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExtracting = true, error = null) }

            try {
                val htmlContent = fetchUrl(url)
                recipeExtractor.extractFromUrl(url, htmlContent, apiKey)
                    .onSuccess { recipe ->
                        _uiState.update {
                            it.copy(
                                extractedRecipe = recipe,
                                editingRecipe = recipe,
                                isExtracting = false
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                error = e.message ?: "Failed to extract recipe",
                                isExtracting = false
                            )
                        }
                    }
            } catch (e: Exception) {
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
        val apiKey = preferencesManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Please add your Claude API key in Settings") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExtracting = true, error = null) }

            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Failed to read image")
                inputStream.close()

                val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"

                recipeExtractor.extractFromImage(bytes, mimeType, apiKey)
                    .onSuccess { recipe ->
                        _uiState.update {
                            it.copy(
                                extractedRecipe = recipe,
                                editingRecipe = recipe,
                                isExtracting = false
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                error = e.message ?: "Failed to extract recipe",
                                isExtracting = false
                            )
                        }
                    }
            } catch (e: Exception) {
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
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val content = reader.readText()
        reader.close()
        connection.disconnect()
        content
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
        val recipe = _uiState.value.editingRecipe
        if (recipe == null) {
            _uiState.update { it.copy(error = "No recipe to save") }
            return
        }

        if (recipe.titleEnglish.isBlank()) {
            _uiState.update { it.copy(error = "Recipe title is required") }
            return
        }

        if (recipe.ingredientsEnglish.isEmpty()) {
            _uiState.update { it.copy(error = "At least one ingredient is required") }
            return
        }

        if (recipe.instructionsEnglish.isEmpty()) {
            _uiState.update { it.copy(error = "At least one instruction is required") }
            return
        }

        viewModelScope.launch {
            if (_uiState.value.isEditMode) {
                recipeRepository.updateRecipe(recipe)
            } else {
                recipeRepository.insertRecipe(recipe)
            }
            _uiState.update { it.copy(savedRecipeId = recipe.id) }
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
