package com.miniprojecttracker.data.local.dao

import androidx.room.*
import com.miniprojecttracker.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getCommentsByTask(taskId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Delete
    suspend fun deleteComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE taskId = :taskId")
    suspend fun deleteCommentsByTask(taskId: String)

    @Query("DELETE FROM comments")
    suspend fun deleteAllComments()
}
