package com.kroslabs.recipemanager.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kroslabs.recipemanager.data.local.PreferencesManager
import com.kroslabs.recipemanager.data.repository.MealPlanRepository
import com.kroslabs.recipemanager.data.repository.RecipeRepository
import com.kroslabs.recipemanager.domain.model.Language
import com.kroslabs.recipemanager.domain.model.MealPlan
import com.kroslabs.recipemanager.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val language: Language = Language.ENGLISH,
    val cloudSyncEnabled: Boolean = false,
    val lastSync: Long? = null,
    val isSyncing: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val recipeRepository: RecipeRepository,
    private val mealPlanRepository: MealPlanRepository
) : ViewModel() {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(apiKey = preferencesManager.getApiKey() ?: "") }

        viewModelScope.launch {
            combine(
                preferencesManager.language,
                preferencesManager.cloudSyncEnabled,
                preferencesManager.lastSync
            ) { language, syncEnabled, lastSync ->
                _uiState.update { state ->
                    state.copy(
                        language = language,
                        cloudSyncEnabled = syncEnabled,
                        lastSync = lastSync
                    )
                }
            }.collect()
        }
    }

    fun onApiKeyChange(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey) }
    }

    fun saveApiKey() {
        val apiKey = _uiState.value.apiKey.trim()
        preferencesManager.setApiKey(apiKey.takeIf { it.isNotBlank() })
        _uiState.update { it.copy(message = "API key saved") }
    }

    fun onLanguageChange(language: Language) {
        viewModelScope.launch {
            preferencesManager.setLanguage(language)
            _uiState.update { it.copy(showLanguageDialog = false) }
        }
    }

    fun onCloudSyncToggle(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCloudSyncEnabled(enabled)
        }
    }

    fun onSyncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            // Cloud sync would go here - for now just update timestamp
            preferencesManager.setLastSync(System.currentTimeMillis())
            _uiState.update { it.copy(isSyncing = false, message = "Sync completed") }
        }
    }

    fun onShowLanguageDialog(show: Boolean) {
        _uiState.update { it.copy(showLanguageDialog = show) }
    }

    fun onShowExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    suspend fun exportData(context: Context, uri: Uri, type: ExportType) {
        viewModelScope.launch {
            try {
                val recipes = recipeRepository.getAllRecipes().first()
                val mealPlans = mealPlanRepository.getAllMealPlans().first()

                val exportData = when (type) {
                    ExportType.ALL -> ExportData(
                        type = "complete",
                        recipes = recipes,
                        mealPlans = mealPlans
                    )
                    ExportType.RECIPES -> ExportData(
                        type = "recipe_collection",
                        recipes = recipes
                    )
                    ExportType.MEAL_PLANS -> ExportData(
                        type = "meal_plan",
                        mealPlans = mealPlans
                    )
                }

                val jsonString = json.encodeToString(exportData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                _uiState.update { it.copy(message = "Export successful", showExportDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Export failed: ${e.message}") }
            }
        }
    }

    suspend fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: throw Exception("Failed to read file")

                val importData = json.decodeFromString<ExportData>(jsonString)

                importData.recipes?.let { recipes ->
                    recipeRepository.insertRecipes(recipes)
                }

                importData.mealPlans?.let { mealPlans ->
                    mealPlanRepository.insertMealPlans(mealPlans)
                }

                val recipeCount = importData.recipes?.size ?: 0
                val mealPlanCount = importData.mealPlans?.size ?: 0
                _uiState.update {
                    it.copy(message = "Imported $recipeCount recipes and $mealPlanCount meal plans")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Import failed: ${e.message}") }
            }
        }
    }
}

enum class ExportType {
    ALL,
    RECIPES,
    MEAL_PLANS
}

@Serializable
data class ExportData(
    val version: String = "1.0",
    val exportDate: String = Instant.now().toString(),
    val type: String,
    val recipes: List<Recipe>? = null,
    val mealPlans: List<MealPlan>? = null
)
