package com.thinh.aistudybuddy.ui.theme.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.local.NetworkConfigStore
import com.thinh.aistudybuddy.data.network.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var baseUrl by remember { mutableStateOf(NetworkConfigStore.readBaseUrl(context).orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionTitle("Preferences")
            SettingsItem(Icons.Default.Palette, "Appearance", "Dark mode enabled")
            SettingsItem(Icons.Default.Language, "Language", "English (US)")
            SettingsItem(Icons.Default.Notifications, "Notifications", "All enabled")

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionTitle("AI & Study Settings")
            SettingsItem(Icons.Default.Psychology, "AI Model", "Gemini 1.5 Flash")
            SettingsItem(Icons.Default.Timer, "Study Reminders", "Every 2 hours")

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionTitle("Backend Connection")
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        singleLine = true,
                        label = { Text("Base URL", color = Color.Gray) },
                        placeholder = { Text("http://10.0.2.2:3000", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF2C2C2E),
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF3A3A3C)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val normalized = NetworkConfigStore.normalizeBaseUrl(baseUrl)
                            if (normalized.isNullOrBlank()) {
                                Toast.makeText(context, "Enter a valid base URL or reset to auto.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            NetworkConfigStore.saveBaseUrl(context, normalized)
                            RetrofitClient.setBaseUrlOverride(normalized)
                            Toast.makeText(context, "Base URL saved.", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Save")
                        }

                        OutlinedButton(onClick = {
                            baseUrl = ""
                            NetworkConfigStore.clearBaseUrl(context)
                            RetrofitClient.resetBaseUrlOverride()
                            Toast.makeText(context, "Base URL set to auto.", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Auto")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionTitle("Support & About")
            SettingsItem(Icons.AutoMirrored.Filled.Help, "Help Center", "")
            SettingsItem(Icons.Default.Info, "About Buddy", "Version 1.0.2")
            SettingsItem(Icons.Default.Description, "Terms of Service", "")

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "AI Study Buddy by ThinhNguyen",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = Color(0xFF00E5FF),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
    )
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.DarkGray)
        }
    }
}