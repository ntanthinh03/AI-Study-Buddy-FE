import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thinh.aistudybuddy.R
@Composable
fun BuddyLogoPng() {
    Image(
        painter = painterResource(id = R.drawable.buddy_logo_png),
        contentDescription = "Buddy Logo (PNG)",
        modifier = Modifier.size(100.dp) // Điều chỉnh kích thước
    )
}