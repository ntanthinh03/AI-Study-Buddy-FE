@file:Suppress("unused", "UNUSED_VALUE")

package com.thinh.aistudybuddy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.models.Conversation
import com.thinh.aistudybuddy.data.models.ConversationKind
import com.thinh.aistudybuddy.ui.theme.*

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
    onStudyRoomClick: () -> Unit,
    onVersusArenaClick: () -> Unit
) {

    ModalDrawerSheet(
        drawerContainerColor = DeepSpaceBackground,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .width(320.dp)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.2f), Color.Transparent)
                ),
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Search conversations...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PrimaryNeonTeal
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = SurfaceContainerLowest.copy(alpha = 0.6f),
                    unfocusedContainerColor = SurfaceContainerLowest.copy(alpha = 0.3f),
                    focusedBorderColor = PrimaryNeonTeal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))


            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                if (searchQuery.isBlank() || "New Chat".contains(searchQuery, ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(PrimaryNeonTeal, TertiaryCosmicIndigo)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onNewChatClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "NEW CHAT",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }


                if (searchQuery.isBlank() || "Daily Study Session".contains(searchQuery, ignoreCase = true)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(12.dp))
                            .clickable { onDailySessionClick() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = SecondaryTangerine,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Daily Study Session",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(SecondaryTangerine, Color(0xFFFF5722))
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "STREAK",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }


                if (searchQuery.isBlank() || "Co-Study Rooms".contains(searchQuery, ignoreCase = true)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(12.dp))
                            .clickable { onStudyRoomClick() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = PrimaryNeonTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Co-Study Rooms",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    color = PrimaryNeonTeal.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "LIVE",
                                color = PrimaryNeonTeal,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }


                val itemsList = listOf(
                    Triple("Flashcards Study", Icons.Default.ViewCarousel, onFlashcardsClick),
                    Triple("Study Analytics", Icons.Default.Insights, onAnalyticsClick),
                    Triple("Focus Timer", Icons.Default.Timer, onFocusClick),
                    Triple("Mock Exam", Icons.AutoMirrored.Filled.MenuBook, onMockExamClick),
                    Triple("Solo 1 vs 1 Arena", Icons.Default.SportsEsports, onVersusArenaClick),
                    Triple("Leaderboard & Ranks", Icons.Default.EmojiEvents, onLeaderboardClick)
                )

                itemsList.forEach { (label, icon, action) ->
                    if (searchQuery.isBlank() || label.contains(searchQuery, ignoreCase = true)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { action() }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))


            Text(
                text = "RECENT CONVERSATIONS",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )


            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                if (conversations.isEmpty() && pendingConversations.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No active conversations",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Launch a new RAG chat to begin.",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {

                    items(conversations) { conversation ->
                        val isActive = conversation.id == activeConversationId

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
                                else -> "General Chat"
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isActive) {
                                        Modifier.cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                                .glassCard(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = if (isActive) Color(0x3300E5FF) else Color(0x0DFFFFFF),
                                    borderColor = Color.Transparent
                                )
                                .clickable { onConversationSelected(conversation.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = null,
                                    tint = if (isActive) PrimaryNeonTeal else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = displayTitle,
                                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = typeLabel,
                                        color = if (isActive) PrimaryNeonTeal.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteConversationRequested(conversation) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = RoseWarning.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }


                    if (pendingConversations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "PENDING...",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        items(pendingConversations) { conversation ->
                            val isActive = conversation.id == activeConversationId
                            val typeIcon = when (conversation.kind) {
                                ConversationKind.QUIZ -> Icons.Default.Quiz
                                ConversationKind.PLAN -> Icons.Default.School
                                ConversationKind.CHAT -> if (conversation.documentId != null) Icons.Default.Description else Icons.Default.ChatBubble
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassCard(
                                        shape = RoundedCornerShape(12.dp),
                                        backgroundColor = if (isActive) Color(0x3300E5FF) else Color(0x05FFFFFF),
                                        borderColor = Color.Transparent
                                    )
                                    .clickable { onConversationSelected(conversation.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Syncing with cloud...",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Pending",
                                            color = Color.White.copy(alpha = 0.2f),
                                            fontSize = 10.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteConversationRequested(conversation) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = RoseWarning.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = SurfaceCardContainer.copy(alpha = 0.7f)
                    )
                    .clickable { onAccountClick() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    size = 40.dp,
                    onClick = onAccountClick
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = userDisplayName.ifBlank { "Study Buddy" },
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
