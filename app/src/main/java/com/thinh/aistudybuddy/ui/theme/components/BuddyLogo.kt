package com.thinh.aistudybuddy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.thinh.aistudybuddy.R

@Composable
fun BuddyLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.buddy_logo_png),
        contentDescription = "Buddy Logo",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}