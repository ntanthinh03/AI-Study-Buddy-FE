package com.thinh.aistudybuddy.ui.examples

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinh.aistudybuddy.data.AskState
import com.thinh.aistudybuddy.viewmodel.ChatViewModel

@Composable
fun AiAskExampleScreen(
    viewModel: ChatViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    var question by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    
    val askState by remember { derivedStateOf { viewModel.getCurrentAskSessionState() } }
    val isProcessing = askState in listOf(AskState.UPLOADING, AskState.PROCESSING)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Ask your question") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing
        )

        if (selectedFileName != null) {
            Text(
                text = "📎 File: $selectedFileName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.directAiAsk(question) },
                modifier = Modifier.weight(1f),
                enabled = question.isNotBlank() && !isProcessing
            ) {
                Text("Ask AI")
            }

            Button(
                onClick = {
                    if (selectedFileUri != null) {
                        viewModel.uploadThenAsk(
                            context = context,
                            fileUri = selectedFileUri!!,
                            question = question,
                            fileName = selectedFileName
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = question.isNotBlank() && selectedFileUri != null && !isProcessing
            ) {
                Text("Ask with File")
            }
        }

        if (viewModel.getCurrentAskSessionState() != AskState.IDLE) {
            Button(
                onClick = { viewModel.retryAsk(if (question.isNotBlank()) question else null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text("Retry (No Re-upload)")
            }
        }

        StateIndicator(state = askState)

        viewModel.errorMessage?.let { error ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "❌ $error",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (askState == AskState.UPLOADING) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Uploading document...", style = MaterialTheme.typography.bodySmall)
        } else if (askState == AskState.PROCESSING) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Processing document...", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StateIndicator(state: AskState) {
    val (icon, text, color) = when (state) {
        AskState.IDLE -> Triple("⭕", "Ready", MaterialTheme.colorScheme.onSurface)
        AskState.UPLOADING -> Triple("📤", "Uploading...", MaterialTheme.colorScheme.primary)
        AskState.PROCESSING -> Triple("⏳", "Processing...", MaterialTheme.colorScheme.primary)
        AskState.COMPLETED -> Triple("✅", "Completed", MaterialTheme.colorScheme.secondary)
        AskState.ERROR -> Triple("⚠️", "Error", MaterialTheme.colorScheme.error)
        AskState.RETRY_READY -> Triple("🔄", "Ready for Retry", MaterialTheme.colorScheme.tertiary)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = "$icon $text",
            modifier = Modifier.padding(12.dp),
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ChatScreenWithAiAsk(
    viewModel: ChatViewModel,
    context: Context
) {
    var showAiAskDialog by remember { mutableStateOf(false) }

    if (showAiAskDialog) {
        AlertDialog(
            title = { Text("Ask AI") },
            text = {
                AiAskExampleScreen(
                    viewModel = viewModel,
                    context = context,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { showAiAskDialog = false }) {
                    Text("Close")
                }
            },
            onDismissRequest = { showAiAskDialog = false }
        )
    }

    Button(onClick = { showAiAskDialog = true }) {
        Text("💬 Ask AI")
    }
}

class AiAskIntegration(
    private val viewModel: ChatViewModel,
    private val context: Context
) {
    fun onFileSelected(uri: Uri, fileName: String, question: String) {
        viewModel.uploadThenAsk(
            context = context,
            fileUri = uri,
            question = question,
            fileName = fileName
        )
    }

    fun onRetryClicked(newQuestion: String? = null) {
        viewModel.retryAsk(newQuestion)
    }

    fun onDirectQuestion(question: String) {
        viewModel.directAiAsk(question)
    }

    fun observeAskState(onStateChanged: (AskState) -> Unit) {
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel, context: Context) {
    var question by remember { mutableStateOf("") }
    val askState = remember { derivedStateOf { viewModel.getCurrentAskSessionState() } }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            items(viewModel.activeMessages) { message ->
                ChatMessageBubble(message = message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type question...") }
            )

            IconButton(
                onClick = { 
                    viewModel.directAiAsk(question)
                    question = ""
                }
            ) {
                Text("Send")
            }

            var showMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showMenu = true }) {
                Text("⋮")
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Ask with File") },
                    onClick = {
                        showMenu = false
                    }
                )
                if (askState.value != AskState.IDLE) {
                    DropdownMenuItem(
                        text = { Text("Retry") },
                        onClick = {
                            viewModel.retryAsk()
                            showMenu = false
                        }
                    )
                }
            }
        }

    }
}

@Composable
fun ChatMessageBubble(message: com.thinh.aistudybuddy.data.model.ChatMessage) {
}

