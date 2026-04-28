package com.miniprojecttracker.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.miniprojecttracker.domain.model.ProjectStatusUpdate
import com.miniprojecttracker.domain.model.TaskUpdate

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTaskUpdateList(value: List<TaskUpdate>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toTaskUpdateList(value: String): List<TaskUpdate> {
        val listType = object : TypeToken<List<TaskUpdate>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromProjectStatusUpdateList(value: List<ProjectStatusUpdate>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toProjectStatusUpdateList(value: String): List<ProjectStatusUpdate> {
        val listType = object : TypeToken<List<ProjectStatusUpdate>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
