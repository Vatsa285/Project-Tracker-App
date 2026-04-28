package com.miniprojecttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miniprojecttracker.domain.model.Priority
import com.miniprojecttracker.domain.model.Project
import com.miniprojecttracker.domain.model.ProjectStatus
import com.miniprojecttracker.domain.model.ProjectStatusUpdate

/**
 * Room entity for projects.
 * Enums stored as strings for readability and Firestore compatibility.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val teamId: String,
    val name: String,
    val description: String,
    val deadline: Long,
    val priority: String,
    val status: String,
    val progress: Float,
    val managerId: String, // Added for isolation
    val createdAt: Long,
    val documentUrl: String = "",
    val updateRequestStatus: String = "NONE",
    val submissionComment: String = "",
    val reviewComment: String = "",
    val previousStatusBeforeHold: String? = null,
    val statusHistory: List<ProjectStatusUpdate> = emptyList()
) {
    fun toDomain(): Project = Project(
        id = id,
        teamId = teamId,
        name = name,
        description = description,
        deadline = deadline,
        priority = try { Priority.valueOf(priority) } catch (e: Exception) { Priority.MEDIUM },
        status = try { ProjectStatus.valueOf(status) } catch (e: Exception) { ProjectStatus.PLANNING },
        progress = progress,
        managerId = managerId,
        createdAt = createdAt,
        documentUrl = documentUrl,
        updateRequestStatus = try { com.miniprojecttracker.domain.model.UpdateRequestStatus.valueOf(updateRequestStatus) } catch (e: Exception) { com.miniprojecttracker.domain.model.UpdateRequestStatus.NONE },
        submissionComment = submissionComment,
        reviewComment = reviewComment,
        previousStatusBeforeHold = previousStatusBeforeHold?.let { try { ProjectStatus.valueOf(it) } catch(e: Exception) { null } },
        statusHistory = statusHistory
    )

    companion object {
        fun fromDomain(project: Project): ProjectEntity = ProjectEntity(
            id = project.id,
            teamId = project.teamId,
            name = project.name,
            description = project.description,
            deadline = project.deadline,
            priority = project.priority.name,
            status = project.status.name,
            progress = project.progress,
            managerId = project.managerId,
            createdAt = project.createdAt,
            documentUrl = project.documentUrl,
            updateRequestStatus = project.updateRequestStatus.name,
            submissionComment = project.submissionComment,
            reviewComment = project.reviewComment,
            previousStatusBeforeHold = project.previousStatusBeforeHold?.name,
            statusHistory = project.statusHistory
        )
    }
}
