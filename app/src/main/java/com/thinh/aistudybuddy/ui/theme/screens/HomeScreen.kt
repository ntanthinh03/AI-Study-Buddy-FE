// ui/screens/HomeScreen.kt
package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Tài liệu của tôi") }) }
    ) { paddingValues ->
        // Ở đây Thinh sẽ dùng LazyColumn để hiện danh sách PDF từ Backend trả về
        Text("Danh sách PDF sẽ hiện ở đây", modifier = Modifier.padding(paddingValues))
    }
}