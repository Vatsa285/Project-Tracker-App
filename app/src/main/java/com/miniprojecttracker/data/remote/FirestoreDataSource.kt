package com.miniprojecttracker.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.miniprojecttracker.domain.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles all Firestore read/write operations.
 * Each collection mirrors a Room entity table.
 * Real-time listeners provide instant sync to local cache.
 */
@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Collection names
    companion object {
        const val USERS = "users"
        const val TEAMS = "teams"
        const val PROJECTS = "projects"
        const val TASKS = "tasks"
        const val COMMENTS = "comments"
    }

    // ==================== USERS ====================

    suspend fun createUser(user: User) {
        firestore.collection(USERS).document(user.id).set(user.toMap()).await()
    }

    suspend fun updateUser(user: User) {
        firestore.collection(USERS).document(user.id).set(user.toMap(), SetOptions.merge()).await()
    }

    suspend fun getUser(userId: String): User? {
        val doc = firestore.collection(USERS).document(userId).get().await()
        return doc.toUser()
    }

    fun observeUsers(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection(USERS).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val users = snapshot?.documents?.mapNotNull { it.toUser() } ?: emptyList()
            trySend(users)
        }
        awaitClose { listener.remove() }
    }

    fun observeUsersByRole(role: UserRole): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection(USERS)
            .whereEqualTo("role", role.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val users = snapshot?.documents?.mapNotNull { it.toUser() } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addPointsToUser(userId: String, points: Int) {
        val user = getUser(userId) ?: return
        firestore.collection(USERS).document(userId)
            .update("points", user.points + points).await()
    }

    suspend fun resetAllDeveloperPoints() {
        val developers = firestore.collection(USERS)
            .whereEqualTo("role", UserRole.DEVELOPER.name)
            .get().await()
        
        val batch = firestore.batch()
        for (doc in developers.documents) {
            batch.update(doc.reference, "points", 0)
        }
        batch.commit().await()
    }

    // ==================== TEAMS ====================

    suspend fun createTeam(team: Team) {
        firestore.collection(TEAMS).document(team.id).set(team.toMap()).await()
    }

    suspend fun updateTeam(team: Team) {
        firestore.collection(TEAMS).document(team.id).set(team.toMap(), SetOptions.merge()).await()
    }

    suspend fun deleteTeam(teamId: String) {
        firestore.collection(TEAMS).document(teamId).delete().await()
    }

    fun observeTeams(managerId: String? = null): Flow<List<Team>> = callbackFlow {
        val query = if (managerId != null) {
            firestore.collection(TEAMS).whereEqualTo("managerId", managerId)
        } else {
            firestore.collection(TEAMS)
        }
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val teams = snapshot?.documents?.mapNotNull { it.toTeam() } ?: emptyList()
            trySend(teams)
        }
        awaitClose { listener.remove() }
    }

    fun observeTeamsByLeader(leaderId: String): Flow<List<Team>> = callbackFlow {
        val listener = firestore.collection(TEAMS)
            .whereEqualTo("leaderId", leaderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val teams = snapshot?.documents?.mapNotNull { it.toTeam() } ?: emptyList()
                trySend(teams)
            }
        awaitClose { listener.remove() }
    }

    fun observeTeamsByMember(memberId: String): Flow<List<Team>> = callbackFlow {
        val listener = firestore.collection(TEAMS)
            .whereArrayContains("memberIds", memberId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val teams = snapshot?.documents?.mapNotNull { it.toTeam() } ?: emptyList()
                trySend(teams)
            }
        awaitClose { listener.remove() }
    }

    // ==================== PROJECTS ====================

    suspend fun createProject(project: Project) {
        firestore.collection(PROJECTS).document(project.id).set(project.toMap()).await()
    }

    suspend fun updateProject(project: Project) {
        firestore.collection(PROJECTS).document(project.id).set(project.toMap(), SetOptions.merge()).await()
    }

    suspend fun deleteProject(projectId: String) {
        firestore.collection(PROJECTS).document(projectId).delete().await()
    }

    fun observeProjects(managerId: String? = null): Flow<List<Project>> = callbackFlow {
        val query = if (managerId != null) {
            firestore.collection(PROJECTS).whereEqualTo("managerId", managerId)
        } else {
            firestore.collection(PROJECTS)
        }
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val projects = snapshot?.documents?.mapNotNull { it.toProject() } ?: emptyList()
            trySend(projects)
        }
        awaitClose { listener.remove() }
    }

    fun observeProjectsByTeam(teamId: String): Flow<List<Project>> = callbackFlow {
        val listener = firestore.collection(PROJECTS)
            .whereEqualTo("teamId", teamId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val projects = snapshot?.documents?.mapNotNull { it.toProject() } ?: emptyList()
                trySend(projects)
            }
        awaitClose { listener.remove() }
    }

    // ==================== TASKS ====================

    suspend fun createTask(task: Task) {
        firestore.collection(TASKS).document(task.id).set(task.toMap()).await()
    }

    suspend fun updateTask(task: Task) {
        firestore.collection(TASKS).document(task.id).set(task.toMap(), SetOptions.merge()).await()
    }

    suspend fun deleteTask(taskId: String) {
        firestore.collection(TASKS).document(taskId).delete().await()
    }

    fun observeTasks(): Flow<List<Task>> = callbackFlow {
        val listener = firestore.collection(TASKS).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val tasks = snapshot?.documents?.mapNotNull { it.toTask() } ?: emptyList()
            trySend(tasks)
        }
        awaitClose { listener.remove() }
    }

    fun observeTasksByProject(projectId: String): Flow<List<Task>> = callbackFlow {
        val listener = firestore.collection(TASKS)
            .whereEqualTo("projectId", projectId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val tasks = snapshot?.documents?.mapNotNull { it.toTask() } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    fun observeTasksByUser(userId: String): Flow<List<Task>> = callbackFlow {
        val listener = firestore.collection(TASKS)
            .whereEqualTo("assignedTo", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val tasks = snapshot?.documents?.mapNotNull { it.toTask() } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    // ==================== COMMENTS ====================

    suspend fun createComment(comment: Comment) {
        firestore.collection(COMMENTS).document(comment.id).set(comment.toMap()).await()
    }

    fun observeCommentsByTask(taskId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection(COMMENTS)
            .whereEqualTo("taskId", taskId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val comments = snapshot?.documents?.mapNotNull { it.toComment() } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    // ==================== MAPPING HELPERS ====================

    private fun User.toMap() = hashMapOf(
        "id" to id, "name" to name, "email" to email,
        "role" to role.name, "teamId" to teamId,
        "points" to points, "avatarUrl" to avatarUrl, "createdAt" to createdAt
    )

    private fun Team.toMap() = hashMapOf(
        "id" to id, "name" to name, "managerId" to managerId, "leaderId" to leaderId,
        "description" to description, "memberIds" to memberIds, "createdAt" to createdAt
    )

    private fun Project.toMap() = hashMapOf(
        "id" to id, "teamId" to teamId, "name" to name,
        "description" to description, "deadline" to deadline,
        "priority" to priority.name, "status" to status.name,
        "progress" to progress, "managerId" to managerId, "createdAt" to createdAt,
        "documentUrl" to documentUrl, "updateRequestStatus" to updateRequestStatus.name,
        "submissionComment" to submissionComment,
        "reviewComment" to reviewComment,
        "previousStatusBeforeHold" to previousStatusBeforeHold?.name,
        "statusHistory" to statusHistory.map { it.toMap() }
    )

    private fun ProjectStatusUpdate.toMap() = hashMapOf(
        "id" to id, "timestamp" to timestamp,
        "fromStatus" to fromStatus.name, "toStatus" to toStatus.name,
        "documentUrl" to documentUrl, "submissionComment" to submissionComment,
        "reviewComment" to reviewComment, "status" to status.name
    )

    private fun Task.toMap() = hashMapOf(
        "id" to id, "projectId" to projectId, "assignedTo" to assignedTo,
        "assignedToName" to assignedToName, "title" to title,
        "description" to description, "status" to status.name,
        "priority" to priority.name, "dueDate" to dueDate,
        "isBlocker" to isBlocker, "createdAt" to createdAt, "completedAt" to completedAt,
        "documentUrl" to documentUrl, "updateRequestStatus" to updateRequestStatus.name,
        "reviewComment" to reviewComment, "updateHistory" to updateHistory.map { it.toMap() }
    )

    private fun TaskUpdate.toMap() = hashMapOf(
        "id" to id, "documentUrl" to documentUrl, "comment" to comment,
        "status" to status.name, "timestamp" to timestamp
    )

    private fun Comment.toMap() = hashMapOf(
        "id" to id, "taskId" to taskId, "userId" to userId,
        "userName" to userName, "content" to content, "timestamp" to timestamp
    )

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User? {
        return try {
            User(
                id = getString("id") ?: id,
                name = getString("name") ?: "",
                email = getString("email") ?: "",
                role = try { UserRole.valueOf(getString("role") ?: "DEVELOPER") } catch (e: Exception) { UserRole.DEVELOPER },
                teamId = getString("teamId") ?: "",
                points = getLong("points")?.toInt() ?: 0,
                avatarUrl = getString("avatarUrl") ?: "",
                createdAt = getLong("createdAt") ?: 0L
            )
        } catch (e: Exception) { null }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toTeam(): Team? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val members = get("memberIds") as? List<String> ?: emptyList()
            Team(
                id = getString("id") ?: id,
                name = getString("name") ?: "",
                managerId = getString("managerId") ?: "",
                leaderId = getString("leaderId") ?: "",
                description = getString("description") ?: "",
                memberIds = members,
                createdAt = getLong("createdAt") ?: 0L
            )
        } catch (e: Exception) { null }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toProject(): Project? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val history = (get("statusHistory") as? List<Map<String, Any>>) ?: emptyList()
            Project(
                id = getString("id") ?: id,
                teamId = getString("teamId") ?: "",
                name = getString("name") ?: "",
                description = getString("description") ?: "",
                deadline = getLong("deadline") ?: 0L,
                priority = try { Priority.valueOf(getString("priority") ?: "MEDIUM") } catch (e: Exception) { Priority.MEDIUM },
                status = try { ProjectStatus.valueOf(getString("status") ?: "PLANNING") } catch (e: Exception) { ProjectStatus.PLANNING },
                progress = getDouble("progress")?.toFloat() ?: 0f,
                managerId = getString("managerId") ?: "",
                createdAt = getLong("createdAt") ?: 0L,
                documentUrl = getString("documentUrl") ?: "",
                updateRequestStatus = try { com.miniprojecttracker.domain.model.UpdateRequestStatus.valueOf(getString("updateRequestStatus") ?: "NONE") } catch (e: Exception) { com.miniprojecttracker.domain.model.UpdateRequestStatus.NONE },
                submissionComment = getString("submissionComment") ?: "",
                reviewComment = getString("reviewComment") ?: "",
                previousStatusBeforeHold = getString("previousStatusBeforeHold")?.let { try { ProjectStatus.valueOf(it) } catch (e: Exception) { null } },
                statusHistory = history.mapNotNull { it.toProjectStatusUpdate() }
            )
        } catch (e: Exception) { null }
    }

    private fun Map<String, Any>.toProjectStatusUpdate(): ProjectStatusUpdate? {
        return try {
            ProjectStatusUpdate(
                id = get("id") as? String ?: "",
                timestamp = get("timestamp") as? Long ?: 0L,
                fromStatus = try { ProjectStatus.valueOf(get("fromStatus") as? String ?: "NOT_STARTED") } catch (e: Exception) { ProjectStatus.NOT_STARTED },
                toStatus = try { ProjectStatus.valueOf(get("toStatus") as? String ?: "NOT_STARTED") } catch (e: Exception) { ProjectStatus.NOT_STARTED },
                documentUrl = get("documentUrl") as? String ?: "",
                submissionComment = get("submissionComment") as? String ?: "",
                reviewComment = get("reviewComment") as? String ?: "",
                status = try { UpdateRequestStatus.valueOf(get("status") as? String ?: "PENDING") } catch (e: Exception) { UpdateRequestStatus.PENDING }
            )
        } catch (e: Exception) { null }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toTask(): Task? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val history = (get("updateHistory") as? List<Map<String, Any>>) ?: emptyList()
            Task(
                id = getString("id") ?: id,
                projectId = getString("projectId") ?: "",
                assignedTo = getString("assignedTo") ?: "",
                assignedToName = getString("assignedToName") ?: "",
                title = getString("title") ?: "",
                description = getString("description") ?: "",
                status = try { TaskStatus.valueOf(getString("status") ?: "TODO") } catch (e: Exception) { TaskStatus.TODO },
                priority = try { Priority.valueOf(getString("priority") ?: "MEDIUM") } catch (e: Exception) { Priority.MEDIUM },
                dueDate = getLong("dueDate") ?: 0L,
                isBlocker = getBoolean("isBlocker") ?: false,
                createdAt = getLong("createdAt") ?: 0L,
                completedAt = getLong("completedAt") ?: 0L,
                documentUrl = getString("documentUrl") ?: "",
                updateRequestStatus = try { UpdateRequestStatus.valueOf(getString("updateRequestStatus") ?: "NONE") } catch (e: Exception) { UpdateRequestStatus.NONE },
                reviewComment = getString("reviewComment") ?: "",
                updateHistory = history.mapNotNull { it.toTaskUpdate() }
            )
        } catch (e: Exception) { null }
    }

    private fun Map<String, Any>.toTaskUpdate(): TaskUpdate? {
        return try {
            TaskUpdate(
                id = get("id") as? String ?: "",
                documentUrl = get("documentUrl") as? String ?: "",
                comment = get("comment") as? String ?: "",
                status = try { UpdateRequestStatus.valueOf(get("status") as? String ?: "PENDING") } catch (e: Exception) { UpdateRequestStatus.PENDING },
                timestamp = get("timestamp") as? Long ?: 0L
            )
        } catch (e: Exception) { null }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toComment(): Comment? {
        return try {
            Comment(
                id = getString("id") ?: id,
                taskId = getString("taskId") ?: "",
                userId = getString("userId") ?: "",
                userName = getString("userName") ?: "",
                content = getString("content") ?: "",
                timestamp = getLong("timestamp") ?: 0L
            )
        } catch (e: Exception) { null }
    }
}
