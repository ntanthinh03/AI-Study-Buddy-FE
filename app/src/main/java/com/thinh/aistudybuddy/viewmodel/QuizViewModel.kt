package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.thinh.aistudybuddy.data.local.LocalHistoryStore
import com.thinh.aistudybuddy.data.network.RetrofitClient
import com.thinh.aistudybuddy.data.model.QuizQuestion
import com.thinh.aistudybuddy.data.SaveDocumentArtifactRequest
import com.thinh.aistudybuddy.data.SaveLessonQuizRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

class QuizViewModel : ViewModel() {

    enum class QuestionStatus {
        CORRECT, INCORRECT, UNANSWERED
    }
    var isNewRecord by mutableStateOf(false)
    var onQuizComplete: ((Int) -> Unit)? = null
    private val _questions = mutableStateListOf<QuizQuestion>()
    val questions: List<QuizQuestion> get() = _questions

    var currentQuestionIndex by mutableIntStateOf(0)
    var score by mutableIntStateOf(0)
    private var currentSessionId by mutableStateOf(UUID.randomUUID().toString())
    private var currentQuizTitle by mutableStateOf("Quiz Session")
    private var currentDocumentId by mutableStateOf<String?>(null)
    private var currentLessonId by mutableStateOf<String?>(null)

    val userAnswers = mutableStateListOf<Int>()
    val submittedQuestions = mutableStateListOf<Boolean>()

    val currentQuestion: QuizQuestion
        get() = _questions[currentQuestionIndex]

    init {
        val defaultQuestions = listOf(
            QuizQuestion(
                id = "1",
                question = "Which component is responsible for running Java bytecode?",
                options = listOf("JVM", "JDK", "JRE", "JIT"),
                correctAnswerIndex = 0,
                hint = "It's the virtual machine.",
                explanation = "The JVM (Java Virtual Machine) executes the bytecode on different platforms."
            ),
            QuizQuestion(
                id = "2",
                question = "Which of the following is NOT a primitive data type in Java?",
                options = listOf("int", "boolean", "String", "char"),
                correctAnswerIndex = 2,
                hint = "It's a class.",
                explanation = "String is a class in Java, while int, boolean, and char are primitive types."
            ),
            QuizQuestion(
                id = "3",
                question = "Which keyword is used to prevent a class from being inherited?",
                options = listOf("static", "final", "abstract", "private"),
                correctAnswerIndex = 1,
                hint = "Think about the 'final' state.",
                explanation = "The 'final' keyword prevents a class from being subclassed."
            ),
            QuizQuestion(
                id = "4",
                question = "What is the correct signature for the main method?",
                options = listOf("public static void main(String[] args)", "static void main(String args)", "public void main(String[] args)", "public static int main(String[] args)"),
                correctAnswerIndex = 0,
                hint = "Public, static, void...",
                explanation = "The standard entry point is public static void main(String[] args)."
            ),
            QuizQuestion(
                id = "5",
                question = "Which keyword is used for inheritance between classes?",
                options = listOf("implements", "extends", "inherits", "super"),
                correctAnswerIndex = 1,
                hint = "To extend functionality.",
                explanation = "The 'extends' keyword is used to create a subclass from a parent class."
            ),
            QuizQuestion(
                id = "6",
                question = "Which block is used to catch and handle an exception?",
                options = listOf("catch", "try", "finally", "throw"),
                correctAnswerIndex = 0,
                hint = "You 'catch' it.",
                explanation = "The 'catch' block handles the exception thrown in the 'try' block."
            ),
            QuizQuestion(
                id = "7",
                question = "Which access modifier makes a member accessible only within its own class?",
                options = listOf("public", "private", "protected", "default"),
                correctAnswerIndex = 1,
                hint = "Most restrictive.",
                explanation = "The 'private' modifier restricts access to only within the same class."
            ),
            QuizQuestion(
                id = "8",
                question = "What is true about a Java constructor?",
                options = listOf("It must return int", "It has a different name than the class", "It has no return type", "It must be static"),
                correctAnswerIndex = 2,
                hint = "Return type?",
                explanation = "Constructors do not have a return type, not even void."
            ),
            QuizQuestion(
                id = "9",
                question = "Which method is used to compare the content of two String objects?",
                options = listOf("==", "equals()", "compare()", "same()"),
                correctAnswerIndex = 1,
                hint = "Checking equality of values.",
                explanation = "The .equals() method compares the actual character content of strings."
            ),
            QuizQuestion(
                id = "10",
                question = "Which keyword is used to implement an interface in a class?",
                options = listOf("extends", "interface", "uses", "implements"),
                correctAnswerIndex = 3,
                hint = "Implementation.",
                explanation = "The 'implements' keyword is used to implement an interface's methods."
            )
        )
        val restoredSession = LocalHistoryStore.loadLatestQuizSession()
        if (restoredSession != null && restoredSession.questions.isNotEmpty()) {
            restoreQuizSession(restoredSession)
        } else {
            loadQuestions(defaultQuestions)
        }
    }

