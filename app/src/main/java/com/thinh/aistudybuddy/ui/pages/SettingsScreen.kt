package com.thinh.aistudybuddy.ui.pages

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.thinh.aistudybuddy.services.network.RetrofitClient
import com.thinh.aistudybuddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "settings_ambient")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_pulse"
        )
        val floatOffset by infiniteTransition.animateFloat(
            initialValue = -30f,
            targetValue = 30f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = FastOutSlowInEasing),
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
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.1f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.8f - floatOffset),
                    radius = size.width * 0.6f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("System Control", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Core AI Platform Settings", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            val scope = rememberCoroutineScope()
            var selectedMode by remember { mutableStateOf("BALANCED") }
            var preferredTime by remember { mutableStateOf("20:00") }
            var streakFreezeAvailable by remember { mutableStateOf(1) }
            var isSettingsLoading by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                isSettingsLoading = true
                runCatching {
                    val resp = RetrofitClient.instance.getUserStats()
                    if (resp.isSuccessful) {
                        resp.body()?.let {
                            selectedMode = it.learningMode ?: "BALANCED"
                            preferredTime = it.preferredNotificationTime ?: "20:00"
                            streakFreezeAvailable = it.streakFreezeAvailable ?: 1
                        }
                    }
                }
                isSettingsLoading = false
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                SettingsSectionTitle("LEARNING PACE & PUSH TELEMETRY")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        . glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                        .cyberBorder(shape = RoundedCornerShape(16.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.2f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Pace Mode",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val modes = listOf(
                            Triple("CASUAL", "Casual Pace (0.8x XP, 1 Quiz Goal)", "Streak Freeze: None"),
                            Triple("BALANCED", "Balanced Pace (1.0x XP, 3 Quiz Goal)", "Streak Freeze: 1/week"),
                            Triple("INTENSE", "Intense Pace (1.5x XP, 5 Quiz Goal)", "Streak Freeze: 2/week")
                        )

                        modes.forEach { (mode, desc, freeze) ->
                            val isSelected = selectedMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMode = mode }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedMode = mode },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PrimaryNeonTeal,
                                        unselectedColor = Color.White.copy(alpha = 0.6f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(desc, color = if (isSelected) PrimaryNeonTeal else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(freeze, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = preferredTime,
                            onValueChange = { preferredTime = it },
                            singleLine = true,
                            label = { Text("Preferred Leisure Hour (e.g. 20:00)", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryNeonTeal,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isSettingsLoading = true
                                    val success = runCatching {
                                        val resp = RetrofitClient.instance.updateSettings(
                                            mapOf(
                                                "learningMode" to selectedMode,
                                                "preferredNotificationTime" to preferredTime
                                            )
                                        )
                                        if (resp.isSuccessful) {
                                            resp.body()?.let {
                                                selectedMode = it.learningMode ?: "BALANCED"
                                                preferredTime = it.preferredNotificationTime ?: "20:00"
                                                streakFreezeAvailable = it.streakFreezeAvailable ?: 1
                                            }
                                            true
                                        } else false
                                    }.getOrDefault(false)

                                    if (success) {
                                        Toast.makeText(context, "Pace & Push settings synced!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Pace & Push sync failed.", Toast.LENGTH_SHORT).show()
                                    }
                                    isSettingsLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(45.dp),
                            enabled = !isSettingsLoading
                        ) {
                            Text("Save Pace & Push Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsSectionTitle("SYSTEM PREFERENCES")
                SettingsItem(Icons.Default.Palette, "Interface Appearance", "Dark mode enabled")
                SettingsItem(Icons.Default.Language, "Interface Language", "English")
                SettingsItem(Icons.Default.Notifications, "Push Notifications", "Daily study reminders on")

                Spacer(modifier = Modifier.height(24.dp))

                SettingsSectionTitle("COGNITIVE & CORE SETTINGS")
                SettingsItem(Icons.Default.Psychology, "AI Model", "Ollama (Local LLM)")
                SettingsItem(Icons.Default.Timer, "Study Break Reminder", "Every 2 hours")


                Spacer(modifier = Modifier.height(24.dp))

                SettingsSectionTitle("DOCUMENTATION & IDENTITY")
                SettingsItem(Icons.AutoMirrored.Filled.Help, "Help & Support", "FAQs and troubleshooting")
                SettingsItem(Icons.Default.Info, "App Version", "v1.0.2")
                SettingsItem(Icons.Default.Description, "Terms & Privacy Policy", "View terms and agreements")

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = RoseWarning.copy(alpha = 0.05f))
                        .border(1.dp, RoseWarning.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(RoseWarning.copy(alpha = 0.15f), CircleShape)
                                .border(0.5.dp, RoseWarning.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = RoseWarning, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("System Error & Feedback", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Report crashes or DB sync issues to: ntanthinh03@gmail.com", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "FEBuddy AI System by ThinhNguyen",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = PrimaryNeonTeal,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
    )
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable { }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PrimaryNeonTeal.copy(alpha = 0.1f), CircleShape)
                    .border(0.5.dp, PrimaryNeonTeal.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PrimaryNeonTeal, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
        }
    }
}