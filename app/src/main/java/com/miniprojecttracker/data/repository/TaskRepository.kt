package com.miniprojecttracker.data.repository

import com.miniprojecttracker.data.local.dao.TaskDao
import com.miniprojecttracker.data.local.entity.TaskEntity
import com.miniprojecttracker.data.remote.FirestoreDataSource
import com.miniprojecttracker.domain.model.Task
import com.miniprojecttracker.domain.model.TaskStatus
import com.miniprojecttracker.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val firestoreDataSource: FirestoreDataSource
) {
    fun getAllTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { list -> list.map { it.toDomain() } }

    fun getTaskById(taskId: String): Flow<Task?> =
        taskDao.getTaskById(taskId).map { it?.toDomain() }

    fun getTasksByProject(projectId: String): Flow<List<Task>> =
        taskDao.getTasksByProject(projectId).map { list -> list.map { it.toDomain() } }

    fun getTasksByUser(userId: String): Flow<List<Task>> =
        taskDao.getTasksByUser(userId).map { list -> list.map { it.toDomain() } }

    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> =
        taskDao.getTasksByStatus(status.name).map { list -> list.map { it.toDomain() } }

    fun getTasksByProjectAndStatus(projectId: String, status: TaskStatus): Flow<List<Task>> =
        taskDao.getTasksByProjectAndStatus(projectId, status.name).map { list -> list.map { it.toDomain() } }

    fun getOverdueTasks(): Flow<List<Task>> =
        taskDao.getOverdueTasks().map { list -> list.map { it.toDomain() } }

    fun searchTasks(query: String): Flow<List<Task>> =
        taskDao.searchTasks(query).map { list -> list.map { it.toDomain() } }

    suspend fun createTask(task: Task): Task {
        val newTask = task.copy(
            id = if (task.id.isBlank()) UUID.randomUUID().toString() else task.id
        )
        firestoreDataSource.createTask(newTask)
        taskDao.insertTask(TaskEntity.fromDomain(newTask))
        return newTask
    }

    suspend fun updateTask(task: Task) {
        firestoreDataSource.updateTask(task)
        taskDao.updateTask(TaskEntity.fromDomain(task))
    }

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        val completedAt = if (status == TaskStatus.DONE) System.currentTimeMillis() else 0L
        taskDao.updateTaskStatus(taskId, status.name, completedAt)
        // Also update Firestore
        val task = taskDao.getTaskByIdOnce(taskId)?.toDomain() ?: return
        firestoreDataSource.updateTask(task.copy(status = status, completedAt = completedAt))
    }

    suspend fun submitUpdateRequest(taskId: String, documentUrl: String, comment: String) {
        val task = taskDao.getTaskByIdOnce(taskId)?.toDomain() ?: return
        val status = com.miniprojecttracker.domain.model.UpdateRequestStatus.PENDING
        val update = com.miniprojecttracker.domain.model.TaskUpdate(
            id = UUID.randomUUID().toString(),
            documentUrl = documentUrl,
            comment = comment,
            status = status
        )
        val updatedTask = task.copy(
            documentUrl = documentUrl,
            updateRequestStatus = status,
            updateHistory = task.updateHistory + update
        )
        updateTask(updatedTask)
    }

    suspend fun reviewUpdateRequest(taskId: String, approved: Boolean, comment: String) {
        val task = taskDao.getTaskByIdOnce(taskId)?.toDomain() ?: return
        val status = if (approved) com.miniprojecttracker.domain.model.UpdateRequestStatus.APPROVED else com.miniprojecttracker.domain.model.UpdateRequestStatus.REJECTED
        
        val lastUpdate = task.updateHistory.lastOrNull()?.copy(status = status)
        val updatedHistory = if (lastUpdate != null) {
            task.updateHistory.dropLast(1) + lastUpdate
        } else task.updateHistory

        var updatedTask = task.copy(
            updateRequestStatus = status,
            reviewComment = comment,
            updateHistory = updatedHistory
        )
        
        if (approved) {
            updatedTask = updatedTask.copy(status = TaskStatus.DONE, completedAt = System.currentTimeMillis())
        }

        updateTask(updatedTask)
    }

    suspend fun deleteTask(task: Task) {
        firestoreDataSource.deleteTask(task.id)
        taskDao.deleteTask(TaskEntity.fromDomain(task))
    }

    suspend fun getCompletedTaskCount(userId: String): Int =
        taskDao.getCompletedTaskCountByUser(userId)

    suspend fun getTotalTaskCount(userId: String): Int =
        taskDao.getTotalTaskCountByUser(userId)

    suspend fun syncTasks(userId: String, role: UserRole) {
        try {
            val tasksFlow = when (role) {
                UserRole.MANAGER -> firestoreDataSource.observeTasks() // Managers see all tasks for now, or we could filter by their projects
                UserRole.TEAM_LEADER -> {
                    // For now, if we don't have a direct "observeTasksByTeamLeader", 
                    // we might need to observe all and filter, or observe by projects of their team.
                    // But FirestoreDataSource has observeTasksByProject.
                    // For simplicity in this sync, we can use observeTasks() if the volume is low, 
                    // or implement more specific ones.
                    firestoreDataSource.observeTasks()
                }
                UserRole.DEVELOPER -> firestoreDataSource.observeTasksByUser(userId)
            }
            val tasks = tasksFlow.first()
            taskDao.insertTasks(tasks.map { TaskEntity.fromDomain(it) })
        } catch (_: Exception) {}
    }
}
