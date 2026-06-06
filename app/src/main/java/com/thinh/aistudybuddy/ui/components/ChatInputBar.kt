package com.thinh.aistudybuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.thinh.aistudybuddy.ui.theme.*

@Composable
fun ChatInputBar(
    inputText: String,
    pendingAttachmentName: String?,
    isListening: Boolean = false,
    isImage: Boolean = false,
    onInputTextChange: (String) -> Unit,
    onSendMessageClick: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onAddClick: () -> Unit = {},
    onMicClick: () -> Unit = {}
) {
    val canSend = inputText.isNotBlank() || !pendingAttachmentName.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (!pendingAttachmentName.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = SurfaceContainerHigh.copy(alpha = 0.8f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isImage) Icons.Default.Image else Icons.Default.Description,
                    contentDescription = null,
                    tint = PrimaryNeonTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = pendingAttachmentName,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemoveAttachment, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove attachment", tint = Color.Gray)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            TextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = { Text("Ask Buddy...", color = Color.White.copy(alpha = 0.4f)) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PrimaryNeonTeal
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (canSend) onSendMessageClick()
                })
            )

            IconButton(onClick = onMicClick) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = "Voice Input",
                    tint = if (isListening) PrimaryNeonTeal else Color.White.copy(alpha = 0.6f)
                )
            }

            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (canSend) {
                            Brush.horizontalGradient(
                                colors = listOf(PrimaryNeonTeal, TertiaryCosmicIndigo)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))
                            )
                        },
                        shape = CircleShape
                    )
                    .clickable(enabled = canSend) { onSendMessageClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.Black else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}