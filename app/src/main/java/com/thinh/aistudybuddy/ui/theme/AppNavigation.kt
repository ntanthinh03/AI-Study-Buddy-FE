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
import com.thinh.aistudybuddy.data.network.RetrofitClient
import com.thinh.aistudybuddy.ui.screens.*
import com.thinh.aistudybuddy.ui.theme.screens.ChangePasswordScreen
import com.thinh.aistudybuddy.ui.theme.screens.ForgotPasswordOtpScreen
import com.thinh.aistudybuddy.ui.theme.screens.ForgotPasswordResetScreen
import com.thinh.aistudybuddy.ui.theme.screens.ForgotPasswordScreen
import com.thinh.aistudybuddy.ui.theme.screens.LoginScreen
import com.thinh.aistudybuddy.ui.theme.screens.RegisterScreen
import com.thinh.aistudybuddy.viewmodel.ChatViewModel
import com.thinh.aistudybuddy.viewmodel.QuizViewModel
import com.thinh.aistudybuddy.viewmodel.StudyPlanViewModel
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
                val destination = if (RetrofitClient.authToken.isNullOrBlank()) "login" else "chat"
                navController.navigate(destination) { popUpTo("welcome") { inclusive = true } }
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
                onForgotPasswordClick = { navController.navigate("forgot_password") },
                onBackClick = { navController.navigate("welcome") }
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
                onStartQuiz = { navController.navigate("quiz") },
                onAccountClick = { navController.navigate("account") },
                onSettingsClick = { navController.navigate("settings") },
                onStudyPlanClick = {
                    navScope.launch {
                        val conversationId = if (chatViewModel.activeConversationId.isBlank()) {
                            chatViewModel.ensureActiveConversationForStudyPlan()
                        } else {
                            chatViewModel.activeConversationId
                        }
                        if (conversationId.isNullOrBlank()) return@launch
                        studyViewModel.updateLessonConversationId(conversationId)
                        navController.navigate("study_plan")
                    }
                },
                onConversationHistoryClick = { conversationId -> navController.navigate("conversation_history/$conversationId") },
                onSessionExpired = forceLogin,
                viewModel = chatViewModel
            )
        }
        composable("study_plan") {
            StudyPlanScreen(
                onBack = { navController.popBackStack() },
                onLearnClick = { lesson ->
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
                studyViewModel = studyViewModel
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
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable(
            route = "conversation_history/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty()
            ConversationHistoryScreen(
                conversationId = conversationId,
                onBack = { navController.popBackStack() },
                onOpenConversation = { conversationId ->
                    chatViewModel.selectConversation(conversationId)
                    navController.popBackStack()
                },
                onOpenLesson = { planRawJson, lessonId ->
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
                timelineStatusByDocumentId = studyViewModel.timeline.associate { it.documentId to it.status },
                onRefresh = { conversationId ->
                    chatViewModel.refreshConversationMessages(conversationId)
                    studyViewModel.refreshProgressTimeline()
                },
                onStartQuiz = { message ->
                    val quizJson = message.planJson
                    if (!quizJson.isNullOrBlank()) {
                        // Attempt to parse quiz questions from the message's planJson
                        val questions = parseQuizFromJson(quizJson)
                        if (questions.isNotEmpty()) {
                            quizViewModel.loadQuestions(
                                newQuestions = questions,
                                title = "Quiz: ${message.attachmentName ?: "History"}"
                            )
                            navController.navigate("quiz")
                        }
                    } else if (message.showQuizButton) {
                        // Fallback if planJson is missing but it's a quiz message
                        navController.navigate("quiz")
                    }
                },
                onCheckPlan = { planJson ->
                    if (!planJson.isNullOrBlank()) {
                        studyViewModel.loadStudyPlanFromJson(planJson)
                        studyViewModel.refreshProgressTimeline()
                        navController.navigate("study_plan")
                    }
                },
                chatViewModel = chatViewModel
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
    }
}

private fun parseQuizFromJson(json: String): List<QuizQuestion> {
    return try {
        val gson = Gson()
        val rootMap = gson.fromJson(json, Map::class.java)
        val questionsList = (rootMap["questions"] ?: rootMap["quiz"]) as? List<Map<String, Any>> ?: (if (json.trim().startsWith("[")) gson.fromJson(json, List::class.java) as? List<Map<String, Any>> else null) ?: return emptyList()

        val parsedQuestions = mutableListOf<QuizQuestion>()
        for (idx in questionsList.indices) {
            val item = questionsList[idx]
            
            val questionText = (item["text"] ?: item["question"])?.toString() ?: continue
            
            val optionsRaw = item["options"] as? List<*>
            val optionsList = optionsRaw?.mapNotNull { it?.toString() }.orEmpty()
            if (optionsList.size != 4) continue
            
            val answerRaw = item["answer"]?.toString() ?: "A"
            val answerIndex = when (answerRaw.trim().uppercase()) {
                "A" -> 0
                "B" -> 1
                "C" -> 2
                "D" -> 3
                else -> {
                    val found = optionsList.indexOfFirst { it.equals(answerRaw, ignoreCase = true) }
                    if (found >= 0) found else 0
                }
            }

            parsedQuestions.add(
                QuizQuestion(
                    id = item["id"]?.toString() ?: "hist-$idx",
                    question = questionText,
                    options = optionsList,
                    correctAnswerIndex = answerIndex,
                    hint = item["hint"]?.toString() ?: "Choose the best answer.",
                    explanation = item["explanation"]?.toString() ?: ""
                )
            )
        }
        parsedQuestions
    } catch (e: Exception) {
        android.util.Log.e("AppNavigation", "Failed to parse quiz JSON", e)
        emptyList()
    }
}