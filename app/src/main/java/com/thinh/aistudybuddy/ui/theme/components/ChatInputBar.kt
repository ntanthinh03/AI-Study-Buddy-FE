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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChatInputBar(
    inputText: String,
    pendingAttachmentName: String?,
    onInputTextChange: (String) -> Unit,
    onSendMessageClick: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onAddClick: () -> Unit = {}
) {
    val canSend = inputText.isNotBlank() || !pendingAttachmentName.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (!pendingAttachmentName.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp))
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
                    tint = Color.Gray
                )
            }

            TextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = { Text("Ask Buddy...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00E5FF)
                )
            )

            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF303030))
                    .clickable(enabled = canSend) { onSendMessageClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}