package com.miniprojecttracker.data.repository

import com.miniprojecttracker.data.local.dao.CommentDao
import com.miniprojecttracker.data.local.entity.CommentEntity
import com.miniprojecttracker.data.remote.FirestoreDataSource
import com.miniprojecttracker.domain.model.Comment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val commentDao: CommentDao,
    private val firestoreDataSource: FirestoreDataSource
) {
    fun getCommentsByTask(taskId: String): Flow<List<Comment>> =
        commentDao.getCommentsByTask(taskId).map { list -> list.map { it.toDomain() } }

    suspend fun addComment(comment: Comment): Comment {
        val newComment = comment.copy(
            id = if (comment.id.isBlank()) UUID.randomUUID().toString() else comment.id
        )
        firestoreDataSource.createComment(newComment)
        commentDao.insertComment(CommentEntity.fromDomain(newComment))
        return newComment
    }

    suspend fun deleteComment(comment: Comment) {
        commentDao.deleteComment(CommentEntity.fromDomain(comment))
    }

    suspend fun syncComments(taskId: String) {
        try {
            val comments = firestoreDataSource.observeCommentsByTask(taskId).first()
            commentDao.insertComments(comments.map { CommentEntity.fromDomain(it) })
        } catch (_: Exception) {}
    }
}
