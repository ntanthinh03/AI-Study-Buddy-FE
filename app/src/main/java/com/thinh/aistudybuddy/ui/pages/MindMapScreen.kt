package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.MindMapNode
import com.thinh.aistudybuddy.data.models.MindMapResponse
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.MindMapState
import com.thinh.aistudybuddy.viewmodel.MindMapViewModel
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import com.thinh.aistudybuddy.data.models.Flashcard
import com.thinh.aistudybuddy.viewmodel.FlashcardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    documentId: String,
    documentName: String,
    onBack: () -> Unit,
    viewModel: MindMapViewModel,
    flashcardViewModel: FlashcardViewModel
) {
    val state = viewModel.state
    val currentMindMap = viewModel.currentMindMap
    
    var activeNodeForStudy by remember { mutableStateOf<MindMapNode?>(null) }
    var conceptFlashcards by remember { mutableStateOf<List<Flashcard>?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "mindmap_ambient")
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
            initialValue = -30f,
            targetValue = 30f,
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
                    center = Offset(size.width * 0.2f, size.height * 0.3f + floatOffset),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.7f - floatOffset),
                    radius = size.width * 0.6f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Cognitive Mind Map", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(documentName, fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (state) {
                    MindMapState.GENERATING -> {
                        GeneratingMindMapAnimation(viewModel.generatingMessage)
                    }
                    MindMapState.COMPLETED -> {
                        currentMindMap?.let { mindMap ->
                            MindMapContent(
                                nodes = mindMap.content,
                                onNodeClick = { node ->
                                    activeNodeForStudy = node
                                    conceptFlashcards = null
                                }
                            )
                        }
                    }
                    MindMapState.ERROR -> {
                        ErrorState(viewModel.errorMessage ?: "Unknown error") {
                            viewModel.generateMindMap(documentId, "Generate mind map for this document.") {}
                        }
                    }
                    MindMapState.IDLE -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Ready to map document structure", color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }


        activeNodeForStudy?.let { node ->
            InteractiveConceptStudyBoard(
                documentId = documentId,
                node = node,
                flashcardViewModel = flashcardViewModel,
                conceptFlashcards = conceptFlashcards,
                onFlashcardsLoaded = { cards ->
                    conceptFlashcards = cards
                },
                onDismiss = {
                    activeNodeForStudy = null
                    conceptFlashcards = null
                }
            )
        }
    }
}

@Composable
fun GeneratingMindMapAnimation(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "mindmap_anim")
        
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(PrimaryNeonTeal, TertiaryCosmicIndigo, PrimaryNeonTeal)
                    ),
                    radius = size.minDimension / 2 * scale,
                    alpha = 0.3f
                )
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .glassCard(shape = CircleShape, backgroundColor = SurfaceCardContainer.copy(alpha = 0.8f))
                    .cyberBorder(shape = CircleShape, borderWidth = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = PrimaryNeonTeal
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = PrimaryNeonTeal, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI COGNITIVE GRAPH GENERATOR",
                color = PrimaryNeonTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.8f).height(6.dp).clip(CircleShape),
            color = PrimaryNeonTeal,
            trackColor = SurfaceContainerLowest
        )
    }
}

@Composable
fun MindMapContent(nodes: List<MindMapNode>, onNodeClick: (MindMapNode) -> Unit) {
    val rootNodes = nodes.filter { it.parentId == null }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        items(rootNodes) { root ->
            MindMapBranch(root, nodes, 0, onNodeClick)
        }
    }
}

