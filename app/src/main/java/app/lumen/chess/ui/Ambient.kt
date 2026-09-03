package app.lumen.chess.ui
import androidx.compose.foundation.Canvas; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset; import androidx.compose.ui.graphics.Brush; import androidx.compose.ui.graphics.Color
import kotlin.math.PI; import kotlin.math.hypot; import kotlin.math.sin; import kotlin.random.Random
private data class Mote(val x: Float, val y0: Float, val speed: Float, val amp: Float, val phase: Float, val r: Float, val alpha: Float)
@Composable
fun HearthAmbient(searching: Boolean, modifier: Modifier = Modifier) {
    val motes = remember { List(26) { Mote(Random.nextFloat(), Random.nextFloat(), .010f + Random.nextFloat() * .02f, .01f + Random.nextFloat() * .03f, Random.nextFloat() * 6.283f, 1.3f + Random.nextFloat() * 2.4f, .25f + Random.nextFloat() * .5f) } }
    var tMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { withFrameMillis { tMs = it } } }
    Canvas(modifier) {
        val w = size.width; val h = size.height; val ts = tMs / 1000f
        val flick = if (searching) .90f + .10f * sin(ts * 21f) * sin(ts * 7.3f) else 1f
        drawRect(brush = Brush.radialGradient(colors = listOf(Color(0xFFFFC878).copy(alpha = .15f * flick), Color.Transparent), center = Offset(w * .5f, -h * .15f), radius = h * 1.05f))
        for (m in motes) {
            val cycle = (((m.y0 - ts * m.speed) % 1f) + 1f) % 1f; val y = cycle * h; val x = (m.x + sin(ts * .6f + m.phase) * m.amp) * w
            val fade = sin(cycle * PI).toFloat(); drawCircle(Color(0xFFFFE8B4), m.r, Offset(x, y), alpha = m.alpha * fade * .8f * flick)
        }
        drawRect(brush = Brush.radialGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = .55f)), center = Offset(w * .5f, h * .45f), radius = hypot(w.toDouble(), h.toDouble()).toFloat() * .62f))
    }
}
