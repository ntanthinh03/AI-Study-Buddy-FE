package com.thinh.aistudybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.ui.components.*
import com.thinh.aistudybuddy.viewmodel.ChatScreenType
import com.thinh.aistudybuddy.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onProfileClick: () -> Unit,
    onStartQuiz: () -> Unit,
    onAccountClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStudyPlanClick: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    val activeConversation = viewModel.conversations.find { it.id == viewModel.activeConversationId }
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.activeMessages.size, viewModel.isTyping) {
        if (viewModel.activeMessages.isNotEmpty() || viewModel.isTyping) {
            listState.animateScrollToItem(
                if (viewModel.isTyping) viewModel.activeMessages.size else viewModel.activeMessages.size - 1
            )
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chat", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameConversation(viewModel.activeConversationId, newTitle)
                    showRenameDialog = false
                }) { Text("Save", color = Color(0xFF1976D2)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                searchQuery = viewModel.searchQuery,
                conversations = viewModel.filteredConversations,
                activeConversationId = viewModel.activeConversationId,
                onNewChatClick = { viewModel.startNewChat(); scope.launch { drawerState.close() } },
                onConversationSelected = { id -> viewModel.selectConversation(id); scope.launch { drawerState.close() } },
                onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onAccountClick = { scope.launch { drawerState.close() }; onAccountClick() },
                onSettingsClick = { scope.launch { drawerState.close() }; onSettingsClick() }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.currentChatType == ChatScreenType.CONTINUING_CHAT) activeConversation?.title ?: "Chat" else "Buddy",
                        color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null, tint = Color.White) }
                },
                actions = {
                    if (viewModel.currentChatType == ChatScreenType.CONTINUING_CHAT) {
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color(0xFF2C2C2E))) {
                                DropdownMenuItem(text = { Text("Pin", color = Color.White) }, leadingIcon = { Icon(Icons.Default.PushPin, null, tint = Color.White, modifier = Modifier.size(18.dp)) }, onClick = { showMenu = false })
                                DropdownMenuItem(text = { Text("Rename", color = Color.White) }, leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, tint = Color.White, modifier = Modifier.size(18.dp)) }, onClick = { showMenu = false; newTitle = activeConversation?.title ?: ""; showRenameDialog = true })
                                DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp)) }, onClick = { viewModel.deleteConversation(viewModel.activeConversationId); showMenu = false })
                                Divider(color = Color.Gray, thickness = 0.5.dp)
                                DropdownMenuItem(text = { Text("Report a problem", color = Color.White) }, leadingIcon = { Icon(Icons.Default.Report, null, tint = Color.White, modifier = Modifier.size(18.dp)) }, onClick = { showMenu = false })
                            }
                        }
                    } else {
                        Box(modifier = Modifier.padding(end = 12.dp)) { UserAvatar(size = 32.dp, onClick = onProfileClick) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (viewModel.currentChatType == ChatScreenType.NEW_CHAT) {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        BuddyLogo(modifier = Modifier.size(220.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        NewChatView(
                            suggestions = viewModel.suggestions,
                            banner = viewModel.banner,
                            onSuggestionClick = { suggestion ->
                                when (suggestion.title) {
                                    "Summarize PDF" -> viewModel.sendMessage("Imported: ai_research_2026.pdf. Summarize this.")
                                    "Study Plan" -> viewModel.sendMessage("Create a study plan for ${suggestion.subtitle}")
                                    else -> viewModel.sendMessage("Give me tips for ${suggestion.subtitle}")
                                }
                            },
                            onBannerCtaClick = { }
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                        itemsIndexed(viewModel.activeMessages) { _, message ->
                            ChatBubble(message = message, onStartQuiz = onStartQuiz, onCheckPlan = onStudyPlanClick)
                        }
                        if (viewModel.isTyping) {
                            item { TypingIndicator() }
                        }
                    }
                }
            }

            ChatInputBar(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                onSendMessageClick = { if (inputText.isNotBlank()) { viewModel.sendMessage(inputText); inputText = "" } },
                onAddClick = { }
            )
        }
    }
}