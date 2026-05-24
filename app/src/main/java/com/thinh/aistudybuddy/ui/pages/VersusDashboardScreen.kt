package com.thinh.aistudybuddy.ui.pages

import android.widget.Toast
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.GamificationStats
import com.thinh.aistudybuddy.data.models.VersusHistoryEntry
import com.thinh.aistudybuddy.services.network.RetrofitClient
import com.thinh.aistudybuddy.ui.theme.*
import com.google.gson.JsonParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersusDashboardScreen(
    onBack: () -> Unit,
    onStartBattle: () -> Unit,
    onReviewMatch: (String) -> Unit
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val scope = rememberCoroutineScope()

    var stats by remember { mutableStateOf<GamificationStats?>(null) }
    var historyList by remember { mutableStateOf<List<VersusHistoryEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Dialog states
    var showEditDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf("") }
    var isSavingProfile by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        scope.launch {
            runCatching {
                val statsResp = RetrofitClient.instance.getUserStats()
                if (statsResp.isSuccessful) {
                    stats = statsResp.body()
                    nicknameInput = stats?.arenaName ?: stats?.user?.fullName ?: ""
                }
                val histResp = RetrofitClient.instance.getVersusMatchHistory()
                if (histResp.isSuccessful) {
                    historyList = histResp.body() ?: emptyList()
                }
            }.onFailure {
                Toast.makeText(context, "Error loading neural dashboard data", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val currentElo = stats?.elo ?: 1200
    val winStreak = stats?.versusWinStreak ?: 0

    // Determine ELO Rank
    val (rankName, rankColor, rankBadgeLabel) = when {
        currentElo < 1200 -> Triple("Bronze Novice", Color(0xFFCD7F32), "BRONZE")
        currentElo in 1200..1399 -> Triple("Silver Synapse", Color(0xFFC0C0C0), "SILVER")
        currentElo in 1400..1599 -> Triple("Gold Cerebrum", Color(0xFFFFD700), "GOLD")
        currentElo in 1600..1799 -> Triple("Platinum Pulse", Color(0xFFE5E4E2), "PLATINUM")
        currentElo in 1800..1999 -> Triple("Diamond Cortex", Color(0xFFB9F2FF), "DIAMOND")
        else -> Triple("Master Mind", Color(0xFFFF4500), "MASTER")
    }

    // Decode Base64 avatar image helper
    val decodedAvatarBitmap = remember(stats?.user?.avatar) {
        val base64Str = stats?.user?.avatar
        if (base64Str != null && base64Str.startsWith("data:image")) {
            try {
                val cleanStr = base64Str.substringAfter("base64,")
                val decodedBytes = android.util.Base64.decode(cleanStr, android.util.Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Android Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        // Encode to Base64
                        val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val dataUrl = "data:image/jpeg;base64,$base64Str"
                        
                        val resp = RetrofitClient.instance.updateVersusAvatar(mapOf("avatar" to dataUrl))
                        if (resp.isSuccessful && resp.body() != null) {
                            stats = resp.body()
                            Toast.makeText(context, "Neural Avatar updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to upload avatar.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error uploading photo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {
        // Space Ambient backdrops
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.2f),
                    radius = size.width * 0.6f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Arena", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { loadData() }) {
                            Icon(Icons.Default.Refresh, "Refresh stats", tint = PrimaryNeonTeal)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (isLoading && stats == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryNeonTeal)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Item 1: Personal ELO & Badge Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.5f))
                                .cyberBorder(
                                    shape = RoundedCornerShape(20.dp),
                                    borderWidth = 1.dp,
                                    startColor = rankColor.copy(alpha = 0.4f),
                                    endColor = PrimaryNeonTeal.copy(alpha = 0.2f)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val playerFullName = stats?.arenaName ?: stats?.user?.fullName ?: "ACTIVE DUELIST"
                                val initials = if (playerFullName.isNotBlank()) {
                                    val parts = playerFullName.trim().split(" ")
                                    if (parts.size >= 2) {
                                        (parts.first().take(1) + parts.last().take(1)).uppercase()
                                    } else {
                                        playerFullName.take(2).uppercase()
                                    }
                                } else {
                                    "ME"
                                }

                                // Glowing Avatar Circle with Rank Shield Badge
                                Box(
                                    modifier = Modifier
                                        .size(108.dp)
                                        .clickable { showEditDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Main Avatar circle
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(rankColor.copy(alpha = 0.2f), DeepSpaceBackground),
                                                    radius = 180f
                                                )
                                            )
                                            .border(2.dp, rankColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (decodedAvatarBitmap != null) {
                                            Image(
                                                bitmap = decodedAvatarBitmap,
                                                contentDescription = "User Avatar",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                text = initials,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 32.sp,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }

                                    // Floating mini Shield Badge at bottom-right
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .align(Alignment.BottomEnd)
                                            .background(SurfaceCardContainer, CircleShape)
                                            .border(1.5.dp, rankColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = rankColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Tap to Edit profile action indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showEditDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = playerFullName.uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        tint = PrimaryNeonTeal.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = rankName.uppercase(),
                                    color = rankColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Star, null, tint = rankColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$currentElo ELO",
                                        color = rankColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Divider
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.1f))
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Streak count
                                Row(
                                    modifier = Modifier
                                        .background(
                                            if (winStreak > 0) SecondaryTangerine.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            0.5.dp,
                                            if (winStreak > 0) SecondaryTangerine.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Win Streak",
                                        tint = if (winStreak > 0) SecondaryTangerine else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (winStreak > 0) "$winStreak MATCH WIN STREAK!" else "No win streak active",
                                        color = if (winStreak > 0) SecondaryTangerine else Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // Item 2: Play Button Panel
                    item {
                        Button(
                            onClick = onStartBattle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryNeonTeal,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "LAUNCH BATTLE MATCHMAKER",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // Item 3: Match History Header
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Icon(Icons.Default.History, null, tint = PrimaryNeonTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BATTLE LOGS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Item 4: History list
                    if (historyList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.3f))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Your battle history is clean.",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Complete your first match to ELO sync.",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(historyList) { entry ->
                            val isVictory = entry.resultText.contains("VICTORY")
                            val isDraw = entry.resultText.contains("DRAW")
                            val resultColor = when {
                                isVictory -> PrimaryNeonTeal
                                isDraw -> Color(0xFFC0C0C0)
                                else -> RoseWarning
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                                    .border(
                                        1.dp,
                                        resultColor.copy(alpha = 0.15f),
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(resultColor, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = entry.resultText,
                                                    color = resultColor,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "vs ${entry.opponentName} (${entry.opponentElo})",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = entry.scoreText,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = entry.dateText,
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Mode details & Review button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (entry.mode) {
                                                "PVP_LOBBY" -> "Private PVP Room"
                                                "PVP" -> "Random Matchmaker"
                                                else -> "Cognitive Bot"
                                            },
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Button(
                                            onClick = { onReviewMatch(entry.matchId) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = resultColor.copy(alpha = 0.1f),
                                                contentColor = resultColor
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = "Review Quiz",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
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

        // Custom Profile Customization Portal Dialog
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSavingProfile) showEditDialog = false },
                title = {
                    Text(
                        "Neural Customization Portal",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Re-configure your combat avatar and synaptic alias identifier.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )

                        // Nickname edit field
                        OutlinedTextField(
                            value = nicknameInput,
                            onValueChange = { nicknameInput = it },
                            label = { Text("Arena Nickname") },
                            singleLine = true,
                            enabled = !isSavingProfile,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryNeonTeal,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = PrimaryNeonTeal,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(shape = RoundedCornerShape(8.dp), backgroundColor = PrimaryNeonTeal.copy(alpha = 0.03f))
                                .border(0.5.dp, PrimaryNeonTeal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "⚠️ Synaptic alias modifications are locked for 7 days upon saving to prevent ELO system confusion.",
                                color = PrimaryNeonTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Avatar photo uploader button
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            enabled = !isSavingProfile,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceCardContainer,
                                contentColor = PrimaryNeonTeal
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(1.dp, PrimaryNeonTeal.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Avatar Image", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (nicknameInput.trim().length < 3) {
                                Toast.makeText(context, "Nickname must be at least 3 characters", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSavingProfile = true
                            scope.launch {
                                runCatching {
                                    val resp = RetrofitClient.instance.updateVersusArenaName(mapOf("arenaName" to nicknameInput.trim()))
                                    if (resp.isSuccessful && resp.body() != null) {
                                        stats = resp.body()
                                        Toast.makeText(context, "Nickname customized successfully!", Toast.LENGTH_SHORT).show()
                                        showEditDialog = false
                                    } else {
                                        val errorMsg = resp.errorBody()?.string() ?: ""
                                        val errMsg = if (errorMsg.contains("message")) {
                                            JsonParser().parse(errorMsg).asJsonObject.get("message")?.asString ?: errorMsg
                                        } else {
                                            errorMsg
                                        }
                                        Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                    }
                                }.onFailure {
                                    Toast.makeText(context, "Error updating nickname: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                                isSavingProfile = false
                            }
                        },
                        enabled = !isSavingProfile && nicknameInput.trim() != (stats?.arenaName ?: stats?.user?.fullName ?: ""),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSavingProfile) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        } else {
                            Text("SAVE NICKNAME", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showEditDialog = false },
                        enabled = !isSavingProfile,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = SurfaceCardContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            )
        }
    }
}
