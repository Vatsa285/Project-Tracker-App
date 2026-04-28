# Project Tracker 🚀

A comprehensive, production-ready Android application for role-based project and task management, built with modern Android development paradigms.

## 🌟 Features

supe- **Role-Based Access Control**: Secure login and separate dashboards for `MANAGER`, `TEAM_LEADER`, and `DEVELOPER`.
- **Project Lifecycle Management**: Enhanced project states including "On Hold" with strict operation locking to prevent edits, task updates, and comments during pauses.
- **Team Management**: Organize teams, assign team leaders, and delegate projects.
- **Task Tracking & Kanban Board**: Visual task management with Todo, In Progress, and Done states.
- **Analytics Dashboard**: Custom-built Pie and Bar charts for performance tracking and smart insights.
- **Enhanced Calendar View**: Navigate through a full year of deadlines (past and future) with specialized Project and Task views for Team Leaders and Managers.
- **Gamification & Leaderboard**: Earn points for completing tasks and view top performers.
- **Real-time Sync**: Powered by Firebase Firestore with Room for offline caching.

## 🛠️ Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM / Clean Architecture Principles
- **Local Database**: Room
- **Cloud Database & Auth**: Firebase (Firestore, Authentication)
- **Dependency Injection**: Dagger Hilt
- **Asynchronous Programming**: Kotlin Coroutines & Flows
- **Build System**: Gradle Version Catalogs (KTS)

## ⚙️ Setup Instructions

### 1. Firebase Configuration (CRITICAL)
This app relies on Firebase for Authentication and Firestore database. You **must** configure Firebase before running the app.

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project (e.g., "Project Tracker").
3. Add an Android app to the project. Use the package name: `com.miniprojecttracker`.
4. Register the app and download the `google-services.json` file.
5. Place the `google-services.json` file inside the `app/` directory of this project.
6. In the Firebase Console, enable **Authentication** (Email/Password provider).
7. Enable **Firestore Database** and start in test mode (or configure appropriate security rules).

### 2. Running the App
1. Open the project in **Android Studio** (recommended Ladybug or newer).
2. Allow Gradle to sync and download dependencies.
3. Build and run the app on an emulator or physical device.

### 3. Demo Accounts
The app seeds the database automatically on the first launch with mock data. You can log in using the following credentials:
- **Manager (formerly Super Manager):** `super@test.com` / `123456`
- **Team Leader (formerly Manager):** `manager@test.com` / `123456`
- **Developer:** `dev@test.com` / `123456`

*(Note: Ensure you sign up first if Firebase Authentication is strictly enforcing user records, or modify `SeedDataHelper` to handle auth creation if needed. For a fresh Firebase project, you will need to sign up these users first through the UI).*

## 📐 Architecture Overview
The project follows a repository-based architecture:
- `data/local`: Room Database, DAOs, Entities, and Preferences (DataStore).
- `data/remote`: Firestore Data Source bridging Firebase Callbacks to Kotlin Flows.
- `data/repository`: Single source of truth. Handles data syncing between remote and local sources.
- `domain/model`: Pure Kotlin data classes (User, Project, Task, Team).
- `ui/`: Compose UI separated by feature (auth, dashboard, project, kanban, etc.) with corresponding ViewModels.

## 🎨 UI Design
Designed entirely with Jetpack Compose using dynamic and expressive Material 3 components. The app includes dark mode support, custom data visualization widgets (Canvas API), and a consistent, premium color palette.
