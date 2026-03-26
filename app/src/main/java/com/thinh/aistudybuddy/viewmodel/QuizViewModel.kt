package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.thinh.aistudybuddy.data.model.QuizQuestion

class QuizViewModel : ViewModel() {

    enum class QuestionStatus {
        CORRECT, INCORRECT, UNANSWERED
    }

    val sampleQuestions = listOf(
        QuizQuestion(
            id = "1",
            question = "What is the primary goal of Supervised Learning?",
            options = listOf("Finding hidden patterns", "Predicting labeled outputs", "Clustering data", "Reinforcing behavior"),
            correctAnswerIndex = 1,
            hint = "Think about labels.",
            explanation = "Supervised learning uses labeled datasets to predict outcomes."
        ),
        QuizQuestion(
            id = "2",
            question = "Which complexity class does O(n log n) belong to?",
            options = listOf("Linear", "Quadratic", "Log-linear", "Exponential"),
            correctAnswerIndex = 2,
            hint = "Common in Merge Sort.",
            explanation = "O(n log n) is known as log-linear complexity."
        )
    )

    var currentQuestionIndex by mutableIntStateOf(0)
    var score by mutableIntStateOf(0)

    val userAnswers = mutableStateListOf<Int>().apply {
        repeat(10) { add(-1) }
    }

    val submittedQuestions = mutableStateListOf<Boolean>().apply {
        repeat(10) { add(false) }
    }

    val currentQuestion: QuizQuestion
        get() = sampleQuestions[currentQuestionIndex]

    fun selectOption(index: Int) {
        if (!submittedQuestions[currentQuestionIndex]) {
            userAnswers[currentQuestionIndex] = index
        }
    }

    fun submitAnswer() {
        if (!submittedQuestions[currentQuestionIndex]) {
            val selectedIdx = userAnswers[currentQuestionIndex]
            if (selectedIdx != -1) {
                submittedQuestions[currentQuestionIndex] = true
                if (selectedIdx == currentQuestion.correctAnswerIndex) {
                    score += 10
                }
            }
        }
    }

    fun getQuestionStatus(index: Int): QuestionStatus {
        val answer = userAnswers[index]
        val submitted = submittedQuestions[index]
        return when {
            !submitted || answer == -1 -> QuestionStatus.UNANSWERED
            answer == sampleQuestions[index].correctAnswerIndex -> QuestionStatus.CORRECT
            else -> QuestionStatus.INCORRECT
        }
    }

    fun getUnansweredQuestions(): List<Int> {
        val unanswered = mutableListOf<Int>()
        userAnswers.take(sampleQuestions.size).forEachIndexed { index, answer ->
            if (answer == -1) unanswered.add(index + 1)
        }
        return unanswered
    }

    fun nextQuestion() {
        if (currentQuestionIndex < sampleQuestions.size - 1) {
            currentQuestionIndex++
        }
    }

    fun previousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
        }
    }

    fun jumpToQuestion(index: Int) {
        currentQuestionIndex = index
    }
}