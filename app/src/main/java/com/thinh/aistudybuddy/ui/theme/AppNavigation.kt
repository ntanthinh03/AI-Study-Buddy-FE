package com.thinh.aistudybuddy.ui.theme

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thinh.aistudybuddy.data.local.LocalHistoryStore
import com.thinh.aistudybuddy.data.local.SessionStore
import com.thinh.aistudybuddy.data.local.TokenDataStore
import com.thinh.aistudybuddy.services.network.RetrofitClient
import com.thinh.aistudybuddy.ui.pages.*
import com.thinh.aistudybuddy.viewmodel.*
import com.thinh.aistudybuddy.data.models.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(navController: NavHostController, initialDisplayName: String = "") {
    val studyViewModel: StudyPlanViewModel = viewModel()
    val quizViewModel: QuizViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val flashcardViewModel: FlashcardViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val focusViewModel: FocusViewModel = viewModel()
    val leaderboardViewModel: LeaderboardViewModel = viewModel()
    val mindMapViewModel: MindMapViewModel = viewModel(factory = ViewModelFactory(RetrofitClient.instance))
    var selectedLessonId by remember { mutableStateOf<String?>(null) }
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var inboxBootstrapped by remember { mutableStateOf(false) }
    val navScope = rememberCoroutineScope()
    val forceLogin = {
        chatViewModel.resetForAccountSwitch()
        RetrofitClient.updateAuthToken(null)
        SessionStore.clearSession(navController.context)
        TokenDataStore.clearToken(navController.context)
        LocalHistoryStore.clearAll()
        inboxBootstrapped = false
        displayName = ""
        navController.navigate("login") {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(onFinished = {
                val hasToken = !RetrofitClient.authToken.isNullOrBlank()
                if (hasToken) {
                    chatViewModel.setDebugAccountIdentity(displayName)
                    chatViewModel.markNewChatLandingAfterLogin()
                    navController.navigate("chat") {
                        popUpTo("welcome") { inclusive = true }
                    }
                } else {
                    navController.navigate("login") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            })
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = { userName ->
                    displayName = userName
                    chatViewModel.setDebugAccountIdentity(userName)
                    Log.d("AppNavigation", "Login success -> markNewChatLandingAfterLogin(acct=${chatViewModel.debugAccountKeyHashForLogs})")
                    chatViewModel.markNewChatLandingAfterLogin()
                    inboxBootstrapped = false
                    navController.navigate("chat") {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRegisterClick = { navController.navigate("register") },
                onForgotPasswordClick = { navController.navigate("forgot_password") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBackToLogin = { navController.popBackStack() },
                onBackToChat = { navController.popBackStack() }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                onBackToLogin = {
                    navController.navigate("login") {
                        popUpTo("forgot_password") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onContinueToOtp = { email ->
                    val encodedEmail = Uri.encode(email)
                    navController.navigate("forgot_password_otp/$encodedEmail")
                }
            )
        }
        composable(
            route = "forgot_password_otp/{email}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = Uri.decode(backStackEntry.arguments?.getString("email").orEmpty())
            ForgotPasswordOtpScreen(
                email = email,
                onBack = { navController.popBackStack() },
                onOtpVerified = {
                    navController.navigate("forgot_password_reset/${Uri.encode(email)}")
                },
                onBackToLogin = {
                    navController.navigate("login") {
                        popUpTo("forgot_password") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = "forgot_password_reset/{email}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = Uri.decode(backStackEntry.arguments?.getString("email").orEmpty())
            ForgotPasswordResetScreen(
                email = email,
                onBack = { navController.popBackStack() },
                onBackToLogin = {
                    navController.navigate("login") {
                        popUpTo("forgot_password") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("chat") {
            LaunchedEffect(RetrofitClient.authToken, inboxBootstrapped) {
                if (!RetrofitClient.authToken.isNullOrBlank() && !inboxBootstrapped) {
                    inboxBootstrapped = true
                    chatViewModel.loadConversationsFromBackend()
                }
            }
            ChatScreen(
                userDisplayName = displayName,
                quizViewModel = quizViewModel,
                studyPlanViewModel = studyViewModel,
                onProfileClick = { navController.navigate("account") },
                onStartQuiz = { message: ChatMessage ->
                    val quizJson = message.artifactJson?.toString() ?: message.planJson
                    if (!quizJson.isNullOrBlank()) {
                        val questions = parseQuizFromJson(quizJson)
                        if (questions.isNotEmpty()) {
                            quizViewModel.loadQuestions(
                                newQuestions = questions,
                                title = "Quiz: ${message.attachmentName ?: "Chat"}",
                                documentId = message.documentId
                            )
                            navController.navigate("quiz")
                        }
                    } else if (!message.documentId.isNullOrBlank()) {
                        quizViewModel.generateQuizForDocument(message.documentId)
                        navController.navigate("quiz")
                    }
                },
                onAccountClick = { navController.navigate("account") },
                onSettingsClick = { navController.navigate("settings") },
                onFlashcardsClick = { navController.navigate("flashcards") },
                onAnalyticsClick = { navController.navigate("analytics") },
                onDailySessionClick = { navController.navigate("daily_session") },
                onFocusClick = { navController.navigate("focus") },
                onLeaderboardClick = { navController.navigate("leaderboard") },
                onStudyPlanClick = {
                    navScope.launch {
                        val conversationId = if (chatViewModel.activeConversationId.isBlank()) {
                            chatViewModel.ensureActiveConversationForStudyPlan()
                        } else {
                            chatViewModel.activeConversationId
                        }
                        studyViewModel.updateLessonConversationId(conversationId)
                        navController.navigate("study_plan")
                    }
                },
                onMockExamClick = { navController.navigate("mock_exam") },
                onStudyRoomClick = { navController.navigate("study_room") },
                onVersusArenaClick = { navController.navigate("versus_dashboard") },
                onMindMapClick = { docId, docName ->
                    mindMapViewModel.resetState()
                    
                    
                    val summary = chatViewModel.activeMessages.findLast { it.documentId == docId && it.text.length > 50 }?.text ?: "Generate mind map for this document."
                    mindMapViewModel.generateMindMap(docId, summary) {
                        
                    }
                    navController.navigate("mind_map/$docId/$docName")
                },
                onConversationHistoryClick = { conversationId: String -> navController.navigate("conversation_history/$conversationId") },
                onSessionExpired = forceLogin,
                flashcardViewModel = flashcardViewModel,
                viewModel = chatViewModel
            )
        }
        composable("study_plan") {
            StudyPlanScreen(
                onBack = { navController.popBackStack() },
                onLearnClick = { lesson: Lesson ->
                    navScope.launch {
                        selectedLessonId = lesson.id
                        val conversationId = if (chatViewModel.activeConversationId.isBlank()) {
                            chatViewModel.ensureActiveConversationForStudyPlan()
                        } else {
                            chatViewModel.activeConversationId
                        }
                        if (conversationId.isNullOrBlank()) return@launch
                        studyViewModel.updateLessonConversationId(conversationId)
                        studyViewModel.ensureLessonEnriched(lesson.id)
                        studyViewModel.markModuleStarted(lesson.documentId)
                        navController.navigate("lesson_learn")
                    }
                },
                studyViewModel = studyViewModel,
                onMindMapClick = { docId, docName ->
                    navController.navigate("mind_map/$docId/$docName")
                }
            )
        }
        composable("lesson_learn") {
            val lesson = studyViewModel.activePlan.lessons.find { it.id == selectedLessonId }
            lesson?.let {
                LaunchedEffect(it.id) {
                    studyViewModel.ensureLessonEnriched(it.id)
                }
                LessonLearnScreen(
                    lesson = it,
                    onBack = { navController.popBackStack() },
                    onStartQuiz = {
                        quizViewModel.setQuizBackendContext(
                            documentId = it.documentId.takeIf { docId -> docId.isNotBlank() },
                            lessonId = studyViewModel.resolveBackendLessonId(it.id),
                            title = it.title
                        )
                        quizViewModel.onQuizComplete = { score ->
                            studyViewModel.completeLesson(it.id, score)
                            quizViewModel.persistQuizToBackend()
                        }
                        navController.navigate("quiz")
                    },
                    quizViewModel = quizViewModel
                )
            }
        }
        composable("quiz") {
            QuizScreen(viewModel = quizViewModel, onCloseClick = { navController.popBackStack() })
        }
        composable("mock_exam") {
            com.thinh.aistudybuddy.ui.pages.MockExamScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("study_room") {
            StudyRoomScreen(
                onNavigateBack = { navController.popBackStack() },
                userDisplayName = displayName
            )
        }
        composable(
            route = "mind_map/{documentId}/{documentName}",
            arguments = listOf(
                navArgument("documentId") { type = NavType.StringType },
                navArgument("documentName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
            val documentName = backStackEntry.arguments?.getString("documentName") ?: ""
            MindMapScreen(
                documentId = documentId,
                documentName = documentName,
                onBack = { navController.popBackStack() },
                viewModel = mindMapViewModel,
                flashcardViewModel = flashcardViewModel
            )
        }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable(
            route = "conversation_history/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty()
            ConversationHistoryScreen(
                conversationId = conversationId,
                onBack = { navController.popBackStack() },
                onOpenConversation = { conversationId: String ->
                    chatViewModel.selectConversation(conversationId)
                    navController.popBackStack()
                },
                onOpenLesson = { planRawJson: String, lessonId: String ->
                    studyViewModel.updateLessonConversationId(conversationId)
                    studyViewModel.loadStudyPlanFromJson(planRawJson)
                    selectedLessonId = lessonId
                    studyViewModel.ensureLessonEnriched(lessonId)
                    val lesson = studyViewModel.activePlan.lessons.find { it.id == lessonId }
                    if (lesson != null) {
                        studyViewModel.markModuleStarted(lesson.documentId)
                    }
                    navController.navigate("lesson_learn")
                },
                onOpenMindMap = { docId ->
                    val docName = chatViewModel.conversations.find { it.documentId == docId }?.title ?: "Document"
                    navController.navigate("mind_map/$docId/$docName")
                },
                timelineStatusByDocumentId = studyViewModel.timeline.associate { it.documentId to it.status },
                onRefresh = { conversationId: String ->
                    chatViewModel.refreshConversationMessages(conversationId)
                    studyViewModel.refreshProgressTimeline()
                },
                onStartQuiz = { message: ChatMessage ->
                    val quizJson = message.artifactJson?.toString() ?: message.planJson
                    if (!quizJson.isNullOrBlank()) {
                        val questions = parseQuizFromJson(quizJson)
                        if (questions.isNotEmpty()) {
                            quizViewModel.loadQuestions(
                                newQuestions = questions,
                                title = "Quiz: ${message.attachmentName ?: "History"}",
                                documentId = message.documentId
                            )
                            navController.navigate("quiz")
                        }
                    } else if (!message.documentId.isNullOrBlank()) {
                        quizViewModel.generateQuizForDocument(message.documentId)
                        navController.navigate("quiz")
                    }
                },
                onCheckPlan = { planJson: String? ->
                    if (!planJson.isNullOrBlank()) {
                        studyViewModel.loadStudyPlanFromJson(planJson)
                        studyViewModel.refreshProgressTimeline()
                        navController.navigate("study_plan")
                    }
                },
                chatViewModel = chatViewModel,
                mindMapViewModel = mindMapViewModel
            )
        }
        composable("account") {
            UserAccountScreen(
                onBack = { navController.popBackStack() },
                onChangePassword = { navController.navigate("change_password") },
                onLogout = {
                    forceLogin()
                }
            )
        }
        composable("change_password") {
            ChangePasswordScreen(
                onBack = { navController.popBackStack() },
                onSessionExpired = forceLogin
            )
        }
        composable("flashcards") {
            FlashcardScreen(
                onBackClick = { navController.popBackStack() },
                onStartStudy = { docId ->
                    flashcardViewModel.focusDocument(docId.takeIf { it != "all" })
                    navController.navigate("flashcard_review/$docId")
                },
                viewModel = flashcardViewModel
            )
        }
        composable(
            route = "flashcard_review/{documentId}",
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: "all"
            FlashcardReviewScreen(
                documentId = documentId,
                onBackClick = { navController.popBackStack() },
                viewModel = flashcardViewModel
            )
        }
        composable("analytics") {
            AnalyticsScreen(
                onBack = { navController.popBackStack() },
                viewModel = analyticsViewModel
            )
        }
        composable("daily_session") {
            val dailyViewModel: DailySessionViewModel = viewModel()
            DailySessionScreen(
                onBack = { navController.popBackStack() },
                viewModel = dailyViewModel
            )
        }
        composable("focus") {
            FocusScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = focusViewModel
            )
        }
        composable("leaderboard") {
            LeaderboardScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = leaderboardViewModel
            )
        }
        composable("versus_dashboard") {
            VersusDashboardScreen(
                onBack = {
                    navController.navigate("chat") {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                },
                onStartBattle = { navController.navigate("versus_matchmaking") },
                onReviewMatch = { matchId ->
                    navController.navigate("versus_review/$matchId")
                }
            )
        }
        composable(
            route = "versus_matchmaking?documentId={documentId}",
            arguments = listOf(navArgument("documentId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("documentId")
            VersusMatchmakingScreen(
                initialDocumentId = docId,
                onBack = { navController.popBackStack() },
                onMatchFound = { matchId, isGuest ->
                    navController.navigate("versus_countdown/$matchId?isGuest=$isGuest") {
                        popUpTo("versus_matchmaking") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "versus_countdown/{matchId}?isGuest={isGuest}",
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("isGuest") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId").orEmpty()
            val isGuest = backStackEntry.arguments?.getBoolean("isGuest") ?: false
            VersusCountdownScreen(
                matchId = matchId,
                onFinished = { mId ->
                    navController.navigate("versus_arena/$mId?isGuest=$isGuest") {
                        popUpTo("versus_countdown/$mId?isGuest=$isGuest") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "versus_arena/{matchId}?isGuest={isGuest}",
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("isGuest") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId").orEmpty()
            val isGuest = backStackEntry.arguments?.getBoolean("isGuest") ?: false
            VersusArenaScreen(
                matchId = matchId,
                isGuest = isGuest,
                onGameFinished = { mId ->
                    if (mId == "QUIT") {
                        navController.navigate("versus_dashboard") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    } else {
                        navController.navigate("versus_review/$mId") {
                            popUpTo("versus_arena/$mId?isGuest=$isGuest") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            route = "versus_review/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId").orEmpty()
            VersusReviewScreen(
                matchId = matchId,
                onReturnHome = {
                    navController.navigate("versus_dashboard") {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            )
        }
    }
}

private fun parseQuizFromJson(json: String): List<QuizQuestion> {
    return try {
        val root = JsonParser().parse(json)
        val questionsArray = when (root) {
            is JsonArray -> root
            is JsonObject -> root.getAsJsonArray("questions") ?: root.getAsJsonArray("quiz")
            else -> null
        } ?: return emptyList()

        questionsArray.mapIndexedNotNull { idx, element ->
            val item = element.asJsonObject ?: return@mapIndexedNotNull null
            val questionText = item.get("text")?.asStringOrNull() ?: item.get("question")?.asStringOrNull() ?: return@mapIndexedNotNull null

            val optionsRaw = item.get("options")
            val optionsList = when {
                optionsRaw == null || optionsRaw.isJsonNull -> emptyList()
                optionsRaw.isJsonArray -> optionsRaw.asJsonArray.mapNotNull { it.asStringOrNull() }
                optionsRaw.isJsonObject -> listOf("A", "B", "C", "D")
                    .mapNotNull { key -> optionsRaw.asJsonObject.get(key)?.asStringOrNull() }
                else -> emptyList()
            }.take(4)
            if (optionsList.size != 4) return@mapIndexedNotNull null

            val answerIndex = when {
                item.has("correctAnswerIndex") -> item.get("correctAnswerIndex").takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt?.coerceIn(0, 3) ?: 0
                else -> orderedAnswerIndex(
                    item.get("answer")?.asStringOrNull()
                        ?: item.get("correctAnswer")?.asStringOrNull()
                        ?: item.get("correct")?.asStringOrNull()
                        ?: "A",
                    optionsList
                )
            }

            QuizQuestion(
                id = item.get("id")?.asStringOrNull() ?: "hist-$idx",
                question = questionText,
                options = optionsList,
                correctAnswerIndex = answerIndex,
                hint = item.get("hint")?.asStringOrNull() ?: "Choose the best answer.",
                explanation = item.get("explanation")?.asStringOrNull() ?: ""
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("AppNavigation", "Failed to parse quiz JSON", e)
        emptyList()
    }
}

private fun JsonElement.asStringOrNull(): String? =
    if (isJsonNull) null else runCatching { asString }.getOrNull()

private fun orderedAnswerIndex(answerRaw: String, options: List<String>): Int {
    val normalized = answerRaw.trim()
    val byLetter = when (normalized.uppercase()) {
        "A" -> 0
        "B" -> 1
        "C" -> 2
        "D" -> 3
        else -> -1
    }
    if (byLetter in 0..3) return byLetter
    val byValue = options.indexOfFirst { it.equals(normalized, ignoreCase = true) }
    return if (byValue >= 0) byValue else 0
}