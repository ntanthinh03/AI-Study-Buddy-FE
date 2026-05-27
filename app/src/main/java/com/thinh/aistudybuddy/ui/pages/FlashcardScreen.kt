@file:Suppress("UNUSED_PARAMETER")

package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.data.models.Flashcard
import com.thinh.aistudybuddy.viewmodel.FlashcardViewModel
import com.thinh.aistudybuddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    onBackClick: () -> Unit,
    onStartStudy: (String) -> Unit,
    viewModel: FlashcardViewModel = viewModel()
) {
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

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "flashcard_ambient")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_pulse"
        )
        val floatOffset by infiniteTransition.animateFloat(
            initialValue = -35f,
            targetValue = 35f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_float"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.2f + floatOffset),
                    radius = size.width * 0.55f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.8f - floatOffset),
                    radius = size.width * 0.55f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                text = "Memory Bank",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Dynamic Spaced Repetition",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back", 
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
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
                        color = RoseWarning.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                    ) {
                        Text(err, color = RoseWarning, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                    }
                }

                val noticeMessage = viewModel.notice
                AnimatedVisibility(
                    visible = noticeMessage != null,
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(500))
                ) {
                    Surface(
                        color = PrimaryNeonTeal.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNeonTeal.copy(alpha = 0.3f))
                    ) {
                        Text(noticeMessage ?: "", color = PrimaryNeonTeal, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                    }
                }

                if (viewModel.isGenerating) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryNeonTeal)
                            Spacer(Modifier.height(12.dp))
                            Text("AI is forging memory cards...", color = PrimaryNeonTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }


                FlashcardManagerContent(
                    flashcards = viewModel.flashcards,
                    reviewCount = viewModel.flashcardsToReview.size,
                    focusDocumentId = viewModel.focusDocumentId,
                    onStartStudy = {
                        if (viewModel.flashcards.isNotEmpty()) {
                            onStartStudy("all")
                        } else {
                            viewModel.showNotice("No flashcards yet. Generate or add cards first.")
                        }
                    },
                    onAddClick = { showAddDialog = true },
                    onStartStudyRepo = { docId ->
                        onStartStudy(docId)
                    }
                )
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun FlashcardManagerContent(
    flashcards: List<Flashcard>,
    reviewCount: Int,
    focusDocumentId: String? = null,
    onStartStudy: () -> Unit,
    onAddClick: () -> Unit,
    onStartStudyRepo: (String) -> Unit
) {
    val cardsNeedingReview = reviewCount
    val focusedDocumentId = focusDocumentId

    val groupedCards by remember(flashcards) {
        derivedStateOf {
            flashcards.groupBy { it.document?.fileName ?: "Personal Collection" }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp) 
    ) {
        
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.6f))
                    .cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.2f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = null,
                        tint = if (flashcards.isNotEmpty()) PrimaryNeonTeal else EmeraldSuccess,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (flashcards.isNotEmpty()) "${flashcards.size} Cards Available" else "All Cards Synthesized!",
                        color = if (flashcards.isNotEmpty()) PrimaryNeonTeal else EmeraldSuccess,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$cardsNeedingReview cards need review",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Spaced repetition leverages neuroplasticity to anchor knowledge.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = onStartStudy,
                        enabled = flashcards.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeonTeal,
                            contentColor = Color.Black,
                            disabledContainerColor = SurfaceContainerHigh.copy(alpha = 0.3f),
                            disabledContentColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Initiate Core Review", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Knowledge Repositories (${groupedCards.size})",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onAddClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal.copy(alpha = 0.1f), contentColor = PrimaryNeonTeal),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("+ New Card", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        items(groupedCards.entries.toList(), key = { it.key }) { (groupName, cards) ->
            val repoDocumentId = cards.firstOrNull { !it.documentId.isNullOrBlank() }?.documentId
            val docIdToOpen = repoDocumentId ?: "all"
            val toReviewCount = cards.count { it.box <= 0 }
            val learnedCount = cards.count { it.box > 0 }
            val isFocused = focusedDocumentId != null && focusedDocumentId == repoDocumentId

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .glassCard(shape = RoundedCornerShape(18.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.55f))
                    .cyberBorder(
                        shape = RoundedCornerShape(18.dp),
                        borderWidth = if (isFocused) 1.5.dp else 1.dp,
                        startColor = if (isFocused) PrimaryNeonTeal.copy(alpha = 0.35f) else PrimaryNeonTeal.copy(alpha = 0.18f),
                        endColor = if (isFocused) TertiaryCosmicIndigo.copy(alpha = 0.35f) else TertiaryCosmicIndigo.copy(alpha = 0.18f)
                    )
                    .clickable { onStartStudyRepo(docIdToOpen) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = groupName.substringBeforeLast("."),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(RoseWarning.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                    .border(1.dp, RoseWarning.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = RoseWarning,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "$toReviewCount To Review",
                                        color = RoseWarning,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(EmeraldSuccess.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "$learnedCount Learned",
                                        color = EmeraldSuccess,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${cards.size} total cards",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
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
        title = { 
            Text(
                "Forge Flashcard", 
                color = Color.White, 
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (Term / Target Question)", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryNeonTeal,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = SurfaceCardContainer.copy(alpha = 0.5f),
                        unfocusedContainerColor = SurfaceCardContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (Definition / Context)", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryNeonTeal,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = SurfaceCardContainer.copy(alpha = 0.5f),
                        unfocusedContainerColor = SurfaceCardContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (front.isNotBlank() && back.isNotBlank()) onConfirm(front, back) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Forge", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.5f))
            ) {
                Text("Abort")
            }
        },
        containerColor = SurfaceCardContainer,
        modifier = Modifier
            .border(1.dp, PrimaryNeonTeal.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
    )
}
