package com.miniprojecttracker.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date formatting utilities used across the app.
 * Uses java.time (thread-safe) since minSdk is 26.
 */
object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val shortFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "No date"
        val instant = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        return dateFormatter.format(instant)
    }

    fun formatDateTime(timestamp: Long): String {
        if (timestamp == 0L) return "No date"
        val instant = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        return dateTimeFormatter.format(instant)
    }

    fun formatShort(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val instant = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        return shortFormatter.format(instant)
    }

    fun isOverdue(deadline: Long): Boolean {
        if (deadline == 0L) return false
        return deadline < System.currentTimeMillis()
    }

    fun daysUntil(timestamp: Long): Int {
        if (timestamp == 0L) return Int.MAX_VALUE
        val diff = timestamp - System.currentTimeMillis()
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    fun getRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> formatDate(timestamp)
        }
    }
}
