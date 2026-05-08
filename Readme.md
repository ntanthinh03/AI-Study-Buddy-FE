# Buddy App - Frontend (Android)

**AI-powered study companion built with Kotlin and Jetpack Compose**

## Overview

Buddy is a mobile study application that helps users learn effectively through spaced repetition, AI-driven content generation, and gamification. The frontend is built with modern Android best practices using Kotlin and Jetpack Compose.

## Features

### Core Learning Features
- **Daily Study Sessions** — Quiz questions and flashcards tailored to your learning level
- **Spaced Repetition (Leitner Box)** — Intelligent flashcard scheduling based on learning progress
- **Document Upload & RAG** — Upload PDFs/images and learn from your own materials
- **AI Chat** — Multi-turn conversations with document context support
- **Quiz Generation** — Auto-generate quizzes from documents via AI
- **Study Plans** — AI-created study roadmaps tailored to your goals

### Gamification
- **XP & Leveling System** — Earn XP for completed sessions, correct answers, and focus time
- **Streaks** — Track daily study streaks and personal records
- **Leaderboard** — Compete with other learners on global rankings
- **Focus Timer** — 25-minute Pomodoro sessions with XP rewards

### Social Features
- **Co-study Rooms** — Study synchronously with friends in real-time
- **Live Quiz Challenges** — Compete in multiplayer quizzes with instant scoring
- **Study Groups** — Join or create study communities

### Account Management
- **JWT Authentication** — Secure login and registration
- **Password Recovery** — Email-based OTP verification
- **Profile Management** — Customize learning preferences
- **Progress Tracking** — View detailed learning analytics and history

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **HTTP Client:** Retrofit 2 with OkHttp interceptors
- **Real-time:** Socket.IO for multiplayer features
- **Local Storage:** DataStore, Room, SharedPreferences
- **Image Loading:** Coil with caching
- **Serialization:** Gson
- **Architecture:** MVVM + Repository pattern

## Project Structure

```
app/src/main/
├── java/com/thinh/aistudybuddy/
│   ├── data/
│   │   ├── models/          # Data classes (StudySession, Flashcard, etc.)
│   │   ├── network/         # Retrofit API interfaces and RetrofitClient
│   │   └── local/           # DataStore, SessionStore, TokenDataStore
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── screens/     # Main UI screens (Chat, DailySession, Quiz, etc.)
│   │   │   ├── components/  # Reusable Compose components
│   │   │   └── Color.kt     # Theme colors
│   │   └── AppNavigation.kt # Navigation setup
│   ├── viewmodel/           # State management (ChatViewModel, etc.)
│   └── MainActivity.kt
└── res/
    ├── values/strings.xml   # String resources
    └── drawable/            # Assets and icons
```

## Prerequisites

- Android SDK 24+ (Android 7.0+)
- Gradle 7+
- Kotlin 1.9+
- Backend server running on accessible network

## Installation & Setup

### 1. Clone and Prepare
```bash
cd FEBuddy
./gradlew clean
```

### 2. Configure Backend URL

Edit `app/src/main/java/com/thinh/aistudybuddy/data/local/NetworkConfigStore.kt` or use Settings screen:

**For Local Development:**
- Emulator: `http://10.0.2.2:3002` (auto-detected)
- Physical Device: `http://<PC-IP>:3002` (set in Settings)

**For Production:**
- Backend URL environment

### 3. Build

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on emulator/device
./gradlew installDebug

# Or run directly from Android Studio
# Shift + F10 (Windows) or Cmd + R (Mac)
```

### 4. Run on Emulator

```bash
# Start emulator first (from Android Studio or)
emulator -avd <emulator_name>

