package com.thinh.aistudybuddy.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAccountScreen(onBack: () -> Unit, onLogout: () -> Unit, onChangePassword: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {
        // Space Ambient glows
        val infiniteTransition = rememberInfiniteTransition(label = "profile_ambient")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_pulse"
        )
        val floatOffset by infiniteTransition.animateFloat(
            initialValue = -30f,
            targetValue = 30f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_float"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.2f + floatOffset),
                    radius = size.width * 0.55f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.1f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.8f - floatOffset),
                    radius = size.width * 0.6f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Scholar Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Personal Identity Nodes", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .glassCard(shape = CircleShape, backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                        .cyberBorder(shape = CircleShape, borderWidth = 2.dp, startColor = PrimaryNeonTeal, endColor = TertiaryCosmicIndigo)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.1f), TertiaryCosmicIndigo.copy(alpha = 0.1f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "T9",
                            color = PrimaryNeonTeal,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = Typography.headlineLarge.fontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ThinhNguyen",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Typography.headlineMedium.fontFamily
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Computer Science Student",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(32.dp))

                ProfileSectionTitle("ACTIVE PROJECTS")
                ProfileCard(
                    title = "AI Study Buddy",
                    subtitle = "Final Year Thesis Project • Kotlin Engine",
                    icon = Icons.Default.Psychology
                )

                Spacer(modifier = Modifier.height(24.dp))

                ProfileSectionTitle("ACADEMIC INTERESTS")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InterestTag("ML & Data Science", Modifier.weight(1f))
                    InterestTag("Cloud (AWS)", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InterestTag("UI/UX Design", Modifier.weight(1f))
                    InterestTag("Photography", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(32.dp))

                ProfileSectionTitle("TECH CLASSIFICATIONS")
                TechItem(Icons.Default.Code, "Kotlin Core, Jetpack Compose, NestJS")
                TechItem(Icons.Default.Cloud, "AWS Server Architecture (EC2, RDS)")
                TechItem(Icons.Default.CameraAlt, "Fujifilm X-T4 Visual Capture")

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onChangePassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryCosmicIndigo, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Change Account Password", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .glassCard(shape = RoundedCornerShape(14.dp), backgroundColor = RoseWarning.copy(alpha = 0.08f))
                        .cyberBorder(shape = RoundedCornerShape(14.dp), borderWidth = 1.dp, startColor = RoseWarning.copy(alpha = 0.4f), endColor = RoseWarning.copy(alpha = 0.1f)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("De-authorize (Log Out)", color = RoseWarning, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProfileSectionTitle(text: String) {
    Text(
        text = text,
        color = PrimaryNeonTeal,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
fun ProfileCard(title: String, subtitle: String, icon: ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryNeonTeal.copy(alpha = 0.1f), CircleShape)
                    .border(0.5.dp, PrimaryNeonTeal.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PrimaryNeonTeal, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun InterestTag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .glassCard(shape = RoundedCornerShape(12.dp), backgroundColor = SurfaceContainerHigh.copy(alpha = 0.3f))
            .cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.2f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(text, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun TechItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}