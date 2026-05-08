package com.thinh.aistudybuddy.ui.theme.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.data.models.MindMapNode
import com.thinh.aistudybuddy.viewmodel.MindMapUiState
import com.thinh.aistudybuddy.viewmodel.MindMapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    documentId: String,
    documentName: String,
    onBack: () -> Unit,
    viewModel: MindMapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(documentId) {
        viewModel.generateMindMap(documentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Mind Map", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(documentName, fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF121212))
        ) {
            when (val state = uiState) {
                is MindMapUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFBB86FC))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating Mind Map...", color = Color.White)
                    }
                }
                is MindMapUiState.Success -> {
                    MindMapContent(state.nodes)
                }
                is MindMapUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.message, color = Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.generateMindMap(documentId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                        ) {
                            Text("Retry")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun MindMapContent(nodes: List<MindMapNode>) {
    val rootNodes = nodes.filter { it.parentId == null }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(rootNodes) { root ->
            MindMapBranch(root, nodes, 0)
        }
    }
}

@Composable
fun MindMapBranch(node: MindMapNode, allNodes: List<MindMapNode>, depth: Int) {
    val children = allNodes.filter { it.parentId == node.id }
    
    Column(modifier = Modifier.padding(start = (depth * 24).dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (depth > 0) {
                Canvas(modifier = Modifier.width(16.dp).height(24.dp)) {
                    drawLine(
                        color = Color.Gray,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }
            
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (depth) {
                        0 -> Color(0xFF6200EE)
                        1 -> Color(0xFF3700B3)
                        else -> Color(0xFF1E1E1E)
                    }
                ),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .border(
                        width = 1.dp,
                        color = if (depth > 1) Color.Gray else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text(
                    text = node.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = if (depth == 0) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (depth == 0) 16.sp else 14.sp
                )
            }
        }
        
        children.forEach { child ->
            MindMapBranch(child, allNodes, depth + 1)
        }
    }
}
