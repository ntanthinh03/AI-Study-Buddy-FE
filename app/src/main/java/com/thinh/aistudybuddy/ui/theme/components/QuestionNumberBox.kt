import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.aistudybuddy.viewmodel.QuizViewModel

@Composable
fun QuestionNumberBox(
    number: Int,
    status: QuizViewModel.QuestionStatus,
    onClick: () -> Unit
) {
    val backgroundColor = when (status) {
        QuizViewModel.QuestionStatus.CORRECT -> Color(0xFF2E7D32)
        QuizViewModel.QuestionStatus.INCORRECT -> Color(0xFFD32F2F)
        QuizViewModel.QuestionStatus.UNANSWERED -> Color(0xFF3A3A3C)
    }

    Box(
        modifier = Modifier
            .size(50.dp)
            .background(backgroundColor, shape = RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}