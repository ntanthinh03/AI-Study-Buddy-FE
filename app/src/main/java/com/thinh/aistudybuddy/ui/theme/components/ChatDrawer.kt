package com.thinh.aistudybuddy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.model.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDrawer(
    searchQuery: String,
    conversations: List<Conversation>,
    activeConversationId: String,
    onNewChatClick: () -> Unit,
    onConversationSelected: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

    if (showDeleteDialog && conversationToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Chat", color = Color.White) },
            text = { Text("Are you sure you want to delete this conversation?", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConversation(conversationToDelete!!.id)
                    showDeleteDialog = false
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF1E1E1E),
        drawerShape = RoundedCornerShape(0.dp),
        modifier = Modifier.width(320.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search for chats", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2C2E), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF2C2C2E),
                    focusedContainerColor = Color(0xFF2C2C2E),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNewChatClick() }
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.5.dp, Color.White)
                    ) {}
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("New Chat", color = Color.White, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Chats", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    val isActive = conversation.id == activeConversationId
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { direction ->
                            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                conversationToDelete = conversation
                                showDeleteDialog = true
                            }
                            false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                when {
                                    dismissState.progress > 0f -> Color(0xFFE53935) // Material Red
                                    else -> Color.Transparent
                                }, label = "swipe_color"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, RoundedCornerShape(24.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (dismissState.progress > 0.1f) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    ) {
                        Surface(
                            color = if (isActive) Color(0xFF3A3A3C) else Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConversationSelected(conversation.id) }
                        ) {
                            Text(
                                text = conversation.title,
                                color = if (isActive) Color.White else Color.LightGray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}