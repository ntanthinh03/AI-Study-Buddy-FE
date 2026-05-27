package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.Flashcard
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.FlashcardViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FlashcardReviewScreen(
    documentId: String,
    onBackClick: () -> Unit,
    viewModel: FlashcardViewModel
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var rotated by remember { mutableStateOf(false) }

    val allFlashcards = viewModel.flashcards
    val scopedFlashcards = remember(documentId, allFlashcards) {
        if (documentId == "all" || documentId.isBlank()) {
            allFlashcards
        } else {
            allFlashcards.filter { it.documentId == documentId || it.document?.id == documentId }
        }
    }
    val visibleCards = remember(scopedFlashcards) { scopedFlashcards.filter { it.box <= 0 } }
    val learnedCards = remember(scopedFlashcards) { scopedFlashcards.filter { it.box > 0 } }

    LaunchedEffect(Unit) {
        viewModel.loadAllFlashcards()
    }

    LaunchedEffect(visibleCards.size) {
        if (scopedFlashcards.isEmpty()) {
            currentIndex = 0
        } else if (currentIndex >= visibleCards.size) {
            currentIndex = (visibleCards.size - 1).coerceAtLeast(0)
        }
    }


    LaunchedEffect(currentIndex) {
        rotated = false
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "review_ambient")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp)
                        ) {
                            Text(
                                text = "Buddy AI",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(SurfaceCardContainer.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "12",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.OfflineBolt,
                                        contentDescription = "Streak",
                                        tint = SecondaryTangerine,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }


                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, PrimaryNeonTeal, CircleShape)
                                        .background(SurfaceCardContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.align(Alignment.Center).size(20.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            if (scopedFlashcards.isEmpty()) {
                val emptyTitle = when {
                    allFlashcards.isEmpty() -> "No flashcards in the database yet"
                    documentId == "all" || documentId.isBlank() -> "No flashcards in this deck yet"
                    else -> "No flashcards in this repository yet"
                }
                val emptyMessage = when {
                    allFlashcards.isEmpty() -> "Create or generate flashcards first, then come back here to review them."
                    documentId == "all" || documentId.isBlank() -> "This deck is empty right now."
                    else -> "This repository has no flashcards yet."
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.96f, animationSpec = tween(300)),
                        exit = fadeOut(tween(200))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Done",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = emptyTitle,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = emptyMessage,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Return to Memory Bank", fontWeight = FontWeight.Bold)
                        }
                        }
                    }
                }
            } else {
                val reviewFinished = visibleCards.isNotEmpty() && currentIndex >= visibleCards.size

                if (reviewFinished) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "All flashcards reviewed",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (learnedCards.isNotEmpty()) {
                                    "You finished the unremembered cards. ${learnedCards.size} card(s) are already learned."
                                } else {
                                    "You finished this review set. Return to the memory bank or review another deck."
                                },
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = onBackClick,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Return to Memory Bank", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val card = visibleCards.getOrNull(currentIndex)
                    if (card != null) {
                    val rotation by animateFloatAsState(
                        targetValue = if (rotated) 180f else 0f,
                        animationSpec = tween(durationMillis = 400),
                        label = "review_flip"
                    )

                    AnimatedContent(targetState = currentIndex, transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (
                            slideInHorizontally(
                                initialOffsetX = { fullWidth: Int -> direction * fullWidth },
                                animationSpec = tween(durationMillis = 300)
                            ) + fadeIn(animationSpec = tween(durationMillis = 220))
                        ).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth: Int -> -direction * fullWidth },
                                animationSpec = tween(durationMillis = 250)
                            ) + fadeOut(animationSpec = tween(durationMillis = 200))
                        )
                    }, label = "card_transition") { _ ->


                    val rawFileName = card.document?.fileName ?: "Personal Collection"
                    val docDisplayName = rawFileName.substringBeforeLast(".")
                    val categoryHeader = docDisplayName.uppercase()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(12.dp))


                        Text(
                            text = "$categoryHeader • DYNAMIC SRS",
                            color = PrimaryNeonTeal.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = docDisplayName,
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(12.dp))


                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROGRESS",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentIndex + 1}/${visibleCards.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / visibleCards.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryNeonTeal,
                            trackColor = SurfaceContainerLowest
                        )


                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp, max = 420.dp)
                                .padding(vertical = 16.dp)
                                .graphicsLayer {
                                    rotationY = rotation
                                    cameraDistance = 12f * density
                                }
                                .clickable { rotated = !rotated },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .glassCard(
                                        shape = RoundedCornerShape(24.dp),
                                        backgroundColor = SurfaceCardContainer.copy(alpha = 0.6f)
                                    )
                                    .cyberBorder(
                                        shape = RoundedCornerShape(24.dp),
                                        borderWidth = 1.dp,
                                        startColor = PrimaryNeonTeal.copy(alpha = 0.3f),
                                        endColor = TertiaryCosmicIndigo.copy(alpha = 0.3f)
                                    ),
                                color = Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (rotation <= 90f) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "CONCEPTUAL QUERY",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }


                                            Text(
                                                text = card.front,
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 24.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )


                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.TouchApp,
                                                    contentDescription = null,
                                                    tint = PrimaryNeonTeal.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "Tap to reveal depth",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    } else {

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer { rotationY = 180f },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .background(PrimaryNeonTeal.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.OfflineBolt,
                                                    contentDescription = null,
                                                    tint = PrimaryNeonTeal,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "SYNTHESIS EXPLAINED",
                                                    color = PrimaryNeonTeal,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }


                                            Text(
                                                text = card.back,
                                                color = PrimaryNeonTeal,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 22.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )


                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.TouchApp,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "Tap to view query",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .background(RoseWarning.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .border(1.5.dp, RoseWarning.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.submitReview(card.id, false)
                                        if (currentIndex < visibleCards.size - 1) {
                                            currentIndex++
                                        } else {
                                            currentIndex = visibleCards.size
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = RoseWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "NEED REVIEW",
                                        color = RoseWarning,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }


                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp)
                                    .background(PrimaryNeonTeal, RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.submitReview(card.id, true)
                                        if (currentIndex < visibleCards.size - 1) {
                                            currentIndex++
                                        } else {
                                            currentIndex = visibleCards.size
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "GOT IT",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .background(SurfaceCardContainer.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "LEARNED",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (learnedCards.isEmpty()) {
                                        "No flashcards are marked as learned yet."
                                    } else {
                                        "${learnedCards.size} flashcard(s) are marked as learned and will no longer appear in this review set."
                                    },
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    }
                    }
                }
            }
        }
    }
}
