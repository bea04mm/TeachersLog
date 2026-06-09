# Teacher's Log - Android Application

## Overvew
A professional Android application designed for teachers to manage academic cycles, subjects, student groups (turmas), and evaluations. The app features dynamic filtering by academic year and a weighted grading system.

## Architecture
The application follows the **MVVM (Model-View-ViewModel)** architectural pattern:

### 1. Data Layer (Model)
- **Room Database**: Local persistence with automated schema management.
- **DAO (Data Access Object)**: SQL logic including weighted averages calculation.
- **Repository**: Single source of truth for the ViewModel, abstracting database access.

### 2. Logic Layer (ViewModel)
- **TeacherViewModel**: Manages reactive data streams using `LiveData` and `switchMap`. 
- **Reactive Filtering**: Changing the academic year in the Sidebar automatically triggers a refresh of all related subjects and stats.

### 3. UI Layer (View)
- **Jetpack Compose**: Modern declarative UI with Material 3 components.
- **Dynamic Themes**: Real-time Light/Dark mode switching.
- **Responsive Layouts**: Consistent TopBar and BottomBar navigation across academic contexts.

## Core Features
- **Weighted Grading**: Calculates final grades based on customized weights (Homework 20%, Participation 10%, Quizzes 40%, Projects 30%).
- **Academic Hierarchy**: Logical organization from Academic Year -> Subject -> Turma -> Student.
- **Navigation Safety**: Automated URL encoding for special characters (e.g., "4ºA").

## Technical Stack
- **Language**: Kotlin
- **Persistence**: Room SQLite
- **UI**: Compose, Material 3
- **Navigation**: Compose Navigation
- **Lifecycle**: LiveData, Coroutines, Flow
