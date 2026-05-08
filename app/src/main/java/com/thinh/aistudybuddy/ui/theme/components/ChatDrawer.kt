@file:Suppress("unused", "UNUSED_VALUE")

package com.thinh.aistudybuddy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.Conversation
import com.thinh.aistudybuddy.data.models.ConversationKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDrawer(
    userDisplayName: String,
    searchQuery: String,
    conversations: List<Conversation>,
    pendingConversations: List<Conversation>,
    activeConversationId: String,
    onNewChatClick: () -> Unit,
    onConversationSelected: (String) -> Unit,
    onDeleteConversationRequested: (Conversation) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAccountClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFlashcardsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onDailySessionClick: () -> Unit,
    onFocusClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onMockExamClick: () -> Unit,
    onStudyRoomClick: () -> Unit
) {

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
                    .clickable { onDailySessionClick() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Daily Study Session", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFlashcardsClick() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ViewCarousel,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Flashcards Study", color = Color.White, modifier = Modifier.weight(1f))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAnalyticsClick() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Study Analytics", color = Color.White, modifier = Modifier.weight(1f))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFocusClick() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Focus Timer", color = Color.White, modifier = Modifier.weight(1f))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMockExamClick() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Mock Exam", color = Color.White, modifier = Modifier.weight(1f))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLeaderboardClick() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Leaderboard & Ranks", color = Color.White, modifier = Modifier.weight(1f))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStudyRoomClick() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Co-Study Rooms", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }

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

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (conversations.isEmpty() && pendingConversations.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No conversations yet",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start a new chat to see it in this drawer.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    if (conversations.isNotEmpty()) {
                        item {
                            Text("Chats", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    items(conversations, key = { it.id }) { conversation ->
                        val isActive = conversation.id == activeConversationId

                        Surface(
                            color = if (isActive) Color(0xFF3A3A3C) else Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val typeIcon = when (conversation.kind) {
                                ConversationKind.QUIZ -> Icons.Default.Quiz
                                ConversationKind.PLAN -> Icons.Default.School
                                ConversationKind.CHAT -> if (conversation.documentId != null) Icons.Default.Description else Icons.Default.ChatBubble
                            }
                            val typeLabel = when (conversation.kind) {
                                ConversationKind.QUIZ -> "Quiz"
                                ConversationKind.PLAN -> "Plan"
                                ConversationKind.CHAT -> "Chat"
                            }
                            val displayTitle = if (conversation.autoTitleApplied && conversation.title.isNotBlank()) {
                                conversation.title
                            } else {
                                when {
                                    conversation.documentId != null -> "Document Chat"
                                    else -> "Chat"
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onConversationSelected(conversation.id) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = null,
                                    tint = if (isActive) Color.White else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = displayTitle,
                                    color = if (isActive) Color.White else Color.LightGray,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = typeLabel, color = Color.Gray, fontSize = 11.sp)
                                IconButton(onClick = {
                                    onDeleteConversationRequested(conversation)
                                }) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (pendingConversations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Pending chats", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        items(pendingConversations, key = { it.id }) { conversation ->
                            val isActive = conversation.id == activeConversationId
                            val typeIcon = when (conversation.kind) {
                                ConversationKind.QUIZ -> Icons.Default.Quiz
                                ConversationKind.PLAN -> Icons.Default.School
                                ConversationKind.CHAT -> if (conversation.documentId != null) Icons.Default.Description else Icons.Default.ChatBubble
                            }

                            Surface(
                                color = if (isActive) Color(0xFF3A3A3C) else Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onConversationSelected(conversation.id) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = null,
                                        tint = if (isActive) Color.White else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Pending chat",
                                        color = if (isActive) Color.White else Color.LightGray,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Pending", color = Color.Gray, fontSize = 11.sp)
                                    IconButton(onClick = {
                                        onDeleteConversationRequested(conversation)
                                    }) {
                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAccountClick() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        size = 36.dp,
                        onClick = onAccountClick
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = userDisplayName.ifBlank { "ThinhNguyen" },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Premium Plan",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.LightGray
                    )
                }
            }
        }
    }
}