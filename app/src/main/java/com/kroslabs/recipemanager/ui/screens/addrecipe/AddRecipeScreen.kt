package com.kroslabs.recipemanager.ui.screens.addrecipe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.kroslabs.recipemanager.R
import com.kroslabs.recipemanager.domain.model.Language
import com.kroslabs.recipemanager.domain.model.RecipeTag
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(
    recipeId: String? = null,
    onNavigateBack: () -> Unit,
    onRecipeSaved: (String) -> Unit,
    viewModel: AddRecipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var newIngredient by remember { mutableStateOf("") }
    var newInstruction by remember { mutableStateOf("") }

    LaunchedEffect(uiState.savedRecipeId) {
        uiState.savedRecipeId?.let { id ->
            onRecipeSaved(id)
        }
    }

    // Camera launcher
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                viewModel.extractFromImage(context, uri)
            }
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.extractFromImage(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode)
                            stringResource(R.string.edit)
                        else
                            stringResource(R.string.add_recipe_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (uiState.editingRecipe != null) {
                        TextButton(onClick = { viewModel.saveRecipe() }) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isExtracting -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.extracting_recipe))
                    }
                }

                uiState.editingRecipe != null -> {
                    // Recipe editor
                    val recipe = uiState.editingRecipe!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        item {
                            OutlinedTextField(
                                value = recipe.getTitle(uiState.language),
                                onValueChange = { viewModel.updateTitle(uiState.language, it) },
                                label = { Text(stringResource(R.string.recipe_title)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // Language tabs
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Language.entries.forEach { language ->
                                    FilterChip(
                                        selected = uiState.language == language,
                                        onClick = { /* Language change handled at ViewModel level */ },
                                        label = { Text(language.displayName) }
                                    )
                                }
                            }
                        }

                        // Metadata row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = recipe.servings?.toString() ?: "",
                                    onValueChange = {
                                        viewModel.updateServings(it.toIntOrNull())
                                    },
                                    label = { Text(stringResource(R.string.servings)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = recipe.prepTime ?: "",
                                    onValueChange = { viewModel.updatePrepTime(it) },
                                    label = { Text(stringResource(R.string.prep_time)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = recipe.cookTime ?: "",
                                    onValueChange = { viewModel.updateCookTime(it) },
                                    label = { Text(stringResource(R.string.cook_time)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        // Ingredients
                        item {
                            Text(
                                text = stringResource(R.string.ingredients),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        val ingredients = recipe.getIngredients(uiState.language)
                        itemsIndexed(ingredients) { _, ingredient ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${ingredient.text}",
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeIngredient(uiState.language, ingredient.id) }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newIngredient,
                                    onValueChange = { newIngredient = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(stringResource(R.string.add_ingredient)) },
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        if (newIngredient.isNotBlank()) {
                                            viewModel.addIngredient(uiState.language, newIngredient)
                                            newIngredient = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            }
                        }

                        // Instructions
                        item {
                            Text(
                                text = stringResource(R.string.instructions),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        val instructions = recipe.getInstructions(uiState.language)
                        itemsIndexed(instructions) { index, instruction ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. $instruction",
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeInstruction(uiState.language, index) }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newInstruction,
                                    onValueChange = { newInstruction = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(stringResource(R.string.add_instruction)) },
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        if (newInstruction.isNotBlank()) {
                                            viewModel.addInstruction(uiState.language, newInstruction)
                                            newInstruction = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            }
                        }

                        // Tags
                        item {
                            Text(
                                text = stringResource(R.string.select_tags),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RecipeTag.entries.forEach { tag ->
                                    FilterChip(
                                        selected = recipe.tags.contains(tag.key),
                                        onClick = { viewModel.toggleTag(tag.key) },
                                        label = { Text("${tag.emoji} ${tag.key}") }
                                    )
                                }
                            }
                        }

                        // Notes
                        item {
                            OutlinedTextField(
                                value = recipe.getNotes(uiState.language) ?: "",
                                onValueChange = { viewModel.updateNotes(uiState.language, it) },
                                label = { Text(stringResource(R.string.notes)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    }
                }

                uiState.inputMode != null -> {
                    // Input mode UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (uiState.inputMode) {
                            InputMode.TEXT -> {
                                OutlinedTextField(
                                    value = uiState.textInput,
                                    onValueChange = viewModel::onTextInputChange,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    placeholder = { Text(stringResource(R.string.paste_recipe_text)) }
                                )
                                Button(
                                    onClick = { viewModel.extractFromText() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.extract))
                                }
                            }

                            InputMode.URL -> {
                                OutlinedTextField(
                                    value = uiState.urlInput,
                                    onValueChange = viewModel::onUrlInputChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(stringResource(R.string.enter_url)) },
                                    singleLine = true
                                )
                                Button(
                                    onClick = { viewModel.extractFromUrl() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.extract))
                                }
                            }

                            InputMode.YOUTUBE -> {
                                OutlinedTextField(
                                    value = uiState.urlInput,
                                    onValueChange = viewModel::onUrlInputChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(stringResource(R.string.enter_youtube_url)) },
                                    singleLine = true
                                )
                                Button(
                                    onClick = { viewModel.extractFromUrl() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.extract))
                                }
                            }

                            else -> { }
                        }
                    }
                }

                else -> {
                    // Input mode selection
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InputModeCard(
                            icon = Icons.Default.CameraAlt,
                            title = stringResource(R.string.from_camera),
                            onClick = {
                                val photoFile = File.createTempFile(
                                    "recipe_",
                                    ".jpg",
                                    context.cacheDir
                                )
                                cameraImageUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                cameraLauncher.launch(cameraImageUri!!)
                            }
                        )

                        InputModeCard(
                            icon = Icons.Default.PhotoLibrary,
                            title = stringResource(R.string.from_photos),
                            onClick = {
                                photoPickerLauncher.launch("image/*")
                            }
                        )

                        InputModeCard(
                            icon = Icons.Default.TextFields,
                            title = stringResource(R.string.from_text),
                            onClick = { viewModel.onInputModeSelect(InputMode.TEXT) }
                        )

                        InputModeCard(
                            icon = Icons.Default.Link,
                            title = stringResource(R.string.from_url),
                            onClick = { viewModel.onInputModeSelect(InputMode.URL) }
                        )

                        InputModeCard(
                            icon = Icons.Default.OndemandVideo,
                            title = stringResource(R.string.from_youtube),
                            onClick = { viewModel.onInputModeSelect(InputMode.YOUTUBE) }
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        TextButton(
                            onClick = { viewModel.createManualRecipe() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create manually")
                        }
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
private fun InputModeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
