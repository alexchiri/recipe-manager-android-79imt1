package com.kroslabs.recipemanager.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kroslabs.recipemanager.util.DebugLogger
import com.kroslabs.recipemanager.util.LogEntry
import com.kroslabs.recipemanager.util.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    onNavigateBack: () -> Unit
) {
    val logs by DebugLogger.logsFlow.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Debug Logs", DebugLogger.getLogsAsText())
                        clipboard.setPrimaryClip(clip)
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy logs")
                    }
                    IconButton(onClick = { DebugLogger.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No logs yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs, key = { "${it.timestamp}-${it.message.hashCode()}" }) { log ->
                    LogEntryItem(log)
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(log: LogEntry) {
    val backgroundColor = when (log.level) {
        LogLevel.DEBUG -> MaterialTheme.colorScheme.surface
        LogLevel.INFO -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        LogLevel.WARN -> Color(0xFFFFF3E0)
        LogLevel.ERROR -> Color(0xFFFFEBEE)
    }

    val textColor = when (log.level) {
        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurface
        LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
        LogLevel.WARN -> Color(0xFFE65100)
        LogLevel.ERROR -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.formattedTime,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = log.level.name,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textColor
                )
            }
            Text(
                text = "[${log.tag}]",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = log.message,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                softWrap = true,
                overflow = TextOverflow.Visible
            )
            log.throwable?.let { throwable ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = throwable.stackTraceToString(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFC62828),
                    modifier = Modifier.fillMaxWidth(),
                    softWrap = true,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}
