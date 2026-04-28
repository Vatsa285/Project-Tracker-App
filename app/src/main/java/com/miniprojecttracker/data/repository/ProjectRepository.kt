package com.miniprojecttracker.data.repository

import com.miniprojecttracker.data.local.dao.ProjectDao
import com.miniprojecttracker.data.local.dao.TaskDao
import com.miniprojecttracker.data.local.entity.ProjectEntity
import com.miniprojecttracker.data.remote.FirestoreDataSource
import com.miniprojecttracker.domain.model.Project
import com.miniprojecttracker.domain.model.ProjectStatus
import com.miniprojecttracker.domain.model.UpdateRequestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val firestoreDataSource: FirestoreDataSource
) {
    fun getAllProjects(): Flow<List<Project>> =
        projectDao.getAllProjects().map { list -> list.map { it.toDomain() } }

    fun getProjectById(projectId: String): Flow<Project?> =
        projectDao.getProjectById(projectId).map { it?.toDomain() }

    fun getProjectsByTeam(teamId: String): Flow<List<Project>> =
        projectDao.getProjectsByTeam(teamId).map { list -> list.map { it.toDomain() } }

    fun getProjectsByManager(managerId: String): Flow<List<Project>> =
        projectDao.getProjectsByManager(managerId).map { list -> list.map { it.toDomain() } }

    fun searchProjects(query: String): Flow<List<Project>> =
        projectDao.searchProjects(query).map { list -> list.map { it.toDomain() } }

    suspend fun createProject(project: Project): Project {
        val newProject = project.copy(
            id = if (project.id.isBlank()) UUID.randomUUID().toString() else project.id
        )
        firestoreDataSource.createProject(newProject)
        projectDao.insertProject(ProjectEntity.fromDomain(newProject))
        return newProject
    }

    suspend fun updateProject(project: Project) {
        firestoreDataSource.updateProject(project)
        projectDao.updateProject(ProjectEntity.fromDomain(project))
    }

    suspend fun deleteProject(project: Project) {
        firestoreDataSource.deleteProject(project.id)
        projectDao.deleteProject(ProjectEntity.fromDomain(project))
        taskDao.deleteTasksByProject(project.id)
    }

    /**
     * Recalculates project progress based on completed tasks.
     * Progress = (done tasks / total tasks) as 0.0 - 1.0
     */
    suspend fun recalculateProgress(projectId: String) {
        val total = taskDao.getTaskCountByProject(projectId)
        if (total == 0) {
            projectDao.updateProgress(projectId, 0f)
            return
        }
        val done = taskDao.getTaskCountByProjectAndStatus(projectId, "DONE")
        val progress = done.toFloat() / total.toFloat()
        projectDao.updateProgress(projectId, progress)

        // Also update in Firestore
        val project = projectDao.getProjectByIdOnce(projectId)?.toDomain() ?: return
        firestoreDataSource.updateProject(project.copy(progress = progress))
    }

    suspend fun getProjectCount(): Int = projectDao.getProjectCount()

    suspend fun getProjectCountByStatus(status: String): Int = projectDao.getProjectCountByStatus(status)

    suspend fun syncProjects() {
        try {
            val projects = firestoreDataSource.observeProjects().first()
            projectDao.insertProjects(projects.map { ProjectEntity.fromDomain(it) })
        } catch (_: Exception) {}
    }

    suspend fun submitStatusUpdateRequest(projectId: String, documentUrl: String, comment: String) {
        val project = projectDao.getProjectByIdOnce(projectId)?.toDomain() ?: return
        val updatedProject = project.copy(
            documentUrl = documentUrl,
            submissionComment = comment,
            updateRequestStatus = com.miniprojecttracker.domain.model.UpdateRequestStatus.PENDING
        )
        updateProject(updatedProject)
    }

    suspend fun reviewStatusUpdateRequest(projectId: String, approved: Boolean, nextStatus: ProjectStatus, comment: String) {
        val project = projectDao.getProjectByIdOnce(projectId)?.toDomain() ?: return
        
        val historyItem = com.miniprojecttracker.domain.model.ProjectStatusUpdate(
            id = UUID.randomUUID().toString(),
            fromStatus = project.status,
            toStatus = if (approved) nextStatus else project.status,
            documentUrl = project.documentUrl,
            submissionComment = project.submissionComment,
            reviewComment = comment,
            status = if (approved) com.miniprojecttracker.domain.model.UpdateRequestStatus.APPROVED else com.miniprojecttracker.domain.model.UpdateRequestStatus.REJECTED,
            timestamp = System.currentTimeMillis()
        )
        
        val updatedProject = if (approved) {
            project.copy(
                status = nextStatus,
                updateRequestStatus = com.miniprojecttracker.domain.model.UpdateRequestStatus.APPROVED,
                reviewComment = comment,
                documentUrl = "", // Clear current link after approval
                statusHistory = project.statusHistory + historyItem
            )
        } else {
            project.copy(
                updateRequestStatus = com.miniprojecttracker.domain.model.UpdateRequestStatus.REJECTED,
                reviewComment = comment,
                statusHistory = project.statusHistory + historyItem
            )
        }
        updateProject(updatedProject)
    }

    suspend fun startProject(projectId: String) {
        val project = projectDao.getProjectByIdOnce(projectId)?.toDomain() ?: return
        if (project.status == ProjectStatus.NOT_STARTED) {
            updateProject(project.copy(status = ProjectStatus.PLANNING))
        }
    }
}
