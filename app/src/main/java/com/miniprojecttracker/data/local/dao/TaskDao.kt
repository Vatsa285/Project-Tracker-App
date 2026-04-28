package com.miniprojecttracker.data.local.dao

import androidx.room.*
import com.miniprojecttracker.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskByIdOnce(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY priority DESC, createdAt DESC")
    fun getTasksByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE assignedTo = :userId ORDER BY dueDate ASC")
    fun getTasksByUser(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority DESC")
    fun getTasksByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND status = :status")
    fun getTasksByProjectAndStatus(projectId: String, status: String): Flow<List<TaskEntity>>

    // Overdue tasks: dueDate is in the past and task is not done
    @Query("SELECT * FROM tasks WHERE dueDate < :currentTime AND status != 'DONE' AND dueDate > 0 ORDER BY dueDate ASC")
    fun getOverdueTasks(currentTime: Long = System.currentTimeMillis()): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, completedAt: Long = 0L)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun deleteTasksByProject(projectId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    // Analytics queries
    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId")
    suspend fun getTaskCountByProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND status = :status")
    suspend fun getTaskCountByProjectAndStatus(projectId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE assignedTo = :userId AND status = 'DONE'")
    suspend fun getCompletedTaskCountByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE assignedTo = :userId")
    suspend fun getTotalTaskCountByUser(userId: String): Int
}
