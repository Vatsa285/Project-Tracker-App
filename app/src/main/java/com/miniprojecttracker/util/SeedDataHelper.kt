package com.miniprojecttracker.util

import com.miniprojecttracker.data.local.dao.*
import com.miniprojecttracker.data.local.entity.*
import com.miniprojecttracker.data.remote.FirestoreDataSource
import com.miniprojecttracker.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the database with sample data for testing and demo purposes.
 * Creates 3 users (one per role), 2 teams, 3 projects, and 12+ tasks.
 */
@Singleton
class SeedDataHelper @Inject constructor(
    private val userDao: UserDao,
    private val teamDao: TeamDao,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val commentDao: CommentDao,
    private val firestoreDataSource: FirestoreDataSource
) {
    // Fixed IDs for demo data — allows consistent references
    private val superManagerId = "demo_super_manager_001"
    private val managerId = "demo_manager_001"
    private val developerId1 = "demo_developer_001"
    private val developerId2 = "demo_developer_002"
    private val developerId3 = "demo_developer_003"

    private val team1Id = "demo_team_alpha"
    private val team2Id = "demo_team_beta"

    private val project1Id = "demo_project_ecommerce"
    private val project2Id = "demo_project_fitness"
    private val project3Id = "demo_project_chat"

    suspend fun seedDatabaseIfEmpty() {
        if (userDao.getUserCount() == 0) {
            seedAll()
        }
    }

    suspend fun seedAll() {
        val now = System.currentTimeMillis()
        val day = 86400000L // milliseconds in a day

        // === USERS ===
        val users = listOf(
            User(superManagerId, "Alex Johnson", "alex@demo.com", UserRole.MANAGER, "", 0),
            User(managerId, "Sarah Williams", "sarah@demo.com", UserRole.TEAM_LEADER, team1Id, 25),
            User(developerId1, "Mike Chen", "mike@demo.com", UserRole.DEVELOPER, team1Id, 85),
            User(developerId2, "Emily Davis", "emily@demo.com", UserRole.DEVELOPER, team1Id, 120),
            User(developerId3, "James Wilson", "james@demo.com", UserRole.DEVELOPER, team2Id, 45)
        )

        // === TEAMS ===
        val teams = listOf(
            Team(team1Id, "Team Alpha", superManagerId, managerId, "Frontend & Mobile squad", listOf(developerId1, developerId2), now),
            Team(team2Id, "Team Beta", superManagerId, managerId, "Backend & Infrastructure squad", listOf(developerId3), now)
        )

        // === PROJECTS ===
        val projects = listOf(
            Project(project1Id, team1Id, "E-Commerce Platform", "Full-stack e-commerce app with payment integration", now + 30 * day, Priority.HIGH, ProjectStatus.ACTIVE, 0.4f, superManagerId, now - 10 * day),
            Project(project2Id, team1Id, "Fitness Tracker App", "Mobile-first fitness and health tracking application", now + 45 * day, Priority.MEDIUM, ProjectStatus.PLANNING, 0.1f, superManagerId, now - 5 * day),
            Project(project3Id, team2Id, "Real-time Chat System", "WebSocket-based chat with end-to-end encryption", now + 15 * day, Priority.CRITICAL, ProjectStatus.ACTIVE, 0.65f, superManagerId, now - 20 * day)
        )

        // === TASKS ===
        val tasks = listOf(
            // E-Commerce tasks
            Task("task_001", project1Id, developerId1, "Mike Chen", "Design product catalog UI", "Create responsive product grid with filters", TaskStatus.DONE, Priority.HIGH, now + 5 * day, false, now - 8 * day, now - 2 * day),
            Task("task_002", project1Id, developerId2, "Emily Davis", "Implement shopping cart", "Add/remove items, quantity updates, price calculation", TaskStatus.IN_PROGRESS, Priority.HIGH, now + 10 * day, false, now - 6 * day, 0),
            Task("task_003", project1Id, developerId1, "Mike Chen", "Payment gateway integration", "Integrate Stripe/Razorpay for payments", TaskStatus.TODO, Priority.CRITICAL, now + 20 * day, false, now - 3 * day, 0),
            Task("task_004", project1Id, developerId2, "Emily Davis", "User authentication flow", "Login, signup, password reset with Firebase", TaskStatus.DONE, Priority.HIGH, now + 8 * day, false, now - 9 * day, now - 4 * day),

            // Fitness Tracker tasks
            Task("task_005", project2Id, developerId1, "Mike Chen", "Design workout screens", "Create workout timer and exercise list UI", TaskStatus.TODO, Priority.MEDIUM, now + 15 * day, false, now - 2 * day, 0),
            Task("task_006", project2Id, developerId2, "Emily Davis", "Step counter integration", "Use device sensors for step tracking", TaskStatus.TODO, Priority.LOW, now + 25 * day, false, now - 1 * day, 0),
            Task("task_007", project2Id, developerId1, "Mike Chen", "Nutrition tracker UI", "Calorie counting and meal logging interface", TaskStatus.TODO, Priority.MEDIUM, now + 20 * day, false, now, 0),

            // Chat System tasks
            Task("task_008", project3Id, developerId3, "James Wilson", "WebSocket server setup", "Configure Socket.IO server with rooms", TaskStatus.DONE, Priority.CRITICAL, now + 3 * day, false, now - 15 * day, now - 10 * day),
            Task("task_009", project3Id, developerId3, "James Wilson", "Message encryption", "Implement E2E encryption for messages", TaskStatus.IN_PROGRESS, Priority.HIGH, now + 8 * day, false, now - 10 * day, 0),
            Task("task_010", project3Id, developerId3, "James Wilson", "File sharing in chat", "Allow image/file sharing in conversations", TaskStatus.TODO, Priority.MEDIUM, now + 12 * day, false, now - 5 * day, 0),
            Task("task_011", project3Id, developerId3, "James Wilson", "Push notifications", "FCM integration for new messages", TaskStatus.TODO, Priority.HIGH, now + 10 * day, true, now - 3 * day, 0),
            Task("task_012", project3Id, developerId3, "James Wilson", "Chat UI polish", "Animations, read receipts, typing indicators", TaskStatus.DONE, Priority.LOW, now + 5 * day, false, now - 12 * day, now - 7 * day)
        )

        // === COMMENTS ===
        val comments = listOf(
            Comment("comment_001", "task_002", developerId2, "Emily Davis", "Cart state management is tricky with Compose. Using a ViewModel with StateFlow.", now - 2 * day),
            Comment("comment_002", "task_002", managerId, "Sarah Williams", "Looking good! Make sure to handle edge cases for empty cart.", now - 1 * day),
            Comment("comment_003", "task_009", developerId3, "James Wilson", "Using libsodium for the encryption layer. Performance is excellent.", now - 3 * day),
            Comment("comment_004", "task_011", developerId3, "James Wilson", "Blocked: Need FCM server key from the admin. Marking as blocker.", now - 2 * day),
            Comment("comment_005", "task_003", developerId1, "Mike Chen", "Should we support both Stripe and Razorpay, or just one?", now - 1 * day)
        )

        // Insert into Room
        userDao.insertUsers(users.map { UserEntity.fromDomain(it) })
        teamDao.insertTeams(teams.map { TeamEntity.fromDomain(it) })
        projectDao.insertProjects(projects.map { ProjectEntity.fromDomain(it) })
        taskDao.insertTasks(tasks.map { TaskEntity.fromDomain(it) })
        commentDao.insertComments(comments.map { CommentEntity.fromDomain(it) })

        // Also seed to Firestore
        try {
            users.forEach { firestoreDataSource.createUser(it) }
            teams.forEach { firestoreDataSource.createTeam(it) }
            projects.forEach { firestoreDataSource.createProject(it) }
            tasks.forEach { firestoreDataSource.createTask(it) }
            comments.forEach { firestoreDataSource.createComment(it) }
        } catch (_: Exception) {
            // Firestore may be unavailable offline; Room seed still works
        }
    }
}
