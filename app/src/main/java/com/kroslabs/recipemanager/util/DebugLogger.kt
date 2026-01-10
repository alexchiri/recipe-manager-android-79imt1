package com.kroslabs.recipemanager.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    val formattedMessage: String
        get() = buildString {
            append("[$formattedTime] ")
            append("[${level.name}] ")
            append("[$tag] ")
            append(message)
            throwable?.let {
                append("\n")
                append(it.stackTraceToString())
            }
        }
}

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

object DebugLogger {
    private const val TAG = "DebugLogger"
    private const val ONE_HOUR_MS = 60 * 60 * 1000L
    private const val MAX_LOGS = 1000

    private val logs = ConcurrentLinkedDeque<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private fun addLog(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        cleanupOldLogs()

        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )

        logs.addFirst(entry)

        // Limit total logs
        while (logs.size > MAX_LOGS) {
            logs.removeLast()
        }

        _logsFlow.value = logs.toList()

        // Also log to Android logcat
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }

    private fun cleanupOldLogs() {
        val cutoffTime = System.currentTimeMillis() - ONE_HOUR_MS
        while (logs.isNotEmpty() && logs.peekLast()?.timestamp ?: 0 < cutoffTime) {
            logs.removeLast()
        }
    }

    fun d(tag: String, message: String) {
        addLog(LogLevel.DEBUG, tag, message)
    }

    fun i(tag: String, message: String) {
        addLog(LogLevel.INFO, tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        addLog(LogLevel.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        addLog(LogLevel.ERROR, tag, message, throwable)
    }

    fun clearLogs() {
        logs.clear()
        _logsFlow.value = emptyList()
    }

    fun getLogs(): List<LogEntry> = logs.toList()

    fun getLogsAsText(): String = logs.joinToString("\n") { it.formattedMessage }
}
