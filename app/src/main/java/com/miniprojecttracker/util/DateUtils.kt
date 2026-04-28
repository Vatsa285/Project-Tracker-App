package com.miniprojecttracker.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Date formatting utilities used across the app.
 */
object DateUtils {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val shortFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "No date"
        return dateFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        if (timestamp == 0L) return "No date"
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatShort(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return shortFormat.format(Date(timestamp))
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
