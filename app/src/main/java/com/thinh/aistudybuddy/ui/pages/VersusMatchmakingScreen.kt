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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.Document
import com.thinh.aistudybuddy.data.models.VersusMatchResponse
import com.thinh.aistudybuddy.services.network.RetrofitClient
import com.thinh.aistudybuddy.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersusMatchmakingScreen(
    initialDocumentId: String?,
    onBack: () -> Unit,
    onMatchFound: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current


    var screenState by remember { mutableStateOf("SELECT_MODE") }

    var documents by remember { mutableStateOf<List<Document>>(emptyList()) }
    var selectedDocId by remember { mutableStateOf(initialDocumentId) }
    var selectedMode by remember { mutableStateOf("BOT") }
    var botDifficulty by remember { mutableStateOf("MEDIUM") }


    var isSearching by remember { mutableStateOf(false) }
    var searchStateText by remember { mutableStateOf("Connecting neural synapses...") }
    var matchedName by remember { mutableStateOf("") }
    var matchedElo by remember { mutableStateOf(1200) }
    var showMatchFoundBadge by remember { mutableStateOf(false) }


    var currentMatchId by remember { mutableStateOf<String?>(null) }
    var lobbyRoomCode by remember { mutableStateOf("") }
    var lobbyHostName by remember { mutableStateOf("Host") }
    var lobbyHostElo by remember { mutableStateOf(1200) }
    var lobbyOpponentName by remember { mutableStateOf("Waiting...") }
    var lobbyOpponentElo by remember { mutableStateOf(1200) }
    var lobbyStatus by remember { mutableStateOf("LOBBY") }
    var isHost by remember { mutableStateOf(true) }


    var joinRoomCodeInput by remember { mutableStateOf("") }
    var isJoiningRoom by remember { mutableStateOf(false) }


    var activeLockoutSeconds by remember { mutableStateOf(0L) }
    var warningsCount by remember { mutableStateOf(0) }
    var isCheckingLockout by remember { mutableStateOf(true) }


    LaunchedEffect(Unit) {
        isCheckingLockout = true
        runCatching {
            val resp = RetrofitClient.instance.getVersusLockoutStatus()
            if (resp.isSuccessful && resp.body() != null) {
                warningsCount = resp.body()!!.warningsCount
                if (resp.body()!!.locked) {
                    activeLockoutSeconds = resp.body()!!.remainingSeconds
                }
            }
        }
        isCheckingLockout = false
    }


    LaunchedEffect(activeLockoutSeconds) {
        if (activeLockoutSeconds > 0) {
            var secs = activeLockoutSeconds
            while (secs > 0) {
                delay(1000)
                secs -= 1
                activeLockoutSeconds = secs
            }
        }
    }


    LaunchedEffect(screenState) {
        if (screenState == "SELECT_DOC" && documents.isEmpty()) {
            runCatching {
                documents = RetrofitClient.instance.getDocuments()
            }.onFailure {
                Toast.makeText(context, "Failed to load knowledge files", Toast.LENGTH_SHORT).show()
            }
        }
    }


    LaunchedEffect(currentMatchId, screenState) {
        if (currentMatchId != null && screenState == "LOBBY_ROOM") {
            while (currentMatchId != null && screenState == "LOBBY_ROOM") {
                runCatching {
                    val resp = RetrofitClient.instance.getVersusLobbyStatus(currentMatchId!!)
                    if (resp.isSuccessful && resp.body() != null) {
                        val data = resp.body()!!
                        lobbyStatus = data.status
                        lobbyOpponentName = data.opponentName ?: "Waiting..."
                        lobbyOpponentElo = data.opponentElo ?: 1200
                        lobbyHostName = data.hostName ?: "Host"
                        lobbyHostElo = data.hostElo ?: 1200


                        if (!isHost && (data.status == "GENERATING_INITIAL" || data.status == "IN_PROGRESS")) {
                            onMatchFound(currentMatchId!!, true)
                            return@LaunchedEffect
                        }
                    }
                }
                delay(2000)
            }
        }
    }


    suspend fun triggerRadarSearch(docId: String, mode: String) {
        screenState = "RADAR_SEARCH"
        isSearching = true
        showMatchFoundBadge = false
        searchStateText = "Establishing synaptic connection..."
        delay(1200)
        searchStateText = if (mode == "PVP") {
            "Searching for human duelists in neural web..."
        } else {
            "Analyzing AI training difficulties..."
        }
        delay(1500)
        searchStateText = "Synchronizing cognitive profiles..."
        
        if (mode == "PVP") {
            val names = listOf("Thinh_Nguyen", "Alex_Study", "Elena_KLTN", "StudyJordan_26", "MindPal_Buddy", "Sophia_Scholastic")
            matchedName = names.random()
            matchedElo = 1350 + (0..300).random()
        } else {
            matchedName = when (botDifficulty) {
                "EASY" -> listOf("StudySloth", "SleepyScribbler", "CasualCortex").random()
                "HARD" -> listOf("MasterMind.ai", "CortexCommander", "NeuroEinstein").random()
                else -> listOf("BrainyBuddy", "QuizQuirky", "SynapseSizer").random()
            }
            matchedElo = when (botDifficulty) {
                "EASY" -> 950 + (0..200).random()
                "HARD" -> 1650 + (0..400).random()
                else -> 1250 + (0..250).random()
            }
        }
        
        showMatchFoundBadge = true
        delay(1800)
        searchStateText = "AI generating competitive quiz parameters..."

        val success = runCatching {
            val resp = RetrofitClient.instance.startVersusMatch(
                mapOf(
                    "documentId" to docId,
                    "mode" to mode,
                    "difficulty" to botDifficulty
                )
            )
            if (resp.isSuccessful && resp.body() != null) {
                val matchId = resp.body()!!.id
                onMatchFound(matchId, false)
                true
            } else false
        }.getOrDefault(false)

        if (!success) {
            Toast.makeText(context, "Failed to launch battle session", Toast.LENGTH_SHORT).show()
            isSearching = false
            screenState = "SELECT_MODE"
        }
    }


    fun createLobbyRoom() {
        scope.launch {
            runCatching {
                val resp = RetrofitClient.instance.createVersusRoom()
                if (resp.isSuccessful && resp.body() != null) {
                    val data = resp.body()!!
                    currentMatchId = data.id
                    lobbyRoomCode = data.roomCode ?: ""
                    lobbyHostName = data.hostName ?: "Host"
                    lobbyHostElo = data.hostElo ?: 1200
                    lobbyOpponentName = "Waiting for Guest..."
                    isHost = true
                    screenState = "LOBBY_ROOM"
                } else {
                    Toast.makeText(context, "Neural Room Creation Failed", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun joinLobbyRoom(code: String) {
        if (code.length != 6) {
            Toast.makeText(context, "Enter a valid 6-digit room code", Toast.LENGTH_SHORT).show()
            return
        }
        isJoiningRoom = true
        scope.launch {
            runCatching {
                val resp = RetrofitClient.instance.joinVersusRoom(mapOf("roomCode" to code))
                if (resp.isSuccessful && resp.body() != null) {
                    val data = resp.body()!!
                    currentMatchId = data.id
                    lobbyRoomCode = data.roomCode ?: ""
                    lobbyHostName = data.hostName ?: "Host"
                    lobbyHostElo = data.hostElo ?: 1200
                    lobbyOpponentName = data.opponentName ?: "You"
                    lobbyOpponentElo = data.opponentElo ?: 1200
                    isHost = false
                    screenState = "LOBBY_ROOM"
                } else {
                    Toast.makeText(context, "Room not found or already occupied.", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "Invalid Room Code.", Toast.LENGTH_SHORT).show()
            }
            isJoiningRoom = false
        }
    }


    fun startLobbyMatch(docId: String) {
        if (currentMatchId == null) return
        scope.launch {
            runCatching {
                val resp = RetrofitClient.instance.startVersusRoomMatch(
                    currentMatchId!!,
                    mapOf("documentId" to docId)
                )
                if (resp.isSuccessful) {
                    onMatchFound(currentMatchId!!, false)
                } else {
                    Toast.makeText(context, "Failed to start room battle", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "Error starting battle", Toast.LENGTH_SHORT).show()
            }
        }
    }


    val infiniteTransition = rememberInfiniteTransition(label = "ripples")
    val radarScale1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "ripple1"
    )
    val radarAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "alpha1"
    )
    val radarScale2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            initialStartOffset = StartOffset(1000)
        ),
        label = "ripple2"
    )
    val radarAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            initialStartOffset = StartOffset(1000)
        ),
        label = "alpha2"
    )

    fun formatLockoutTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.4f),
                    radius = size.width * 0.6f
                )
            )
        }

        if (isCheckingLockout) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryNeonTeal)
            }
        } else if (activeLockoutSeconds > 0) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(RoseWarning.copy(alpha = 0.1f), CircleShape)
                        .border(2.dp, RoseWarning, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = RoseWarning, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "NEURAL ARENA LOCKED",
                    color = RoseWarning,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Your access to the Arena has been suspended due to 3 active warnings (mid-game forfeits). Penalties lift automatically.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "LOCKOUT LIFTS IN:",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    formatLockoutTime(activeLockoutSeconds),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Return to Headquarters", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = when (screenState) {
                                        "LOBBY_ROOM" -> "Private Battle Room"
                                        "JOIN_ROOM" -> "Join Arena Room"
                                        "SELECT_DOC" -> "Select Training Data"
                                        "RADAR_SEARCH" -> "Cognitive Matchmaker"
                                        else -> "Versus Battle modes"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text("Dynamic Cognitive Battleground", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                when (screenState) {
                                    "SELECT_MODE" -> onBack()
                                    "BOT_DIFFICULTY" -> screenState = "SELECT_MODE"
                                    "SELECT_DOC" -> {
                                        if (selectedMode == "BOT") screenState = "BOT_DIFFICULTY"
                                        else screenState = "SELECT_MODE"
                                    }
                                    "JOIN_ROOM" -> screenState = "SELECT_MODE"
                                    "LOBBY_ROOM" -> {

                                        currentMatchId = null
                                        screenState = "SELECT_MODE"
                                    }
                                    else -> screenState = "SELECT_MODE"
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
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
                    when (screenState) {
                        "SELECT_MODE" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "SELECT SIMULATION PARAMETER",
                                    color = PrimaryNeonTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )


                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                                        .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.15f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.15f))
                                        .clickable {
                                            selectedMode = "BOT"
                                            screenState = "BOT_DIFFICULTY"
                                        }
                                        .padding(20.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(PrimaryNeonTeal.copy(alpha = 0.1f), CircleShape)
                                                .border(1.dp, PrimaryNeonTeal, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.SmartToy, null, tint = PrimaryNeonTeal, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("VS Bot Cognitive Trainer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Train against scalable AI. Pick custom bot difficulty configurations.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))


                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                                        .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.15f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.15f))
                                        .clickable {
                                            selectedMode = "PVP"
                                            screenState = "SELECT_DOC"
                                        }
                                        .padding(20.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(PrimaryNeonTeal.copy(alpha = 0.1f), CircleShape)
                                                .border(1.dp, PrimaryNeonTeal, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Radar, null, tint = PrimaryNeonTeal, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Global PvP Matchmaker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Instant matchmaking queue. Duel live student avatars globally.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))


                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                                            .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = TertiaryCosmicIndigo.copy(alpha = 0.2f), endColor = PrimaryNeonTeal.copy(alpha = 0.1f))
                                            .clickable {
                                                selectedMode = "LOBBY"
                                                createLobbyRoom()
                                            }
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Group, null, tint = PrimaryNeonTeal, modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("Create Room", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Host custom lobby", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, textAlign = TextAlign.Center)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                                            .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = TertiaryCosmicIndigo.copy(alpha = 0.2f), endColor = PrimaryNeonTeal.copy(alpha = 0.1f))
                                            .clickable {
                                                selectedMode = "LOBBY"
                                                screenState = "JOIN_ROOM"
                                            }
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Lock, null, tint = SecondaryTangerine, modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("Join Room", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Enter lobby code", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }

                        "BOT_DIFFICULTY" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "COGNITIVE BOT CALIBRATION",
                                    color = PrimaryNeonTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    "Determine the AI difficulty settings to modify correctness & speed parameters.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 32.dp)
                                )

                                val difficulties = listOf(
                                    Triple("EASY", "Accuracy ~55% • Delay 6-15s", PrimaryNeonTeal),
                                    Triple("MEDIUM", "Accuracy ~75% • Delay 4-12s", SecondaryTangerine),
                                    Triple("HARD", "Accuracy ~92% • Delay 3-8s", RoseWarning)
                                )

                                difficulties.forEach { (level, desc, color) ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .glassCard(shape = RoundedCornerShape(14.dp), backgroundColor = if (botDifficulty == level) color.copy(alpha = 0.08f) else SurfaceCardContainer.copy(alpha = 0.3f))
                                            .border(1.dp, if (botDifficulty == level) color else Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                            .clickable { botDifficulty = level }
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text(level, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 1.sp)
                                                Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                            }
                                            RadioButton(
                                                selected = botDifficulty == level,
                                                onClick = { botDifficulty = level },
                                                colors = RadioButtonDefaults.colors(selectedColor = color, unselectedColor = Color.White.copy(alpha = 0.2f))
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Button(
                                    onClick = { screenState = "SELECT_DOC" },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("CONFIRM AI CONFIGURATION", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        "SELECT_DOC" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Text(
                                    "CHOOSE COGNITIVE PDF DATA",
                                    color = PrimaryNeonTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    "AI will process this document to generate quiz challenges in real-time.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                )

                                if (documents.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = PrimaryNeonTeal)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(documents) { doc ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        scope.launch {
                                                            triggerRadarSearch(doc.id, selectedMode)
                                                        }
                                                    }
                                                    .padding(16.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(PrimaryNeonTeal.copy(alpha = 0.1f), CircleShape)
                                                            .border(0.5.dp, PrimaryNeonTeal.copy(alpha = 0.2f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Description, null, tint = PrimaryNeonTeal, modifier = Modifier.size(18.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(doc.fileName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                                        Text("Status: ${doc.status}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                                    }
                                                    Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "JOIN_ROOM" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "ENTER PRIVATE LOBBY CODE",
                                    color = PrimaryNeonTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    "Input the 6-digit short code shared by the room creator.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )

                                OutlinedTextField(
                                    value = joinRoomCodeInput,
                                    onValueChange = { if (it.length <= 6) joinRoomCodeInput = it },
                                    label = { Text("Lobby Code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryNeonTeal,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedLabelColor = PrimaryNeonTeal,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                                )

                                Button(
                                    onClick = { joinLobbyRoom(joinRoomCodeInput) },
                                    enabled = !isJoiningRoom && joinRoomCodeInput.length == 6,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    if (isJoiningRoom) {
                                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text("CONNECT TO ROOM", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        "LOBBY_ROOM" -> {
                            val oppActive = lobbyOpponentName != "Waiting..." && lobbyOpponentName != "Waiting for Guest..."
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Box(
                                    modifier = Modifier
                                        .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                                        .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.3f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.1f))
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ROOM ENTRANCE CODE", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(lobbyRoomCode, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            IconButton(onClick = {
                                                clipboardManager.setText(AnnotatedString(lobbyRoomCode))
                                                Toast.makeText(context, "Room Code copied!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.ContentCopy, "Copy", tint = PrimaryNeonTeal)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))


                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                                            .border(1.dp, PrimaryNeonTeal.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(modifier = Modifier.size(40.dp).background(PrimaryNeonTeal.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, null, tint = PrimaryNeonTeal)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(lobbyHostName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                            Text("Elo: $lobbyHostElo", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("HOST", color = PrimaryNeonTeal, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }


                                    val cardBorder = if (oppActive) SecondaryTangerine.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
                                    val iconTint = if (oppActive) SecondaryTangerine else Color.White.copy(alpha = 0.3f)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(modifier = Modifier.size(40.dp).background(iconTint.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, null, tint = iconTint)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(lobbyOpponentName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                            Text(if (oppActive) "Elo: $lobbyOpponentElo" else "Ready to join", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(if (oppActive) "GUEST" else "LOBBY STATUS", color = if (oppActive) SecondaryTangerine else Color.White.copy(alpha = 0.3f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))


                                if (isHost) {
                                    Text(
                                        "SELECT DOCUMENT & LAUNCH",
                                        color = PrimaryNeonTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.5.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    if (documents.isEmpty()) {

                                        LaunchedEffect(Unit) {
                                            runCatching {
                                                documents = RetrofitClient.instance.getDocuments()
                                            }
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = PrimaryNeonTeal)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(documents) { doc ->
                                                val isSelected = selectedDocId == doc.id
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .glassCard(shape = RoundedCornerShape(10.dp), backgroundColor = if (isSelected) PrimaryNeonTeal.copy(alpha = 0.05f) else SurfaceCardContainer.copy(alpha = 0.3f))
                                                        .border(1.dp, if (isSelected) PrimaryNeonTeal else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                                        .clickable { selectedDocId = doc.id }
                                                        .padding(12.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Description, null, tint = if (isSelected) PrimaryNeonTeal else Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(doc.fileName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        val canStart = oppActive && selectedDocId != null
                                        Button(
                                            onClick = { startLobbyMatch(selectedDocId!!) },
                                            enabled = canStart,
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Text("START BATTLE DUEL", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .glassCard(shape = RoundedCornerShape(16.dp))
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.HourglassEmpty, null, tint = PrimaryNeonTeal, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("WAITING FOR HOST...", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 1.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("The room host is currently selecting a cognitive PDF document to sync battle questions.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }

                        "RADAR_SEARCH" -> {

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        drawCircle(
                                            color = PrimaryNeonTeal.copy(alpha = radarAlpha1),
                                            radius = size.width / 2f * radarScale1,
                                            center = center,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                        drawCircle(
                                            color = TertiaryCosmicIndigo.copy(alpha = radarAlpha2),
                                            radius = size.width / 2f * radarScale2,
                                            center = center,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(PrimaryNeonTeal.copy(alpha = 0.1f), CircleShape)
                                            .border(1.5.dp, PrimaryNeonTeal, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Radar, null, tint = PrimaryNeonTeal, modifier = Modifier.size(36.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(48.dp))

                                Text(
                                    text = searchStateText,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                AnimatedVisibility(
                                    visible = showMatchFoundBadge,
                                    enter = fadeIn() + expandVertically()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                                            .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.2f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(PrimaryNeonTeal.copy(alpha = 0.1f), CircleShape)
                                                    .border(1.dp, PrimaryNeonTeal, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (selectedMode == "PVP") Icons.Default.Person else Icons.Default.SmartToy,
                                                    contentDescription = null,
                                                    tint = PrimaryNeonTeal,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column {
                                                Text(matchedName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = if (selectedMode == "PVP") {
                                                        "HUMAN COMBATANT • Elo: $matchedElo"
                                                    } else {
                                                        "AI BOT (${botDifficulty}) • Elo: $matchedElo"
                                                    },
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 12.sp
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
    }
}
