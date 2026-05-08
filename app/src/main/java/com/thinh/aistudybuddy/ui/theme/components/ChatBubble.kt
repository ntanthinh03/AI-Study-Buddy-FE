package com.thinh.aistudybuddy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import android.util.Base64
import com.thinh.aistudybuddy.R
import com.thinh.aistudybuddy.data.models.ChatMessage

@Composable
fun ChatBubble(
    message: ChatMessage,
    onStartQuiz: () -> Unit,
    onCheckPlan: (planJson: String?) -> Unit,
    onGenerateFlashcards: (documentId: String) -> Unit,
    onSpeakClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isUser) {
            Image(
                painter = painterResource(id = R.drawable.buddy_logo_png),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (message.isProcessing) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.text,
                            color = Color(0xFFB3E5FC),
                            fontSize = 16.sp
                        )
                        val transition = rememberInfiniteTransition(label = "processing")
                        repeat(3) { index ->
                            val delay = index * 150
                            val scale by transition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(450, delayMillis = delay, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dot"
                            )
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF00E5FF).copy(alpha = scale)))
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onSpeakClick(message.text) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (message.showQuizButton) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start Quiz", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (message.showFlashcardButton && !message.documentId.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onGenerateFlashcards(message.documentId) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.ViewCarousel, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create Flashcards", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (message.showStudyPlanButton) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (message.courses.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            message.courses.forEach { course ->
                                Surface(
                                    color = Color(0xFF2C2C2E),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = course.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            Text(text = "Course", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = { onCheckPlan(message.planJson) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Check Plan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFF2C2C2E),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .widthIn(max = 280.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!message.imageBase64.isNullOrBlank() && !message.imageMimeType.isNullOrBlank()) {
                        ImageBubble(imageBase64 = message.imageBase64!!, mimeType = message.imageMimeType!!)
                    }

                    if (!message.attachmentName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF3A3A3C), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.attachmentName,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }

                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageBubble(imageBase64: String, mimeType: String) {
    val imageBitmap: ImageBitmap? = remember(imageBase64) {
        runCatching {
            val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            bitmap?.asImageBitmap()
        }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Attached image",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF3A3A3C))
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color(0xFF3A3A3C), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Unable to load image", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
