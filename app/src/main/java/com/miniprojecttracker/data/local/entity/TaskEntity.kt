package com.miniprojecttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miniprojecttracker.domain.model.Priority
import com.miniprojecttracker.domain.model.Task
import com.miniprojecttracker.domain.model.TaskStatus
import com.miniprojecttracker.domain.model.TaskUpdate
import com.miniprojecttracker.domain.model.UpdateRequestStatus

/**
 * Room entity for tasks within projects.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val assignedTo: String,
    val assignedToName: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val dueDate: Long,
    val isBlocker: Boolean,
    val createdAt: Long,
    val completedAt: Long,
    val documentUrl: String,
    val updateRequestStatus: String,
    val reviewComment: String,
    val updateHistory: List<TaskUpdate>
) {
    fun toDomain(): Task = Task(
        id = id,
        projectId = projectId,
        assignedTo = assignedTo,
        assignedToName = assignedToName,
        title = title,
        description = description,
        status = try { TaskStatus.valueOf(status) } catch (e: Exception) { TaskStatus.TODO },
        priority = try { Priority.valueOf(priority) } catch (e: Exception) { Priority.MEDIUM },
        dueDate = dueDate,
        isBlocker = isBlocker,
        createdAt = createdAt,
        completedAt = completedAt,
        documentUrl = documentUrl,
        updateRequestStatus = try { UpdateRequestStatus.valueOf(updateRequestStatus) } catch (e: Exception) { UpdateRequestStatus.NONE },
        reviewComment = reviewComment,
        updateHistory = updateHistory
    )

    companion object {
        fun fromDomain(task: Task): TaskEntity = TaskEntity(
            id = task.id,
            projectId = task.projectId,
            assignedTo = task.assignedTo,
            assignedToName = task.assignedToName,
            title = task.title,
            description = task.description,
            status = task.status.name,
            priority = task.priority.name,
            dueDate = task.dueDate,
            isBlocker = task.isBlocker,
            createdAt = task.createdAt,
            completedAt = task.completedAt,
            documentUrl = task.documentUrl,
            updateRequestStatus = task.updateRequestStatus.name,
            reviewComment = task.reviewComment,
            updateHistory = task.updateHistory
        )
    }
}
