package com.thinh.aistudybuddy.ui.theme

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thinh.aistudybuddy.ui.screens.*
import com.thinh.aistudybuddy.viewmodel.QuizViewModel
import com.thinh.aistudybuddy.viewmodel.StudyPlanViewModel

@Composable
fun AppNavigation(navController: NavHostController) {
    val studyViewModel: StudyPlanViewModel = viewModel()
    val quizViewModel: QuizViewModel = viewModel()
    var selectedLessonId by remember { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(onFinished = {
                navController.navigate("chat") { popUpTo("welcome") { inclusive = true } }
            })
        }
        composable("chat") {
            ChatScreen(
                onProfileClick = { navController.navigate("account") },
                onStartQuiz = { navController.navigate("quiz") },
                onAccountClick = { navController.navigate("account") },
                onSettingsClick = { navController.navigate("settings") },
                onStudyPlanClick = { navController.navigate("study_plan") }
            )
        }
        composable("study_plan") {
            StudyPlanScreen(
                onBack = { navController.popBackStack() },
                onLearnClick = { lesson ->
                    selectedLessonId = lesson.id
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
                        quizViewModel.onQuizComplete = { score ->
                            studyViewModel.updateLessonScore(it.id, score / 10)
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
        composable("account") { UserAccountScreen(onBack = { navController.popBackStack() }) }
    }
}