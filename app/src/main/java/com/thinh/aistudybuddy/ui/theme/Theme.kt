package com.thinh.aistudybuddy.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberPrimary,
    onPrimary = Color.Black,
    secondary = CyberSecondary,
    onSecondary = Color.Black,
    tertiary = CyberTertiary,
    background = DeepSpaceBackground,
    onBackground = Color.White,
    surface = SurfaceCardContainer,
    onSurface = Color.White,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = Color.White,
    outline = Color(0x26FFFFFF), // Translucent borders
    error = RoseWarning,
    onError = Color.White
)

@Composable
fun FEBuddyTheme(
    darkTheme: Boolean = true, // Force dark theme by default for immersive cyberpunk feel
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve high-fidelity Stitch AI color system
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        CyberDarkColorScheme
    } else {
        // Fallback/Legacy light mode maps back to dark space background for cyberpunk continuity
        CyberDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Global Cyberpunk Glass Card Modifier Extension
fun Modifier.glassCard(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color(0xB3102034), // Rich translucent space navy (70% opacity)
    borderColor: Color = Color(0x1Fffffff), // Soft translucent white border (12% opacity)
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(width = borderWidth, color = borderColor, shape = shape)

// Helper: Cyber Gradient Border
fun Modifier.cyberBorder(
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.5.dp,
    startColor: Color = PrimaryNeonTeal,
    endColor: Color = TertiaryCosmicIndigo
): Modifier = this
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(colors = listOf(startColor, endColor)),
        shape = shape
    )