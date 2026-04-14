# FE Buddy

Android app for the AI Study Buddy project.

## Tech Stack
- Kotlin
- Jetpack Compose
- Retrofit
- DataStore / SharedPreferences session storage
- Android ViewModel + Coroutines

## Main Features
- Login / register / forgot password / change password
- AI chat with documents
- PDF / image upload
- Quiz generation
- Study plan flow
- Progress timeline and lesson history
- Conversation inbox

## Run
Build the app with:

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-daemon
```

Or run the app from Android Studio.

## Notes
- Backend base URL is auto-detected from the active port.
- Authentication token is required for protected upload and chat flows.

