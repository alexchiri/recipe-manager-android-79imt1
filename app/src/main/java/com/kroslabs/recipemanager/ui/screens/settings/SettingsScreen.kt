package com.kroslabs.recipemanager.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kroslabs.recipemanager.R
import com.kroslabs.recipemanager.domain.model.Language
import com.kroslabs.recipemanager.util.DebugLogger
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogs: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showApiKey by remember { mutableStateOf(false) }

    // Export file picker
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                viewModel.exportData(context, it, ExportType.ALL)
            }
        }
    }

    // Import file picker
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                viewModel.importData(context, it)
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Key section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.api_key),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.apiKey,
                            onValueChange = viewModel::onApiKeyChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.api_key_hint)) },
                            visualTransformation = if (showApiKey)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            imageVector = if (showApiKey)
                                                Icons.Default.VisibilityOff
                                            else
                                                Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                    IconButton(onClick = { viewModel.saveApiKey() }) {
                                        Icon(Icons.Default.Save, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true
                        )
                    }
                }
            }

            // Language section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onShowLanguageDialog(true) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.language),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = uiState.language.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            // Cloud sync section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.cloud_sync),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.cloud_sync_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.cloudSyncEnabled,
                                onCheckedChange = viewModel::onCloudSyncToggle
                            )
                        }

                        if (uiState.cloudSyncEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.lastSync?.let { timestamp ->
                                        val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                                        stringResource(R.string.last_sync, dateFormat.format(Date(timestamp)))
                                    } ?: stringResource(R.string.never_synced),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { viewModel.onSyncNow() },
                                    enabled = !uiState.isSyncing
                                ) {
                                    if (uiState.isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(stringResource(R.string.sync_now))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Import/Export section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.import_data)) },
                            leadingContent = {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                importLauncher.launch("application/json")
                            }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.export_data)) },
                            leadingContent = {
                                Icon(Icons.Default.FileUpload, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                viewModel.onShowExportDialog(true)
                            }
                        )
                    }
                }
            }

            // Debug Logs section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToLogs() }
                ) {
                    ListItem(
                        headlineContent = { Text("View Debug Logs") },
                        supportingContent = { Text("View application logs for troubleshooting") },
                        leadingContent = {
                            Icon(Icons.Default.BugReport, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    )
                }
            }
        }

        // Language dialog
        if (uiState.showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onShowLanguageDialog(false) },
                title = { Text(stringResource(R.string.language)) },
                text = {
                    Column {
                        Language.entries.forEach { language ->
                            ListItem(
                                headlineContent = { Text(language.displayName) },
                                leadingContent = {
                                    RadioButton(
                                        selected = uiState.language == language,
                                        onClick = { viewModel.onLanguageChange(language) }
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.onLanguageChange(language)
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.onShowLanguageDialog(false) }) {
                        Text(stringResource(R.string.done))
                    }
                }
            )
        }

        // Export dialog
        if (uiState.showExportDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onShowExportDialog(false) },
                title = { Text(stringResource(R.string.export_data)) },
                text = {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.export_all)) },
                            modifier = Modifier.clickable {
                                val timestamp = System.currentTimeMillis()
                                exportLauncher.launch("recipe_manager_export_$timestamp.json")
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.export_recipes)) },
                            modifier = Modifier.clickable {
                                val timestamp = System.currentTimeMillis()
                                exportLauncher.launch("recipes_export_$timestamp.json")
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.export_meal_plans)) },
                            modifier = Modifier.clickable {
                                val timestamp = System.currentTimeMillis()
                                exportLauncher.launch("meal_plans_export_$timestamp.json")
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.onShowExportDialog(false) }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
