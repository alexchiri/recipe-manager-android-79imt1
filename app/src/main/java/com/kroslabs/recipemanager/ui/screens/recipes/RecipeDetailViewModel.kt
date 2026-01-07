package com.kroslabs.recipemanager.ui.screens.recipes

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kroslabs.recipemanager.data.local.PreferencesManager
import com.kroslabs.recipemanager.data.repository.RecipeRepository
import com.kroslabs.recipemanager.domain.model.Language
import com.kroslabs.recipemanager.domain.model.Recipe
import com.kroslabs.recipemanager.domain.model.RecipeTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val language: Language = Language.ENGLISH,
    val isLoading: Boolean = true,
    val showDeleteDialog: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val recipeId: String = savedStateHandle["recipeId"] ?: ""

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.language.collect { language ->
                _uiState.update { it.copy(language = language) }
            }
        }
        loadRecipe()
    }

    private fun loadRecipe() {
        viewModelScope.launch {
            val recipe = recipeRepository.getRecipeById(recipeId)
            _uiState.update {
                it.copy(recipe = recipe, isLoading = false)
            }
        }
    }

    fun onLanguageChange(language: Language) {
        _uiState.update { it.copy(language = language) }
    }

    fun onRatingChange(rating: Int?) {
        viewModelScope.launch {
            val recipe = _uiState.value.recipe ?: return@launch
            val updated = recipe.copy(rating = rating)
            recipeRepository.updateRecipe(updated)
            _uiState.update { it.copy(recipe = updated) }
        }
    }

    fun onShowDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteDialog = show) }
    }

    fun deleteRecipe() {
        viewModelScope.launch {
            val recipe = _uiState.value.recipe ?: return@launch
            recipeRepository.deleteRecipe(recipe)
            _uiState.update { it.copy(isDeleted = true, showDeleteDialog = false) }
        }
    }

    fun shareRecipe(context: Context) {
        val recipe = _uiState.value.recipe ?: return
        val language = _uiState.value.language

        val text = buildString {
            appendLine(recipe.getTitle(language))
            appendLine()

            val metadata = mutableListOf<String>()
            recipe.servings?.let { metadata.add("Servings: $it") }
            recipe.prepTime?.let { metadata.add("Prep: $it") }
            recipe.cookTime?.let { metadata.add("Cook: $it") }
            if (metadata.isNotEmpty()) {
                appendLine(metadata.joinToString(" | "))
            }

            recipe.rating?.let {
                val stars = "★".repeat(it) + "☆".repeat(5 - it)
                appendLine("Rating: $stars ($it/5)")
            }
            appendLine()

            if (recipe.tags.isNotEmpty()) {
                val tagDisplay = recipe.tags.mapNotNull { tag ->
                    RecipeTag.fromKey(tag)?.let { "${it.emoji} $tag" }
                }.joinToString(" ")
                appendLine("Tags: $tagDisplay")
                appendLine()
            }

            appendLine("INGREDIENTS:")
            recipe.getIngredients(language).forEach { ingredient ->
                appendLine("- ${ingredient.text}")
            }
            appendLine()

            appendLine("INSTRUCTIONS:")
            recipe.getInstructions(language).forEachIndexed { index, instruction ->
                appendLine("${index + 1}. $instruction")
            }

            recipe.getNotes(language)?.let { notes ->
                if (notes.isNotBlank()) {
                    appendLine()
                    appendLine("NOTES:")
                    appendLine(notes)
                }
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Recipe"))
    }

    fun shareIngredients(context: Context) {
        val recipe = _uiState.value.recipe ?: return
        val language = _uiState.value.language

        val items = recipe.getIngredients(language).map { ingredient ->
            QuickyShoppyItem(
                name = ingredient.name,
                quantity = listOfNotNull(ingredient.amount, ingredient.unit).joinToString(" ").takeIf { it.isNotBlank() }
            )
        }

        val shoppyData = QuickyShoppyData(items = items)
        val json = Json.encodeToString(shoppyData)
        val base64 = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
        val url = "quickyshoppy://import?data=$base64"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(url)
        }
        context.startActivity(intent)
    }

    @Serializable
    private data class QuickyShoppyData(
        val version: String = "1.0",
        val items: List<QuickyShoppyItem>
    )

    @Serializable
    private data class QuickyShoppyItem(
        val name: String,
        val quantity: String? = null
    )
}