@Composable
fun MindMapBranch(node: MindMapNode, allNodes: List<MindMapNode>, depth: Int, onNodeClick: (MindMapNode) -> Unit) {
    val children = allNodes.filter { it.parentId == node.id }
    
    Column(modifier = Modifier.padding(start = (depth * 20).dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (depth > 0) {
                Canvas(modifier = Modifier.width(16.dp).height(32.dp)) {
                    drawLine(
                        color = PrimaryNeonTeal.copy(alpha = 0.4f),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }
            }
            
            val (nodeBg, nodeBorder) = when (depth) {
                0 -> Pair(PrimaryNeonTeal.copy(alpha = 0.15f), PrimaryNeonTeal)
                1 -> Pair(TertiaryCosmicIndigo.copy(alpha = 0.15f), TertiaryCosmicIndigo)
                else -> Pair(SurfaceCardContainer.copy(alpha = 0.6f), Color.White.copy(alpha = 0.15f))
            }

            Box(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .glassCard(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = nodeBg,
                        borderColor = nodeBorder
                    )
                    .clickable { onNodeClick(node) }
            ) {
                Text(
                    text = node.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = if (depth == 0) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = if (depth == 0) 15.sp else 13.sp
                )
            }
        }
        
        children.forEach { child ->
            MindMapBranch(child, allNodes, depth + 1, onNodeClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveConceptStudyBoard(
    documentId: String,
    node: MindMapNode,
    flashcardViewModel: FlashcardViewModel,
    conceptFlashcards: List<Flashcard>?,
    onFlashcardsLoaded: (List<Flashcard>) -> Unit,
    onDismiss: () -> Unit
) {
    val isGenerating = flashcardViewModel.isGenerating
    var cardIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "card_flip_rotation"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        containerColor = DeepSpaceBackground,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(16.dp)
            .glassCard(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = SurfaceCardContainer.copy(alpha = 0.95f),
                borderColor = PrimaryNeonTeal
            )
            .cyberBorder(shape = RoundedCornerShape(24.dp), borderWidth = 2.dp),
        confirmButton = {},
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONCEPT STUDY BOARD",
                        color = PrimaryNeonTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = node.label,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryNeonTeal)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "AI Study Buddy is analyzing PDF...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Text(
                            "Synthesizing active recall cards...",
                            color = PrimaryNeonTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (conceptFlashcards == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryNeonTeal,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Extract key facts from this subtopic and study them using 3D interactive flashcards.",
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                flashcardViewModel.generateFlashcardsByTopic(documentId, node.label) { result ->
                                    if (result != null) {
                                        onFlashcardsLoaded(result)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.dp)
                        ) {
                            Text("COMPILE CONCEPT CARDS", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    if (conceptFlashcards.isEmpty()) {
                        Text(
                            text = "AI couldn't generate cards for this specific concept. Try another subtopic!",
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val card = conceptFlashcards[cardIndex]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                                    .graphicsLayer {
                                        rotationY = rotation
                                        cameraDistance = 8 * density
                                    }
                                    .clickable { isFlipped = !isFlipped }
                                    .glassCard(
                                        shape = RoundedCornerShape(16.dp),
                                        backgroundColor = if (rotation <= 90f) SurfaceCardContainer.copy(alpha = 0.8f) else TertiaryCosmicIndigo.copy(alpha = 0.2f),
                                        borderColor = if (rotation <= 90f) PrimaryNeonTeal.copy(alpha = 0.5f) else TertiaryCosmicIndigo
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (rotation <= 90f) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "FRONT (Tap to flip)",
                                            color = PrimaryNeonTeal.copy(alpha = 0.6f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = card.front,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .graphicsLayer {
                                                rotationY = 180f
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "BACK (Answer / Definition)",
                                            color = TertiaryCosmicIndigo.copy(alpha = 0.8f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = card.back,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (cardIndex > 0) {
                                            cardIndex--
                                            isFlipped = false
                                        }
                                    },
                                    enabled = cardIndex > 0,
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardContainer)
                                ) {
                                    Text("Prev", color = Color.White)
                                }
                                
                                Text(
                                    text = "CARD ${cardIndex + 1}/${conceptFlashcards.size}",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = {
                                        if (cardIndex < conceptFlashcards.size - 1) {
                                            cardIndex++
                                            isFlipped = false
                                        }
                                    },
                                    enabled = cardIndex < conceptFlashcards.size - 1,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal)
                                ) {
                                    Text("Next", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = RoseWarning, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(message, color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
            modifier = Modifier.cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.dp)
        ) {
            Text("Retry Generation", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
