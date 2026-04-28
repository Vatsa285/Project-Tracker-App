package com.miniprojecttracker.data.local.dao

import androidx.room.*
import com.miniprojecttracker.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectById(projectId: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectByIdOnce(projectId: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE teamId = :teamId ORDER BY createdAt DESC")
    fun getProjectsByTeam(teamId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE managerId = :managerId ORDER BY createdAt DESC")
    fun getProjectsByManager(managerId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY deadline ASC")
    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchProjects(query: String): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET progress = :progress WHERE id = :projectId")
    suspend fun updateProgress(projectId: String, progress: Float)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int

    @Query("SELECT COUNT(*) FROM projects WHERE status = :status")
    suspend fun getProjectCountByStatus(status: String): Int
}