    fun loadQuestions(
        newQuestions: List<QuizQuestion>,
        documentId: String? = currentDocumentId,
        lessonId: String? = currentLessonId,
        title: String? = currentQuizTitle
    ) {
        currentSessionId = UUID.randomUUID().toString()
        currentQuizTitle = title?.takeIf { it.isNotBlank() } ?: "Quiz Session"
        currentDocumentId = documentId
        currentLessonId = lessonId
        _questions.clear()
        _questions.addAll(newQuestions)

        userAnswers.clear()
        repeat(_questions.size) { userAnswers.add(-1) }

        submittedQuestions.clear()
        repeat(_questions.size) { submittedQuestions.add(false) }

        currentQuestionIndex = 0
        score = 0
        isNewRecord = false
        persistQuizState()
        persistQuizToBackend()
    }

    fun setQuizBackendContext(documentId: String? = null, lessonId: String? = null, title: String? = null) {
        currentDocumentId = documentId?.takeIf { it.isNotBlank() }
        currentLessonId = lessonId?.takeIf { it.isNotBlank() }
        if (!title.isNullOrBlank()) {
            currentQuizTitle = title
        }
    }

    fun selectOption(index: Int) {
        if (!submittedQuestions[currentQuestionIndex]) {
            userAnswers[currentQuestionIndex] = index
            persistQuizState()
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
                persistQuizState()
            }
        }
    }

    fun getQuestionStatus(index: Int): QuestionStatus {
        val answer = userAnswers[index]
        val submitted = submittedQuestions[index]
        return when {
            !submitted || answer == -1 -> QuestionStatus.UNANSWERED
            answer == _questions[index].correctAnswerIndex -> QuestionStatus.CORRECT
            else -> QuestionStatus.INCORRECT
        }
    }

    fun getUnansweredQuestions(): List<Int> {
        val unanswered = mutableListOf<Int>()
        userAnswers.forEachIndexed { index, answer ->
            if (answer == -1) unanswered.add(index + 1)
        }
        return unanswered
    }

    fun nextQuestion() {
        if (currentQuestionIndex < _questions.size - 1) {
            currentQuestionIndex++
            persistQuizState()
        }
    }

    fun previousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
            persistQuizState()
        }
    }

    fun jumpToQuestion(index: Int) {
        currentQuestionIndex = index
        persistQuizState()
    }

    fun resetQuiz() {
        currentQuestionIndex = 0
        score = 0
        for (i in userAnswers.indices) {
            userAnswers[i] = -1
        }
        for (i in submittedQuestions.indices) {
            submittedQuestions[i] = false
        }
        persistQuizState()
    }

    private fun restoreQuizSession(session: com.thinh.aistudybuddy.data.local.CachedQuizSession) {
        currentSessionId = session.sessionId
        currentQuizTitle = session.title
        currentDocumentId = session.documentId

        _questions.clear()
        _questions.addAll(session.questions)

        userAnswers.clear()
        userAnswers.addAll(session.userAnswers.ifEmpty { List(_questions.size) { -1 } })
        if (userAnswers.size < _questions.size) {
            repeat(_questions.size - userAnswers.size) { userAnswers.add(-1) }
        }

        submittedQuestions.clear()
        submittedQuestions.addAll(session.submittedQuestions.ifEmpty { List(_questions.size) { false } })
        if (submittedQuestions.size < _questions.size) {
            repeat(_questions.size - submittedQuestions.size) { submittedQuestions.add(false) }
        }

        currentQuestionIndex = session.currentQuestionIndex.coerceIn(0, (_questions.size - 1).coerceAtLeast(0))
        score = session.score
        isNewRecord = session.isNewRecord
    }

    private fun persistQuizState() {
        if (_questions.isEmpty()) return

        LocalHistoryStore.saveQuizSession(
            LocalHistoryStore.runtimeQuizSession(
                sessionId = currentSessionId,
                title = currentQuizTitle,
                documentId = currentDocumentId,
                questions = _questions.toList(),
                userAnswers = userAnswers.toList(),
                submittedQuestions = submittedQuestions.toList(),
                currentQuestionIndex = currentQuestionIndex,
                score = score,
                isNewRecord = isNewRecord
            )
        )
    }

    fun persistQuizToBackend() {
        if (_questions.isEmpty() || RetrofitClient.authToken.isNullOrBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val quizPayload = buildQuizPayload()
            try {
                when {
                    !currentLessonId.isNullOrBlank() -> {
                        RetrofitClient.instance.saveProgressLessonQuiz(
                            currentLessonId!!,
                            SaveLessonQuizRequest(quiz = quizPayload)
                        )
                    }

                    !currentDocumentId.isNullOrBlank() -> {
                        RetrofitClient.instance.saveDocumentArtifact(
                            currentDocumentId!!,
                            SaveDocumentArtifactRequest(
                                artifactType = "QUIZ",
                                artifact = quizPayload,
                                note = "Quiz saved for the signed-in account"
                            )
                        )
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    RetrofitClient.authToken = null
                }
            } catch (_: IOException) {
            } catch (_: Exception) {
            }
        }
    }

    private fun buildQuizPayload(): JsonElement {
        val payload = JsonObject().apply {
            addProperty("sessionId", currentSessionId)
            addProperty("title", currentQuizTitle)
            addProperty("score", score)
            addProperty("currentQuestionIndex", currentQuestionIndex)
            add("questions", Gson().toJsonTree(_questions))
            add("userAnswers", Gson().toJsonTree(userAnswers.toList()))
            add("submittedQuestions", Gson().toJsonTree(submittedQuestions.toList()))
            currentDocumentId?.let { addProperty("documentId", it) }
            currentLessonId?.let { addProperty("lessonId", it) }
        }
        return payload
    }
}