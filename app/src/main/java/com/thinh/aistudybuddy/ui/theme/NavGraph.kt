package com.thinh.aistudybuddy.ui.theme

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thinh.aistudybuddy.ui.screens.ChatScreen
import com.thinh.aistudybuddy.ui.screens.QuizScreen
import com.thinh.aistudybuddy.ui.theme.screens.LoginScreen
import com.thinh.aistudybuddy.ui.theme.screens.RegisterScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "chat"
    ) {
        composable("chat") {
            ChatScreen(
                onProfileClick = { navController.navigate("login") },
                onStartQuiz = { navController.navigate("quiz") }
            )
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("chat") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate("register") },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("quiz") {
            QuizScreen(
                onCloseClick = { navController.popBackStack() }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    // Sau khi đăng ký xong thì về Login để người dùng đăng nhập
                    navController.navigate("login")
                },
                onBackToLogin = {
                    // Quay lại màn hình Login
                    navController.popBackStack()
                },
                onBackToChat = {
                    // Quay thẳng về màn hình Chat
                    navController.navigate("chat") {
                        popUpTo("chat") { inclusive = true }
                    }
                }
            )
        }
    }
}