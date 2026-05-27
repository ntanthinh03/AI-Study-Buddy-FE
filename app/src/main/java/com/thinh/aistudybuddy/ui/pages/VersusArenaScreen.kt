package com.thinh.aistudybuddy.ui.pages

import android.widget.Toast
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
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.BackendQuizQuestion
import com.thinh.aistudybuddy.data.models.VersusAnswerSubmitRequest
import com.thinh.aistudybuddy.data.models.VersusMatchResponse
import com.thinh.aistudybuddy.services.network.RetrofitClient
import com.thinh.aistudybuddy.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersusArenaScreen(
    matchId: String,
    isGuest: Boolean = false,
    onGameFinished: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var match by remember { mutableStateOf<VersusMatchResponse?>(null) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoadingNextChunk by remember { mutableStateOf(false) }


    var timeLeft by remember { mutableStateOf(30) }
    var timerActive by remember { mutableStateOf(false) }


    var opponentProgressText by remember { mutableStateOf("Opponent analyzing...") }
    var opponentAnsweredThisQuestion by remember { mutableStateOf(false) }


    var showQuitDialog by remember { mutableStateOf(false) }
    var activeWarnings by remember { mutableStateOf(0) }


    LaunchedEffect(showQuitDialog) {
        if (showQuitDialog) {
            runCatching {
                val resp = RetrofitClient.instance.getVersusLockoutStatus()
                if (resp.isSuccessful && resp.body() != null) {
                    activeWarnings = resp.body()!!.warningsCount
                }
            }
        }
    }


    suspend fun pollForNextQuestions() {
        isLoadingNextChunk = true
        var retries = 0
        val oldTotal = match?.questions?.size ?: 0
        while (retries < 30) {
            delay(2000)
            val resp = runCatching { RetrofitClient.instance.getVersusLobbyStatus(matchId) }.getOrNull()
            if (resp != null && resp.isSuccessful) {
                val updatedMatch = resp.body()
                if (updatedMatch != null) {
                    if (updatedMatch.status == "COMPLETED") {
                        match = updatedMatch
                        isLoadingNextChunk = false
                        onGameFinished(matchId)
                        return
                    }
                    if (updatedMatch.questions.size > oldTotal) {
                        match = updatedMatch
                        isLoadingNextChunk = false
                        currentQuestionIndex++
                        timerActive = true
                        return
                    }
                }
            }
            retries++
        }
        isLoadingNextChunk = false
        onGameFinished(matchId)
    }


    fun submitAnswer(optionKey: String) {
        if (isSubmitting || match == null) return
        isSubmitting = true
        timerActive = false

        val timeTaken = (30 - timeLeft).toDouble()

        scope.launch {
            val success = runCatching {
                val resp = if (isGuest) {
                    RetrofitClient.instance.submitVersusOpponentAnswer(
                        matchId,
                        VersusAnswerSubmitRequest(currentQuestionIndex, optionKey, timeTaken)
                    )
                } else {
                    RetrofitClient.instance.submitVersusAnswer(
                        matchId,
                        VersusAnswerSubmitRequest(currentQuestionIndex, optionKey, timeTaken)
                    )
                }
                if (resp.isSuccessful && resp.body() != null) {
                    match = resp.body()
                    true
                } else false
            }.getOrDefault(false)

            isSubmitting = false

            if (success) {
                val nextIdx = currentQuestionIndex + 1
                val totalQuestions = match?.questions?.size ?: 0
                
                if (nextIdx < totalQuestions) {
                    currentQuestionIndex = nextIdx
                    timerActive = true
                } else {
                    if (match?.status == "COMPLETED") {
                        onGameFinished(matchId)
                    } else {
                        timerActive = false
                        pollForNextQuestions()
                    }
                }
            } else {
                Toast.makeText(context, "Synchronization anomaly. Retrying...", Toast.LENGTH_SHORT).show()
                timerActive = true
            }
        }
    }


    LaunchedEffect(matchId) {
        runCatching {
            val resp = RetrofitClient.instance.getVersusLobbyStatus(matchId)
            if (resp.isSuccessful) {
                match = resp.body()
                timerActive = true
            }
        }.onFailure {
            Toast.makeText(context, "Anomaly establishing gate connection", Toast.LENGTH_SHORT).show()
        }
    }


    LaunchedEffect(currentQuestionIndex, timerActive) {
        if (!timerActive) return@LaunchedEffect
        timeLeft = 30
        while (timeLeft > 0 && timerActive) {
            delay(1000)
            timeLeft -= 1
        }
        if (timeLeft == 0 && timerActive) {
            submitAnswer("TIMEOUT")
        }
    }


    LaunchedEffect(currentQuestionIndex, match?.mode) {
        if (match == null) return@LaunchedEffect
        opponentAnsweredThisQuestion = false

        if (match?.mode == "PVP_LOBBY") {

            opponentProgressText = "Opponent is reading..."
            while (!opponentAnsweredThisQuestion) {
                delay(3000)
                runCatching {
                    val resp = RetrofitClient.instance.getVersusLobbyStatus(matchId)
                    if (resp.isSuccessful && resp.body() != null) {
                        val m = resp.body()!!
                        val opponentAnswersMap = if (isGuest) m.playerAnswers else m.botAnswers
                        if (opponentAnswersMap?.containsKey(currentQuestionIndex.toString()) == true) {
                            opponentAnsweredThisQuestion = true
                            opponentProgressText = "Opponent submitted answer!"
                            match = m
                        }
                    }
                }
            }
        } else {

            opponentProgressText = "Opponent is reading..."
            val botDelay = 3000L + (0..6000).random().toLong()
            delay(botDelay)
            opponentAnsweredThisQuestion = true
            opponentProgressText = "Opponent submitted answer!"
        }
    }

    if (match == null) {
        Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryNeonTeal)
        }
        return
    }

    val currentQuestion = match?.questions?.getOrNull(currentQuestionIndex)


    val myName = if (isGuest) (match?.opponentName ?: "You") else (match?.hostName ?: "You")
    val myScore = if (isGuest) (match?.botScore ?: 0) else (match?.playerScore ?: 0)
    val myCorrectCount = if (isGuest) (match?.botCorrectCount ?: 0) else (match?.playerCorrectCount ?: 0)

    val oppName = if (isGuest) (match?.hostName ?: "Host") else (match?.opponentName ?: "AI Bot")
    val oppElo = if (isGuest) (match?.hostElo ?: 1200) else (match?.opponentElo ?: 1200)
    val oppScore = if (isGuest) (match?.playerScore ?: 0) else (match?.botScore ?: 0)
    val oppCorrectCount = if (isGuest) (match?.playerCorrectCount ?: 0) else (match?.botCorrectCount ?: 0)


    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.3f),
                    radius = size.width * 0.5f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "SYNAPSE CHALLENGE ${currentQuestionIndex + 1} / ${match?.questions?.size ?: 3}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { showQuitDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quit Battle",
                                tint = RoseWarning
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (isLoadingNextChunk) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = PrimaryNeonTeal, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "CALIBRATING COGNITIVE BRIDGES...",
                        color = PrimaryNeonTeal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AI is computing and assembling advanced neural challenges from your document.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (currentQuestion != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.6f))
                            .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.15f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(PrimaryNeonTeal, CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("$myName (You)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text("$myScore PTS", color = PrimaryNeonTeal, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { myCorrectCount.toFloat() / (match?.questions?.size?.toFloat() ?: 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = PrimaryNeonTeal,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))


                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (match?.mode == "BOT") Icons.Default.SmartToy else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = SecondaryTangerine,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("$oppName ($oppElo)", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text("$oppScore PTS", color = SecondaryTangerine, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { oppCorrectCount.toFloat() / (match?.questions?.size?.toFloat() ?: 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = SecondaryTangerine,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Match status: $opponentProgressText",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, tint = if (timeLeft < 10) RoseWarning else PrimaryNeonTeal, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${timeLeft}s left",
                                color = if (timeLeft < 10) RoseWarning else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        
                        Text(
                            text = "+${Math.max(0, Math.round((timeLeft) * (500.0 / 30.0)))} SPEED BONUS",
                            color = PrimaryNeonTeal,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { timeLeft / 30f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = if (timeLeft < 10) RoseWarning else PrimaryNeonTeal,
                        trackColor = Color.White.copy(alpha = 0.05f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))


                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.3f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = currentQuestion.question,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))


                    val options = listOf("A", "B", "C", "D")
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        options.forEach { key ->
                            val text = currentQuestion.options[key].orEmpty()
                            if (text.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassCard(shape = RoundedCornerShape(14.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                        .clickable(enabled = !isSubmitting) {
                                            submitAnswer(key)
                                        }
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(PrimaryNeonTeal.copy(alpha = 0.1f), CircleShape)
                                                .border(1.dp, PrimaryNeonTeal, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = key,
                                                color = PrimaryNeonTeal,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Text(
                                            text = text,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
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

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = RoseWarning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ARENA FORFEIT PROTOCOL",
                        color = RoseWarning,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Aborting a live battle counts as an immediate defeat and issues 1 penalty warning point.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(10.dp), backgroundColor = RoseWarning.copy(alpha = 0.05f))
                            .border(0.5.dp, RoseWarning.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Acquiring 3 warning points will trigger a 6-hour lockout.\n\nYour active warnings: $activeWarnings / 3",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        scope.launch {
                            runCatching {
                                val resp = RetrofitClient.instance.quitVersusMatch(matchId)
                                if (resp.isSuccessful) {
                                    Toast.makeText(context, "Forfeited. Penalty warning issued.", Toast.LENGTH_LONG).show()
                                }
                            }
                            onGameFinished("QUIT")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseWarning, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DISCONNECT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showQuitDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryNeonTeal),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNeonTeal.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("STAY & FIGHT", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceCardContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, RoseWarning.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        )
    }
}
