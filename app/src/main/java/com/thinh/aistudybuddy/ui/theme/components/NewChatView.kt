package com.thinh.aistudybuddy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.data.model.Banner
import com.thinh.aistudybuddy.data.model.Suggestion

@Composable
fun NewChatView(
    suggestions: List<Suggestion>,
    banner: Banner?,
    onSuggestionClick: (Suggestion) -> Unit,
    onBannerCtaClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Hello, Thinh",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
        ) {
            items(suggestions) { suggestion ->
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(60.dp).clickable { onSuggestionClick(suggestion) }
                ) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Text(text = suggestion.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = suggestion.subtitle, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        banner?.let {
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(60.dp).clickable { onBannerCtaClick() }
            ) {
                Row(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = it.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Button(onClick = onBannerCtaClick, colors = ButtonDefaults.buttonColors(containerColor = Color.White), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(text = it.ctaText, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}