package com.thinh.aistudybuddy.ui.pages

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.BackendQuizQuestion
import com.thinh.aistudybuddy.data.models.VersusMatchResponse
import com.thinh.aistudybuddy.services.network.RetrofitClient
import com.thinh.aistudybuddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersusReviewScreen(
    matchId: String,
    onReturnHome: () -> Unit
) {
    val context = LocalContext.current
    var match by remember { mutableStateOf<VersusMatchResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(matchId) {
        isLoading = true
        runCatching {
            val resp = RetrofitClient.instance.getVersusMatchStatus(matchId)
            if (resp.isSuccessful) {
                match = resp.body()
            }
        }.onFailure {
            Toast.makeText(context, "Error reading post-game metrics", Toast.LENGTH_SHORT).show()
        }
        isLoading = false
    }

    if (isLoading || match == null) {
        Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryNeonTeal)
        }
        return
    }

    val isWin = (match?.playerScore ?: 0) > (match?.botScore ?: 0)
    val isDraw = (match?.playerScore ?: 0) == (match?.botScore ?: 0)

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isWin) EmeraldSuccess else if (isDraw) PrimaryNeonTeal else RoseWarning).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.25f),
                    radius = size.width * 0.7f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Neural Arena Post-Game",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onReturnHome) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Return to Dashboard",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                            .cyberBorder(
                                shape = RoundedCornerShape(20.dp),
                                borderWidth = 1.dp,
                                startColor = if (isWin) EmeraldSuccess else if (isDraw) PrimaryNeonTeal else RoseWarning,
                                endColor = TertiaryCosmicIndigo
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        (if (isWin) EmeraldSuccess else if (isDraw) PrimaryNeonTeal else RoseWarning).copy(alpha = 0.1f),
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (isWin) EmeraldSuccess else if (isDraw) PrimaryNeonTeal else RoseWarning,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isWin) Icons.Default.EmojiEvents else if (isDraw) Icons.Default.SportsEsports else Icons.Default.SentimentVeryDissatisfied,
                                    contentDescription = null,
                                    tint = if (isWin) EmeraldSuccess else if (isDraw) PrimaryNeonTeal else RoseWarning,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isWin) "VICTORY ACHIEVED!" else if (isDraw) "SYNAPSE DRAW!" else "DEFEAT IN ARENA",
                                color = if (isWin) EmeraldSuccess else if (isDraw) PrimaryNeonTeal else RoseWarning,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))


                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${match?.playerScore}",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "  VS  ",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${match?.botScore}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your accuracy: ${match?.playerCorrectCount} / ${match?.questions?.size} • Bot accuracy: ${match?.botCorrectCount} / ${match?.questions?.size}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "SYNAPTIC ANALYSIS & REVIEWS",
                        color = PrimaryNeonTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }


                val questions = match?.questions ?: emptyList()
                itemsIndexed(questions) { index, question ->
                    val playerAns = match?.playerAnswers?.get(index.toString())
                    val botAns = match?.botAnswers?.get(index.toString())

                    val isCorrect = playerAns?.selectedAnswer == question.correctAnswer
                    val accentColor = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)

                    var isExpanded by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Question ${index + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = question.question,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))


                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Your Choice", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${playerAns?.selectedAnswer ?: "None"} (${if (isCorrect) "Correct" else "Wrong"})",
                                        color = accentColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AI Bot Choice", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${botAns?.selectedAnswer ?: "None"} (${if (botAns?.isCorrect == true) "Correct" else "Wrong"})",
                                        color = if (botAns?.isCorrect == true) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!isCorrect) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Correct Answer: ${question.correctAnswer}",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "AI EXPLANATION:",
                                        color = PrimaryNeonTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = question.explanation,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }


                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onReturnHome,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Return to Headquarters", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
