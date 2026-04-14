package com.thinh.aistudybuddy.ui.theme

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
import com.thinh.aistudybuddy.data.network.RetrofitClient
import com.thinh.aistudybuddy.ui.screens.*
import com.thinh.aistudybuddy.ui.theme.screens.ChangePasswordScreen
import com.thinh.aistudybuddy.ui.theme.screens.ForgotPasswordScreen
import com.thinh.aistudybuddy.ui.theme.screens.LoginScreen
import com.thinh.aistudybuddy.ui.theme.screens.RegisterScreen
import com.thinh.aistudybuddy.viewmodel.ChatViewModel
import com.thinh.aistudybuddy.viewmodel.QuizViewModel
import com.thinh.aistudybuddy.viewmodel.StudyPlanViewModel

@Composable
fun AppNavigation(navController: NavHostController, initialDisplayName: String = "") {
    val studyViewModel: StudyPlanViewModel = viewModel()
    val quizViewModel: QuizViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    var selectedLessonId by remember { mutableStateOf<String?>(null) }
    var displayName by remember { mutableStateOf(initialDisplayName) }
    val forceLogin = {
        chatViewModel.resetForAccountSwitch()
        RetrofitClient.authToken = null
        SessionStore.clearSession(navController.context)
        LocalHistoryStore.clearAll()
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
                    chatViewModel.loadConversationsFromBackend()
                    navController.navigate("chat") {
                        popUpTo("login") { inclusive = true }
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
                }
            )
        }
        composable("chat") {
            LaunchedEffect(RetrofitClient.authToken) {
                if (!RetrofitClient.authToken.isNullOrBlank()) {
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
                onStudyPlanClick = { navController.navigate("study_plan") },
                onConversationHistoryClick = { conversationId -> navController.navigate("conversation_history/$conversationId") },
                onSessionExpired = forceLogin,
                viewModel = chatViewModel
            )
        }
        composable("study_plan") {
            StudyPlanScreen(
                onBack = { navController.popBackStack() },
                onLearnClick = { lesson ->
                    selectedLessonId = lesson.id
                    studyViewModel.markModuleStarted(lesson.documentId.ifBlank { lesson.id })
                    navController.navigate("lesson_learn")
                },
                studyViewModel = studyViewModel
            )
        }
        composable("lesson_learn") {
            val lesson = studyViewModel.activePlan.lessons.find { it.id == selectedLessonId }
            lesson?.let {
                LessonLearnScreen(
                    lesson = it,
                    onBack = { navController.popBackStack() },
                    onStartQuiz = {
                        quizViewModel.setQuizBackendContext(
                            documentId = it.documentId.takeIf { docId -> docId.isNotBlank() },
                            lessonId = it.id,
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
            ConversationHistoryScreen(
                conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty(),
                onBack = { navController.popBackStack() },
                onOpenConversation = { conversationId ->
                    chatViewModel.selectConversation(conversationId)
                    navController.popBackStack()
                },
                onRefresh = { conversationId -> chatViewModel.refreshConversationMessages(conversationId) },
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