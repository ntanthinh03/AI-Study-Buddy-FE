package com.thinh.aistudybuddy.ui.theme.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.ui.components.AuthTextField
import com.thinh.aistudybuddy.ui.components.BuddyLogo
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    onBackToChat: () -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedMajor by remember { mutableStateOf("Computer Science") }

    val majors = listOf("Computer Science", "Business", "Medicine", "Engineering")

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(110.dp)
                    .background(Color(0xFF121212))
            ) {
                IconButton(
                    onClick = onBackToChat,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                BuddyLogo(
                    modifier = Modifier
                        .size(270.dp)
                        .align(Alignment.BottomCenter)
                        .aspectRatio(1f)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Start Your AI Journey",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                AuthTextField(fullName, { fullName = it }, "Full Name", "Enter your full name")
                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(email, { email = it }, "University Email", "your.name@university.edu")
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                AuthTextField(password, { password = it }, "Password", "Create a password", isPassword = true)
                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(confirmPassword, { confirmPassword = it }, "Confirm Password", "Repeat password", isPassword = true)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "What are you studying?",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MajorItem(majors[0], selectedMajor == majors[0], Modifier.weight(1f)) { selectedMajor = majors[0] }
                        MajorItem(majors[1], selectedMajor == majors[1], Modifier.weight(1f)) { selectedMajor = majors[1] }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MajorItem(majors[2], selectedMajor == majors[2], Modifier.weight(1f)) { selectedMajor = majors[2] }
                        MajorItem(majors[3], selectedMajor == majors[3], Modifier.weight(1f)) { selectedMajor = majors[3] }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, "Registration Successful!", Toast.LENGTH_SHORT).show()
                        onRegisterSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Sign Up", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row {
                    Text("Already have an account? ", color = Color.Gray, fontSize = 14.sp)
                    Text(
                        text = "Log In",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onBackToLogin() }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MajorItem(title: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        color = if (isSelected) Color(0xFF1976D2) else Color(0xFF2C2C2E),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF3A3A3C))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(title, color = if (isSelected) Color.White else Color.LightGray, fontSize = 13.sp)
        }
    }
}
