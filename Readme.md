# Buddy App: Mobile Client Application

This repository contains the mobile frontend of the AI Study Buddy platform, an Android client developed in Kotlin utilizing declarative Jetpack Compose UI architectures. The client coordinates with the `server-study-buddy` backend server to provide offline-first data synchronization, real-time cooperative study spaces, and local AI-driven content generation workflows.

---

## Architecture and Core Functionality

The mobile client leverages reactive programming paradigms and modern Android architecture guidelines to deliver a highly interactive interface.

### Primary Capabilities
- **Interactive Daily Quizzes**: Step-by-step quiz interfaces utilizing immediate color-coded responses (RoseWarning and EmeraldSuccess) accompanied by active theory explanations and final full-session summary reviews.
- **Milestone Streak Celebrations**: High-fidelity animated streak summaries displaying cumulative streak milestones (Bronze, Silver, Gold, Platinum, Diamond) based on verified study schedules.
- **Interactive Scholar Profile**: Access to registration inputs (Full Name, University Email, Phone Number, Major Selection) alongside base64 gallery image picker integrations for instant profile picture updates.
- **Transactional Verification Panels**: Custom dialog layers designed to verify profile alterations (Email and Phone changes) utilizing short-lived 6-digit OTP tokens delivered securely to current active emails via Brevo.
- **RAG & Leitner Spaced Repetition**: Deep integration with vector processing backends and local Leitner deck memory box systems to support personalized study intervals.
- **Robust List Optimizations**: Performance-tuned LazyColumn lists with optimized key constraints, fully resolving duplicate identification errors and preventing UI freeze anomalies.

---

## Technical Stack

- **Development Language**: Kotlin
- **User Interface Framework**: Jetpack Compose (Declarative UI)
- **HTTP Communications**: Retrofit 2 and custom OkHttp interceptors
- **Real-Time Communication**: Socket.IO client library
- **Persistence Layer**: Jetpack DataStore (encrypted configuration) and Room SQLite Database
- **Image Pipeline**: Coil (cached remote asset rendering)
- **Design Pattern**: Model-View-ViewModel (MVVM) coupled with the Repository pattern

---

## Directory Hierarchy

```
app/src/main/
├── java/com/thinh/aistudybuddy/
│   ├── data/
│   │   ├── models/          # Data schemas (StudySession, Flashcard, User)
│   │   ├── network/         # Retrofit API interface blueprints and HttpClient
│   │   └── local/           # Room Database, SharedPreferences, and DataStore
│   ├── ui/
│   │   ├── theme/           # Palette design tokens and structural styles
│   │   ├── screens/         # Compose layouts (ChatScreen, FlashcardScreen, etc.)
│   │   ├── components/      # Reusable Compose atomic nodes
│   │   └── AppNavigation.kt # Navigation graph definitions
│   ├── viewmodel/           # UI State holders (ChatViewModel, AuthViewModel)
│   └── MainActivity.kt      # Application bootstrap entry point
└── res/
    ├── values/strings.xml   # Localization resources
    └── drawable/            # Vector and raster assets
```

---

## Technical Deployment and Connection Guide

Follow these configuration steps to bind this client to the backend application server:

### Step 1: Confirm API Server Status
Prior to client execution, verify the `server-study-buddy` server is active. The backend must be configured to accept connections on port `3001` (or your chosen environment variable).

### Step 2: Establish Network Connections
1. **Android Emulator Deployment**:
   - The networking system defaults to loopback route `http://10.0.2.2:3001` to interface with the development host machine.
2. **Physical Hardware Deployment**:
   - The development computer and Android device must reside on the same Wi-Fi subnet.
   - Navigate to the **Settings Screen** in the app, input the host IP address (e.g., `http://192.168.1.15:3001`), and save.

### Step 3: Compile and Install
```bash
# Clean local cache structures
./gradlew clean

# Install directly to active emulator or tethered device
./gradlew installDebug
```

---

## State and Data Persistence Architecture

### Reactive State Pattern
The application UI observes state changes via Kotlin `StateFlow` structures exposed by corresponding ViewModels, ensuring data consistency across screen rotations and configuration changes:
```kotlin
class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    fun postMessage(content: String) {
        // API transmission and view state mutation logic
    }
}
```

### Storage Paradigms
- **Credential Storage**: Access tokens are preserved securely in an encrypted `TokenDataStore` utilizing the Jetpack DataStore engine.
- **Relational Cache**: Conversational streams and offline activity cards are stored in local Room SQL tables, optimizing data access in limited network environments.
