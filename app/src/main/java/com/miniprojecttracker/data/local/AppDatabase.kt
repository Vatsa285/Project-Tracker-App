package com.miniprojecttracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.miniprojecttracker.data.local.dao.*
import com.miniprojecttracker.data.local.entity.*

/**
 * Room database definition.
 * Single database holding all local cached data.
 * Acts as the single source of truth for UI — Firestore syncs into this.
 */
@Database(
    entities = [
        UserEntity::class,
        TeamEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        CommentEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun teamDao(): TeamDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun commentDao(): CommentDao
}
