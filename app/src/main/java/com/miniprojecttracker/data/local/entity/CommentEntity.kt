package com.miniprojecttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miniprojecttracker.domain.model.Comment

/**
 * Room entity for task comments / activity log entries.
 */
@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val userId: String,
    val userName: String,
    val content: String,
    val timestamp: Long
) {
    fun toDomain(): Comment = Comment(
        id = id,
        taskId = taskId,
        userId = userId,
        userName = userName,
        content = content,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(comment: Comment): CommentEntity = CommentEntity(
            id = comment.id,
            taskId = comment.taskId,
            userId = comment.userId,
            userName = comment.userName,
            content = comment.content,
            timestamp = comment.timestamp
        )
    }
}
