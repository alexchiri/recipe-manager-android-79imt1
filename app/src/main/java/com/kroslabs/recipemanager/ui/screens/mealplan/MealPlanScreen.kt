package com.kroslabs.recipemanager.ui.screens.mealplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kroslabs.recipemanager.R
import com.kroslabs.recipemanager.domain.model.MealType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    onRecipeClick: (String) -> Unit,
    viewModel: MealPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val dateRange = viewModel.getDateRange()
    val dates = remember(dateRange) {
        generateSequence(dateRange.first) { it.plusDays(1) }
            .takeWhile { !it.isAfter(dateRange.second) }
            .toList()
    }

    val today = LocalDate.now()
    val todayIndex = dates.indexOf(today).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = todayIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meal_plan_title)) },
                actions = {
                    IconButton(onClick = { viewModel.onShowShareDialog(true) }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dates, key = { it.toString() }) { date ->
                        DayCard(
                            date = date,
                            mealPlan = uiState.mealPlans[date],
                            language = uiState.language,
                            recipes = uiState.recipes.associateBy { it.id },
                            onLunchClick = { viewModel.onSlotClick(date, MealType.LUNCH) },
                            onDinnerClick = { viewModel.onSlotClick(date, MealType.DINNER) },
                            onSwapMeals = { viewModel.onSwapMeals(date) },
                            onRemoveLunch = { viewModel.onRemoveMeal(date, MealType.LUNCH) },
                            onRemoveDinner = { viewModel.onRemoveMeal(date, MealType.DINNER) },
                            onRecipeClick = onRecipeClick
                        )
                    }
                }
            }
        }

        // Recipe selector dialog
        if (uiState.showRecipeSelector) {
            RecipeSelectorDialog(
                recipes = viewModel.getFilteredRecipes(),
                language = uiState.language,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onRecipeSelect = viewModel::onRecipeSelect,
                onDismiss = { viewModel.onDismissRecipeSelector() }
            )
        }

        // Share dialog
        if (uiState.showShareDialog) {
            ShareMealPlanDialog(
                onShare = { startDate, endDate, includeShoppingList ->
                    viewModel.shareMealPlan(context, startDate, endDate, includeShoppingList)
                },
                onDismiss = { viewModel.onShowShareDialog(false) }
            )
        }
    }
}

@Composable
private fun DayCard(
    date: LocalDate,
    mealPlan: com.kroslabs.recipemanager.domain.model.MealPlan?,
    language: com.kroslabs.recipemanager.domain.model.Language,
    recipes: Map<String, com.kroslabs.recipemanager.domain.model.Recipe>,
    onLunchClick: () -> Unit,
    onDinnerClick: () -> Unit,
    onSwapMeals: () -> Unit,
    onRemoveLunch: () -> Unit,
    onRemoveDinner: () -> Unit,
    onRecipeClick: (String) -> Unit
) {
    val today = LocalDate.now()
    val isToday = date == today
    val isTomorrow = date == today.plusDays(1)
    val isYesterday = date == today.minusDays(1)

    val locale = when (language) {
        com.kroslabs.recipemanager.domain.model.Language.ENGLISH -> Locale.ENGLISH
        com.kroslabs.recipemanager.domain.model.Language.SWEDISH -> Locale("sv")
        com.kroslabs.recipemanager.domain.model.Language.ROMANIAN -> Locale("ro")
    }

    val dateText = when {
        isToday -> stringResource(R.string.today)
        isTomorrow -> stringResource(R.string.tomorrow)
        isYesterday -> stringResource(R.string.yesterday)
        else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    }

    val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isToday) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = date.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val hasLunch = mealPlan?.lunchSlot?.recipeId != null
                val hasDinner = mealPlan?.dinnerSlot?.recipeId != null
                if (hasLunch && hasDinner) {
                    IconButton(onClick = onSwapMeals) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = stringResource(R.string.swap_meals)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lunch slot
            MealSlotRow(
                emoji = "\uD83E\uDD57",
                label = stringResource(R.string.lunch),
                recipeName = mealPlan?.lunchSlot?.recipeId?.let { id ->
                    recipes[id]?.getTitle(language)
                } ?: mealPlan?.lunchSlot?.recipeName,
                onClick = {
                    val recipeId = mealPlan?.lunchSlot?.recipeId
                    if (recipeId != null) {
                        onRecipeClick(recipeId)
                    } else {
                        onLunchClick()
                    }
                },
                onAddClick = onLunchClick,
                onRemoveClick = if (mealPlan?.lunchSlot?.recipeId != null) onRemoveLunch else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dinner slot
            MealSlotRow(
                emoji = "\uD83C\uDF7D️",
                label = stringResource(R.string.dinner),
                recipeName = mealPlan?.dinnerSlot?.recipeId?.let { id ->
                    recipes[id]?.getTitle(language)
                } ?: mealPlan?.dinnerSlot?.recipeName,
                onClick = {
                    val recipeId = mealPlan?.dinnerSlot?.recipeId
                    if (recipeId != null) {
                        onRecipeClick(recipeId)
                    } else {
                        onDinnerClick()
                    }
                },
                onAddClick = onDinnerClick,
                onRemoveClick = if (mealPlan?.dinnerSlot?.recipeId != null) onRemoveDinner else null
            )
        }
    }
}

@Composable
private fun MealSlotRow(
    emoji: String,
    label: String,
    recipeName: String?,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    onRemoveClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium
        )
        if (recipeName != null) {
            Text(
                text = recipeName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            onRemoveClick?.let { remove ->
                IconButton(
                    onClick = remove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_meal),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.empty_slot),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_recipe),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeSelectorDialog(
    recipes: List<com.kroslabs.recipemanager.domain.model.Recipe>,
    language: com.kroslabs.recipemanager.domain.model.Language,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRecipeSelect: (com.kroslabs.recipemanager.domain.model.Recipe) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_recipe)) },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_recipes)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(recipes, key = { it.id }) { recipe ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    recipe.getTitle(language),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.clickable { onRecipeSelect(recipe) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ShareMealPlanDialog(
    onShare: (LocalDate, LocalDate, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRange by remember { mutableStateOf(0) } // 0: this week, 1: next week, 2: custom
    var includeShoppingList by remember { mutableStateOf(true) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(6)) }

    val today = LocalDate.now()
    val thisWeekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val thisWeekEnd = thisWeekStart.plusDays(6)
    val nextWeekStart = thisWeekEnd.plusDays(1)
    val nextWeekEnd = nextWeekStart.plusDays(6)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.generate_summary)) },
        text = {
            Column {
                // Range selection
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRange == 0,
                        onClick = {
                            selectedRange = 0
                            startDate = thisWeekStart
                            endDate = thisWeekEnd
                        },
                        label = { Text(stringResource(R.string.this_week)) }
                    )
                    FilterChip(
                        selected = selectedRange == 1,
                        onClick = {
                            selectedRange = 1
                            startDate = nextWeekStart
                            endDate = nextWeekEnd
                        },
                        label = { Text(stringResource(R.string.next_week)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Shopping list toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeShoppingList,
                        onCheckedChange = { includeShoppingList = it }
                    )
                    Text(stringResource(R.string.include_shopping_list))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onShare(startDate, endDate, includeShoppingList) }
            ) {
                Text(stringResource(R.string.share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
