package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.thinh.aistudybuddy.data.models.Flashcard
import com.thinh.aistudybuddy.viewmodel.FlashcardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    onBackClick: () -> Unit,
    viewModel: FlashcardViewModel = viewModel()
) {
    var isStudyMode by remember { mutableStateOf(false) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAllFlashcards()
        viewModel.loadFlashcardsToReview()
    }

    if (showAddDialog) {
        AddFlashcardDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { front, back ->
                viewModel.addManualFlashcard(front, back)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isStudyMode) "Flashcard Review" else "Flashcards Library",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (isStudyMode) isStudyMode = false else onBackClick() }) {
                        Icon(
                            if (isStudyMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back", 
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            viewModel.error?.let { err ->
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(err, color = Color.Red, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                }
            }

            if (viewModel.isGenerating) {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                        Spacer(Modifier.height(8.dp))
                        Text("Generating flashcards with AI...", color = Color(0xFF00E5FF), fontSize = 14.sp)
                    }
                }
            }

            if (viewModel.isLoading && !viewModel.isGenerating) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                }
            } else if (isStudyMode && viewModel.flashcardsToReview.isNotEmpty()) {
                StudyModeContent(
                    flashcards = viewModel.flashcardsToReview,
                    currentIndex = currentCardIndex,
                    onNext = { isCorrect ->
                        viewModel.submitReview(viewModel.flashcardsToReview[currentCardIndex].id, isCorrect)
                        if (currentCardIndex < viewModel.flashcardsToReview.size - 1) {
                            currentCardIndex++
                        } else {
                            isStudyMode = false
                        }
                    }
                )
            } else {
                FlashcardManagerContent(
                    flashcards = viewModel.flashcards,
                    reviewCount = viewModel.flashcardsToReview.size,
                    onStartStudy = {
                        if (viewModel.flashcardsToReview.isNotEmpty()) {
                            currentCardIndex = 0
                            isStudyMode = true
                        }
                    },
                    onAddClick = { showAddDialog = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlashcardManagerContent(
    flashcards: List<Flashcard>,
    reviewCount: Int,
    onStartStudy: () -> Unit,
    onAddClick: () -> Unit
) {
    val groupedCards by remember(flashcards) {
        derivedStateOf {
            flashcards.groupBy { it.document?.fileName ?: "Personal Collection" }
        }
    }

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp) // Use 0 to prevent gaps for "leaking" text
    ) {
        // Summary Card at the top
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (reviewCount > 0) "$reviewCount cards to review" else "Great! You're all caught up",
                        color = if (reviewCount > 0) Color(0xFF00E5FF) else Color(0xFF4CAF50),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Spaced repetition helps you remember better",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = onStartStudy,
                        enabled = reviewCount > 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF2C2C2E),
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Review Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section Title Header (Inside LazyColumn)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212)) // Match screen background to hide items scrolling behind
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your Collections (${groupedCards.size})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onAddClick) {
                        Text("+ Add Card", color = Color(0xFF00E5FF))
                    }
                }
            }
        }

        // Grouped Collections
        groupedCards.forEach { (groupName, cards) ->
            val isExpanded = expandedStates[groupName] ?: false
            
            stickyHeader(key = groupName) {
                // Surface with solid background to hide items scrolling behind
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = Color(0xFF121212), // Same as screen background
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedStates[groupName] = !isExpanded },
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        groupName, 
                                        color = Color.White, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 16.sp,
                                        maxLines = 1
                                    )
                                    Text("${cards.size} cards", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }

            if (isExpanded) {
                items(cards, key = { "card_${groupName}_${it.id}" }) { card ->
                    FlashcardListItem(card)
                }
                
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { expandedStates[groupName] = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                        ) {
                            Icon(Icons.Default.ExpandLess, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Collapse Collection", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                item {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun FlashcardListItem(card: Flashcard) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        color = Color(0xFF2C2C2E).copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A3C))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(card.front, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF3A3A3C))
            Spacer(Modifier.height(8.dp))
            Text(card.back, color = Color(0xFF00E5FF).copy(alpha = 0.8f), fontSize = 13.sp)
        }
    }
}

@Composable
fun StudyModeContent(
    flashcards: List<Flashcard>,
    currentIndex: Int,
    onNext: (Boolean) -> Unit
) {
    val card = flashcards[currentIndex]
    var rotated by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(durationMillis = 400)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / flashcards.size.coerceAtLeast(1).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF00E5FF),
            trackColor = Color(0xFF1E1E1E)
        )
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            "Card ${currentIndex + 1} of ${flashcards.size}",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { rotated = !rotated },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        Text(
                            text = card.front,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = card.back,
                            color = Color(0xFF00E5FF),
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer { rotationY = 180f }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { rotated = false; onNext(false) },
                modifier = Modifier.weight(1f).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Hard", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { rotated = false; onNext(true) },
                modifier = Modifier.weight(1f).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Easy", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddFlashcardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Flashcard", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (Question/Term)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (Answer/Definition)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (front.isNotBlank() && back.isNotBlank()) onConfirm(front, back) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Add", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}
