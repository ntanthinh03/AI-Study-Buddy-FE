package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.data.models.QuizQuestion
import com.thinh.aistudybuddy.viewmodel.MockExamUiState
import com.thinh.aistudybuddy.viewmodel.MockExamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamScreen(
    onNavigateBack: () -> Unit,
    viewModel: MockExamViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val isLoadingDocs by viewModel.isLoadingDocs.collectAsState()

    val primaryCyan = Color(0xFF00E5FF)
    val darkBg = Color(0xFF0F0F0F)

    var selectedDocId by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mock Exam Simulator", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState is MockExamUiState.InProgress) showExitDialog = true
                        else onNavigateBack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        },
        containerColor = darkBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(darkBg, Color(0xFF1A1A1A)))
            ))

            when (val state = uiState) {
                is MockExamUiState.Initial -> {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = primaryCyan.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, null, tint = primaryCyan) }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Select Material", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("Choose the source for your AI exam", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                        if (isLoadingDocs) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = primaryCyan) }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                items(documents) { doc ->
                                    val isSelected = selectedDocId == doc.id
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable { selectedDocId = doc.id },
                                        color = if (isSelected) primaryCyan.copy(alpha = 0.15f) else Color(0xFF1A1A1A),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, if (isSelected) primaryCyan else Color(0xFF262626))
                                    ) {
                                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PictureAsPdf, null, tint = primaryCyan, modifier = Modifier.size(28.dp))
                                            Spacer(Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(doc.fileName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${doc.createdAt ?: "Recently"}", color = Color.Gray, fontSize = 11.sp)
                                            }
                                            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = primaryCyan)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { selectedDocId?.let { viewModel.startMockExam(20, it) } },
                            enabled = selectedDocId != null,
                            modifier = Modifier.fillMaxWidth().height(64.dp).shadow(if (selectedDocId != null) 12.dp else 0.dp, RoundedCornerShape(20.dp), spotColor = primaryCyan),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryCyan, disabledContainerColor = Color(0xFF2C2C2E)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("START EXAM", fontWeight = FontWeight.ExtraBold, color = if (selectedDocId != null) Color.Black else Color.Gray)
                        }
                    }
                }
                is MockExamUiState.Loading -> PremiumLoadingView("Crafting your unique exam...", primaryCyan)
                is MockExamUiState.InProgress -> InProgressView(state, viewModel, primaryCyan)
                is MockExamUiState.Submitting -> PremiumLoadingView("Grading your exam...", primaryCyan)
                is MockExamUiState.Completed -> CompletedView(state, primaryCyan, onNavigateBack)
                is MockExamUiState.Error -> ErrorView(state.message, onNavigateBack)
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Quit Exam?") },
            text = { Text("Your progress will be lost. Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onNavigateBack()
                }) { Text("EXIT", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("CONTINUE") }
            }
        )
    }
}

@Composable
fun InProgressView(state: MockExamUiState.InProgress, viewModel: MockExamViewModel, primaryColor: Color) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val question = state.questions.getOrNull(currentIndex) ?: return
    val selectedAnswer = viewModel.getSelectedAnswer(currentIndex)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("QUESTION ${currentIndex + 1}", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("${state.questions.size} Questions Total", color = Color.Gray, fontSize = 11.sp)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                CircularProgressIndicator(progress = (currentIndex + 1).toFloat() / state.questions.size, color = primaryColor, trackColor = Color(0xFF1E1E1E), strokeWidth = 4.dp)
                Icon(Icons.Default.Timer, null, tint = primaryColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(32.dp))
        Surface(color = Color(0xFF1A1A1A), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFF262626))) {
            Text(question.question, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(24.dp), lineHeight = 28.sp)
        }
        Spacer(Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            itemsIndexed(question.options) { index, text ->
                val key = when(index) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" }
                val isSelected = selectedAnswer == key
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.selectAnswer(currentIndex, key) },
                    color = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isSelected) primaryColor else Color(0xFF2C2C2E))
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isSelected) primaryColor else Color(0xFF2C2C2E)), contentAlignment = Alignment.Center) {
                            Text(key, color = if (isSelected) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(text, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentIndex > 0) {
                OutlinedButton(onClick = { currentIndex-- }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.Gray)) {
                    Text("PREVIOUS", color = Color.White)
                }
            }
            val isLast = currentIndex == state.questions.size - 1
            Button(
                onClick = { if (isLast) viewModel.submitExam() else currentIndex++ },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isLast) primaryColor else Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isLast) "FINISH" else "NEXT", fontWeight = FontWeight.Bold, color = if (isLast) Color.Black else Color.White)
            }
        }
    }
}

@Composable
fun CompletedView(state: MockExamUiState.Completed, primaryColor: Color, onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(color = primaryColor.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(160.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.correctAnswers}/${state.totalQuestions}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = primaryColor)
                    Text("SCORE", fontSize = 12.sp, color = primaryColor.copy(alpha = 0.7f), letterSpacing = 2.sp)
                }
            }
        }
        Spacer(Modifier.height(48.dp))
        Text("Exam Completed!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("You earned ${state.xpEarned} XP", color = Color.Yellow, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor), shape = RoundedCornerShape(20.dp)) {
            Text("DONE", fontWeight = FontWeight.ExtraBold, color = Color.Black)
        }
    }
}

@Composable
fun PremiumLoadingView(message: String, primaryColor: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(0.8f, 1.2f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(140.dp), color = primaryColor, strokeWidth = 4.dp, trackColor = Color(0xFF1E1E1E))
            Icon(Icons.Default.AutoAwesome, null, tint = primaryColor.copy(alpha = 0.8f), modifier = Modifier.size(60.dp).graphicsLayer { scaleX = scale; scaleY = scale })
        }
        Spacer(Modifier.height(48.dp))
        Text(message, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 32.sp)
        Spacer(Modifier.height(16.dp))
        Text("Our AI is reading your document and generating high-quality questions just for you.", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp))
    }
}