# Then run
./gradlew installDebug
```

## Key Screens

| Screen | Purpose |
|--------|---------|
| **LoginScreen** | User authentication |
| **ChatScreen** | Main chat interface with document upload |
| **DailySessionScreen** | Quiz + flashcard learning session |
| **FlashcardScreen** | Flashcard library and review mode |
| **AnalyticsScreen** | Learning dashboard with stats |
| **QuizScreen** | Take quizzes on demand |
| **MockExamScreen** | Full-length timed exams |
| **LeaderboardScreen** | View global rankings |
| **FocusScreen** | Pomodoro timer for focused study |
| **StudyRoomScreen** | Join multiplayer study sessions |
| **SettingsScreen** | Configure backend URL and preferences |

## State Management

State is managed using **ViewModel + StateFlow** pattern:

```kotlin
class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    fun sendMessage(text: String) {
        // Message sending logic
    }
}
```

Each screen has a corresponding ViewModel that:
- Manages UI state
- Makes API calls via RetrofitClient
- Handles local storage (DataStore, Room)
- Notifies UI of state changes

## API Integration

### RetrofitClient
Central API client configuration:

```kotlin
RetrofitClient.getInstance(context).create(ApiService::class.java)
```

Features:
- Auto-detects backend URL from active port
- Adds JWT token to all requests
- Handles token refresh on 401
- Timeout configuration for file uploads

### Key Endpoints

**Authentication:**
- `POST /auth/login` — User login
- `POST /auth/register` — Create account
- `POST /auth/change-password` — Update password
- `POST /auth/forgot-password` — Initiate password recovery

**Study Sessions:**
- `GET /study-sessions/daily` — Get/create daily session
- `POST /study-sessions/:id/submit` — Submit session results
- `GET /study-sessions/leaderboard` — Get top users

**Flashcards:**
- `POST /flashcards/generate` — Generate from document
- `GET /flashcards/to-review` — Get due cards
- `POST /flashcards/:id/review` — Record review

**Documents:**
- `POST /documents/upload` — Upload PDF/image
- `GET /documents` — List documents
- `DELETE /documents/:id` — Delete document

**Chat:**
- `POST /chat` — Send message with context
- `GET /conversations` — List conversations
- `GET /conversations/:id/messages` — Get history

**WebSocket (Study Rooms):**
- Connect: `ws://backend:3002/study-rooms`
- Events: `joinRoom`, `answerSubmit`, `leaderboardUpdate`

## Authentication Flow

1. **Login:** Send email + password → receive JWT token
2. **Store:** Save token to TokenDataStore (encrypted DataStore)
3. **Requests:** Retrofit automatically adds `Authorization: Bearer <token>` header
4. **Refresh:** On 401 response, request new token automatically
5. **Logout:** Clear token from storage

## Local Storage

### DataStore (Recommended)
```kotlin
// Secure token storage
val tokenStore = TokenDataStore(context)
tokenStore.getToken() // Read
tokenStore.saveToken(token) // Write
```

### Room Database
- **LocalHistoryStore:** Offline conversation history
- Pre-populated with sample data for development

### SharedPreferences (Legacy)
- Session data before DataStore migration
- Gradually replaced by DataStore

## Building for Release

```bash
# Create release keystore (first time)
keytool -genkey -v -keystore release.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias buddy-key

# Build signed APK
./gradlew bundleRelease
```

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests (on device/emulator)
```bash
./gradlew connectedAndroidTest
```

### Manual Testing
1. Start backend: `npm run start:dev`
2. Start emulator
3. Run app: `./gradlew installDebug`
4. Navigate through screens
5. Check logs: `adb logcat | grep "StudyBuddy"`

## Troubleshooting

### Build Issues

**Error: Cannot find Gradle dependency**
- Solution: `./gradlew clean && ./gradlew build --refresh-dependencies`

**Error: Build locked by another Gradle instance**
- Solution: `./gradlew --stop && ./gradlew clean assembleDebug`

### Runtime Issues

**Backend URL not connecting**
- Check: Settings screen → Backend Connection
- Verify backend is running: `curl http://<backend-ip>:3002/health`
- For emulator: Use `http://10.0.2.2:3002` instead of `localhost`

**Images not loading**
- Check: `adb logcat | grep Coil`
- Ensure document IDs are correct
- Verify backend serves images correctly

**Token expired/401 errors**
- App auto-refreshes token (check TokenDataStore)
- If persisting: Clear app data → Re-login
- `adb shell pm clear com.thinh.aistudybuddy`

## Performance Optimization

- **Lazy Loading:** Compose screens load only visible content
- **Image Caching:** Coil caches images locally
- **State Preservation:** ViewModel survives configuration changes
- **Network Caching:** Retrofit + OkHttp configured for smart caching

## Security

- **Token Storage:** Encrypted DataStore (not SharedPreferences)
- **HTTPS Ready:** Retrofit configured for SSL pinning (optional)
- **Input Validation:** All user inputs sanitized before API calls
- **Permissions:** Only requests necessary Android permissions

## Contributing

1. Create feature branch: `git checkout -b feature/your-feature`
2. Follow Kotlin style guide (4-space indentation)
3. Test on both emulator and physical device
4. Commit: `git commit -m "feat: description"`
5. Push and create PR

## Resources

- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Android Architecture Components](https://developer.android.com/topic/architecture)
- [Backend API Documentation](../server-study-buddy/README.md)

---

**Version:** 1.0  
**Last Updated:** May 8, 2026  
**Maintained by:** Study Buddy Team

