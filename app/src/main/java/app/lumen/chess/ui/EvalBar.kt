package app.lumen.chess.ui
import androidx.compose.animation.core.animateFloatAsState; import androidx.compose.animation.core.tween
import androidx.compose.foundation.background; import androidx.compose.foundation.layout.*; import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*; import androidx.compose.ui.Modifier; import androidx.compose.ui.draw.clip; import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color; import androidx.compose.ui.unit.dp; import kotlin.math.exp
@Composable
fun EvalBar(cp: Int?, mate: Int?, inverted: Boolean, modifier: Modifier = Modifier) {
    val whiteWin = when { mate != null -> if (mate > 0) 1f else 0f; cp != null -> 1f / (1f + exp(-cp / 330f)); else -> .5f }
    val target = if (inverted) 1f - whiteWin else whiteWin; val fill by animateFloatAsState(target, tween(700), label = "eval")
    Column(modifier.width(13.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF101015))) {
        Spacer(Modifier.weight((1f - fill).coerceAtLeast(0.001f)))
        Box(Modifier.weight(fill.coerceAtLeast(0.001f)).background(Brush.verticalGradient(listOf(Hearth.Amber, Hearth.Ember))))
    }
}
