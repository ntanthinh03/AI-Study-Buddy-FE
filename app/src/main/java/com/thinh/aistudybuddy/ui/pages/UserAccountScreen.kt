package com.thinh.aistudybuddy.ui.pages

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.ui.components.AuthTextField
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.UserAccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAccountScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: UserAccountViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    val user = viewModel.user

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val base64 = uriToBase64(context, it)
            if (base64 != null) {
                viewModel.updateAvatar(base64)
            } else {
                Toast.makeText(context, "Failed to process image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val avatarBitmap = remember(user?.avatar) {
        user?.avatar?.let { base64Str ->
            try {
                val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    var showChangeDialog by remember { mutableStateOf(false) }
    var changeType by remember { mutableStateOf("") }
    var changeInputVal by remember { mutableStateOf("") }
    var otpInputVal by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {
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
            if (viewModel.isLoading && user == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryNeonTeal)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clickable { imageLauncher.launch("image/*") }
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
                                        colors = listOf(PrimaryNeonTeal.copy(alpha = 0.15f), TertiaryCosmicIndigo.copy(alpha = 0.15f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = avatarBitmap,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = (user?.fullName ?: "S").take(2).uppercase(),
                                    color = PrimaryNeonTeal,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(TertiaryCosmicIndigo, CircleShape)
                                .border(1.dp, PrimaryNeonTeal, CircleShape)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = user?.fullName ?: "Scholar",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = user?.major ?: "Unassigned Field of Study",
                        color = PrimaryNeonTeal.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    viewModel.errorMessage?.let {
                        Text(
                            text = it,
                            color = RoseWarning,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    viewModel.successMessage?.let {
                        Text(
                            text = it,
                            color = EmeraldSuccess,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    ProfileFieldContainer(
                        label = "Full Name",
                        value = user?.fullName ?: "",
                        icon = Icons.Default.Info,
                        onActionClick = null
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileFieldContainer(
                        label = "University Email",
                        value = maskEmail(user?.email ?: ""),
                        icon = Icons.Default.Email,
                        onActionClick = {
                            changeType = "email"
                            changeInputVal = ""
                            otpInputVal = ""
                            viewModel.clearMessages()
                            showChangeDialog = true
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileFieldContainer(
                        label = "Phone Number",
                        value = maskPhone(user?.phoneNumber ?: "None added"),
                        icon = Icons.Default.Phone,
                        onActionClick = {
                            changeType = "phone"
                            changeInputVal = ""
                            otpInputVal = ""
                            viewModel.clearMessages()
                            showChangeDialog = true
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "What are you studying?",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
                    )

                    val majors = listOf("Computer Science", "Business", "Medicine", "Engineering")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MajorSelectorItem(majors[0], user?.major == majors[0], Modifier.weight(1f)) {
                                viewModel.selectMajor(majors[0])
                            }
                            MajorSelectorItem(majors[1], user?.major == majors[1], Modifier.weight(1f)) {
                                viewModel.selectMajor(majors[1])
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MajorSelectorItem(majors[2], user?.major == majors[2], Modifier.weight(1f)) {
                                viewModel.selectMajor(majors[2])
                            }
                            MajorSelectorItem(majors[3], user?.major == majors[3], Modifier.weight(1f)) {
                                viewModel.selectMajor(majors[3])
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

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
                        Text("Log Out", color = RoseWarning, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(16.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SecondaryTangerine,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Report App Error / System Logs",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "ntanthinh03@gmail.com",
                                    color = SecondaryTangerine,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showChangeDialog) {
            Dialog(onDismissRequest = { showChangeDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(shape = RoundedCornerShape(24.dp))
                        .cyberBorder(shape = RoundedCornerShape(24.dp), borderWidth = 1.5.dp)
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (changeType == "email") "Update Email Node" else "Update Phone Node",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Text(
                            text = "We will send a security OTP code to your current email address via Brevo to authorize this update.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        if (!viewModel.otpSentSuccessfully) {
                            AuthTextField(
                                value = changeInputVal,
                                onValueChange = { changeInputVal = it },
                                label = if (changeType == "email") "New University Email" else "New Phone Number",
                                placeholder = if (changeType == "email") "your.name@university.edu" else "+84901234567",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    viewModel.requestOtp(changeType, changeInputVal)
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !viewModel.isOtpSending
                            ) {
                                if (viewModel.isOtpSending) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text("Send Verification Code", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        } else {
                            Text(
                                text = "Code sent to target. Please check inbox.",
                                color = EmeraldSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            AuthTextField(
                                value = otpInputVal,
                                onValueChange = { if (it.length <= 6) otpInputVal = it },
                                label = "Verification Code (6-digit)",
                                placeholder = "Enter 6-digit OTP",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    viewModel.verifyOtp(otpInputVal) {
                                        Toast.makeText(context, "Identity verified successfully!", Toast.LENGTH_SHORT).show()
                                        showChangeDialog = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !viewModel.isOtpVerifying
                            ) {
                                if (viewModel.isOtpVerifying) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text("Verify & Commit Update", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        viewModel.errorMessage?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(it, color = RoseWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Cancel",
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { 
                                    showChangeDialog = false
                                    viewModel.clearMessages()
                                    viewModel.otpSentSuccessfully = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldContainer(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onActionClick: (() -> Unit)?
) {
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
                Icon(icon, null, tint = PrimaryNeonTeal, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(1.dp))
                Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (onActionClick != null) {
                Box(
                    modifier = Modifier
                        .glassCard(shape = RoundedCornerShape(10.dp), backgroundColor = PrimaryNeonTeal.copy(alpha = 0.08f))
                        .cyberBorder(shape = RoundedCornerShape(10.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.4f), endColor = PrimaryNeonTeal.copy(alpha = 0.1f))
                        .clickable { onActionClick() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Change", color = PrimaryNeonTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MajorSelectorItem(title: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.5.dp)
                } else {
                    Modifier.glassCard(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = SurfaceCardContainer.copy(alpha = 0.3f),
                        borderColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) PrimaryNeonTeal else Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

private fun uriToBase64(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } else null
    } catch (e: Exception) {
        null
    }
}

private fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return email
    val local = parts[0]
    val domain = parts[1]
    if (local.length <= 4) {
        return if (local.isNotEmpty()) "${local[0]}***@$domain" else email
    }
    val prefix = local.take(2)
    val suffix = local.takeLast(2)
    return "$prefix*****$suffix@$domain"
}

private fun maskPhone(phone: String): String {
    val cleanPhone = phone.trim()
    if (cleanPhone.isEmpty() || cleanPhone == "None added") return cleanPhone
    if (cleanPhone.length <= 5) return cleanPhone
    val prefix = cleanPhone.take(3)
    val suffix = cleanPhone.takeLast(2)
    return "$prefix****$suffix"
}