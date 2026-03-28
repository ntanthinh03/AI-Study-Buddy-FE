package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.thinh.aistudybuddy.data.model.*
import java.util.UUID

class StudyPlanViewModel : ViewModel() {
    private val _lessons = mutableStateListOf(
        Lesson(
            id = "L1",
            title = "1. Introduction to Arrays",
            description = "Learn about contiguous memory and indexing.",
            content = """
                Arrays are the most fundamental data structure used to store elements of the same type in contiguous memory locations.
                
                Key Concepts:
                1. Fixed Size: Once defined, the size cannot be changed.
                2. Indexing: Access elements using index (starting from 0).
                3. Memory: Elements are stored side-by-side in RAM.
                
                Time Complexity:
                - Access: O(1)
                - Search: O(n)
                - Insertion/Deletion: O(n)
            """.trimIndent(),
            quizQuestions = generateMockQuestions("Arrays"),
            isCompleted = false
        ),
        Lesson(
            id = "L2",
            title = "2. Linked Lists Deep Dive",
            description = "Understanding nodes, pointers, and dynamic memory.",
            content = """
                Linked lists consist of nodes where each node contains data and a reference to the next node in the sequence.
                
                Key Concepts:
                1. Dynamic Size: Can grow or shrink during runtime.
                2. Nodes: Basic building blocks containing data and pointers.
                3. Sequential Access: Must traverse from the head to find an element.
                
                Types:
                - Singly Linked List
                - Doubly Linked List
                - Circular Linked List
            """.trimIndent(),
            quizQuestions = generateMockQuestions("Linked Lists"),
            isCompleted = false
        )
    )

    val activePlan = StudyPlan(
        id = "ds_101",
        title = "Data Structures Mastery",
        lessons = _lessons
    )

    fun updateLessonScore(lessonId: String, correctAnswers: Int): Boolean {
        val index = _lessons.indexOfFirst { it.id == lessonId }
        if (index != -1) {
            val currentBest = _lessons[index].userScore ?: 0
            if (correctAnswers > currentBest) {
                _lessons[index] = _lessons[index].copy(userScore = correctAnswers)
                return true // Trả về true nếu phá kỷ lục
            }
        }
        return false
    }

    fun toggleLessonCompletion(lessonId: String) {
        val index = _lessons.indexOfFirst { it.id == lessonId }
        if (index != -1) {
            val currentStatus = _lessons[index].isCompleted
            _lessons[index] = _lessons[index].copy(isCompleted = !currentStatus)
        }
    }

    private fun generateMockQuestions(topic: String): List<QuizQuestion> {
        return List(10) { i ->
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                question = "Question ${i + 1} regarding $topic: Which statement is true?",
                options = listOf("Option A", "Option B", "Option C", "Option D"),
                correctAnswerIndex = (0..3).random(),
                hint = "Focus on the core concept.",
                explanation = "This is a detailed explanation of why the answer is correct."
            )
        }
    }
}