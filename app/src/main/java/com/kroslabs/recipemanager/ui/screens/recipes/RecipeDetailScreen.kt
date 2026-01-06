package com.kroslabs.recipemanager.ui.screens.recipes

import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.kroslabs.recipemanager.R
import com.kroslabs.recipemanager.domain.model.Language
import com.kroslabs.recipemanager.domain.model.RecipeTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    // Language selector
                    var showLanguageMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(Icons.Default.Language, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            Language.entries.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(language.displayName) },
                                    onClick = {
                                        viewModel.onLanguageChange(language)
                                        showLanguageMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.language == language) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Share button
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share)) },
                                onClick = {
                                    viewModel.shareRecipe(context)
                                    showShareMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share_ingredients)) },
                                onClick = {
                                    viewModel.shareIngredients(context)
                                    showShareMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                            )
                        }
                    }

                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }

                    IconButton(onClick = { viewModel.onShowDeleteDialog(true) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val recipe = uiState.recipe
            if (recipe == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Recipe not found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    item {
                        Text(
                            text = recipe.getTitle(uiState.language),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    // Rating
                    item {
                        RatingBar(
                            rating = recipe.rating,
                            onRatingChange = viewModel::onRatingChange
                        )
                    }

                    // Metadata
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            recipe.servings?.let { servings ->
                                MetadataChip(
                                    icon = Icons.Default.People,
                                    text = "$servings ${stringResource(R.string.servings)}"
                                )
                            }
                            recipe.prepTime?.let { time ->
                                MetadataChip(
                                    icon = Icons.Default.Timer,
                                    text = time
                                )
                            }
                            recipe.cookTime?.let { time ->
                                MetadataChip(
                                    icon = Icons.Default.LocalFireDepartment,
                                    text = time
                                )
                            }
                        }
                    }

                    // Tags
                    if (recipe.tags.isNotEmpty()) {
                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recipe.tags.forEach { tag ->
                                    RecipeTag.fromKey(tag)?.let { recipeTag ->
                                        AssistChip(
                                            onClick = { },
                                            label = { Text("${recipeTag.emoji} $tag") }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Ingredients
                    item {
                        Text(
                            text = stringResource(R.string.ingredients),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    val ingredients = recipe.getIngredients(uiState.language)
                    items(ingredients.size) { index ->
                        Row(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = ingredients[index].text,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Instructions
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.instructions),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    val instructions = recipe.getInstructions(uiState.language)
                    itemsIndexed(instructions) { index, instruction ->
                        Row(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}. ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = instruction,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Notes
                    val notes = recipe.getNotes(uiState.language)
                    if (!notes.isNullOrBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.notes),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (uiState.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onShowDeleteDialog(false) },
                title = { Text(stringResource(R.string.confirm_delete)) },
                text = { Text(stringResource(R.string.delete_recipe_message)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteRecipe() }) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onShowDeleteDialog(false) }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun RatingBar(
    rating: Int?,
    onRatingChange: (Int?) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (1..5).forEach { star ->
            IconButton(
                onClick = {
                    onRatingChange(if (rating == star) null else star)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if ((rating ?: 0) >= star) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if ((rating ?: 0) >= star)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (rating != null) {
            Text(
                text = "($rating/5)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.unrated),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetadataChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
