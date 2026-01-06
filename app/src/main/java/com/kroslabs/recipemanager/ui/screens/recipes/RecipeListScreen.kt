package com.kroslabs.recipemanager.ui.screens.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kroslabs.recipemanager.R
import com.kroslabs.recipemanager.domain.model.Recipe
import com.kroslabs.recipemanager.domain.model.RecipeTag
import com.kroslabs.recipemanager.domain.model.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    onRecipeClick: (String) -> Unit,
    onAddRecipeClick: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_recipes)) },
                actions = {
                    IconButton(onClick = { viewModel.onShowFilterDialog(true) }) {
                        Badge(
                            modifier = Modifier.offset(x = 8.dp, y = (-8).dp),
                            containerColor = if (uiState.selectedTags.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        ) {
                            if (uiState.selectedTags.isNotEmpty()) {
                                Text(uiState.selectedTags.size.toString())
                            }
                        }
                        Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter))
                    }
                    IconButton(onClick = { viewModel.onShowSortDialog(true) }) {
                        Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.sort))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecipeClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_recipe))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_recipes)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredRecipes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.recipes.isEmpty())
                            stringResource(R.string.no_recipes)
                        else
                            stringResource(R.string.no_recipes_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredRecipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            language = uiState.language,
                            onClick = { onRecipeClick(recipe.id) }
                        )
                    }
                }
            }
        }

        // Filter Dialog
        if (uiState.showFilterDialog) {
            FilterDialog(
                selectedTags = uiState.selectedTags,
                onTagToggle = viewModel::onTagToggle,
                onClearFilters = viewModel::onClearFilters,
                onDismiss = { viewModel.onShowFilterDialog(false) }
            )
        }

        // Sort Dialog
        if (uiState.showSortDialog) {
            SortDialog(
                currentSort = uiState.sortOption,
                onSortSelect = viewModel::onSortOptionChange,
                onDismiss = { viewModel.onShowSortDialog(false) }
            )
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: Recipe,
    language: com.kroslabs.recipemanager.domain.model.Language,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = recipe.getTitle(language),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                recipe.rating?.let { rating ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$rating/5",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                recipe.prepTime?.let { time ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = time,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                recipe.servings?.let { servings ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = servings.toString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (recipe.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recipe.tags.take(4).forEach { tag ->
                        RecipeTag.fromKey(tag)?.let { recipeTag ->
                            Text(
                                text = recipeTag.emoji,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (recipe.tags.size > 4) {
                        Text(
                            text = "+${recipe.tags.size - 4}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter)) },
        text = {
            LazyColumn {
                item {
                    FilterCategory(
                        title = stringResource(R.string.category_dietary),
                        tags = RecipeTag.dietaryRestrictions,
                        selectedTags = selectedTags,
                        onTagToggle = onTagToggle
                    )
                }
                item {
                    FilterCategory(
                        title = stringResource(R.string.category_diet_styles),
                        tags = RecipeTag.dietStyles,
                        selectedTags = selectedTags,
                        onTagToggle = onTagToggle
                    )
                }
                item {
                    FilterCategory(
                        title = stringResource(R.string.category_meal_types),
                        tags = RecipeTag.mealTypes,
                        selectedTags = selectedTags,
                        onTagToggle = onTagToggle
                    )
                }
                item {
                    FilterCategory(
                        title = stringResource(R.string.category_protein),
                        tags = RecipeTag.proteinTypes,
                        selectedTags = selectedTags,
                        onTagToggle = onTagToggle
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            if (selectedTags.isNotEmpty()) {
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.clear_filters))
                }
            }
        }
    )
}

@Composable
private fun FilterCategory(
    title: String,
    tags: List<RecipeTag>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = selectedTags.contains(tag.key),
                    onClick = { onTagToggle(tag.key) },
                    label = { Text("${tag.emoji} ${tag.key}") }
                )
            }
        }
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

@Composable
private fun SortDialog(
    currentSort: SortOption,
    onSortSelect: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort)) },
        text = {
            Column {
                SortOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == option,
                            onClick = { onSortSelect(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (option) {
                                SortOption.DATE_ADDED -> stringResource(R.string.sort_by_date)
                                SortOption.RATING -> stringResource(R.string.sort_by_rating)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    )
}
