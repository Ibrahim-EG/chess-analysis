package app.lumen.chess

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Lamp.NightTop, surface = Lamp.Plate, onSurface = Lamp.Text)) {
                LumenRoot()
            }
        }
    }
}

@Composable
fun LumenRoot() {
    val vm: ChessVm = viewModel()
    val ui by vm.ui.collectAsState()
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Lamp.NightTop, Lamp.NightBottom)))) {
        HearthAmbient(ui.searching, Modifier.matchParentSize())
        if (ui.reviewMode) ReviewScreen(ui, vm) else {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
                Header(ui)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                    EvalBar(ui.topLineCp, ui.orientation == Side.BLACK, Modifier.fillMaxHeight())
                    Spacer(Modifier.width(12.dp))
                    ChessBoard(ui, { vm.onEvent(it) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                ui.lastBadge?.let { b ->
                    Card(colors = CardDefaults.cardColors(containerColor = b.badge.color.copy(alpha = 0.15f)), border = CardDefaults.outlinedCardBorder().copy(b.badge.color)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(b.badge.label, color = b.badge.color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Centipawn Loss: ${b.cpl}", color = Lamp.TextDim, fontSize = 12.sp)
                            Text(b.explanation, color = Lamp.Text, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = vm::newGame, colors = ButtonDefaults.buttonColors(containerColor = Lamp.Plate)) { Text("New") }
                    Button(onClick = vm::flip, colors = ButtonDefaults.buttonColors(containerColor = Lamp.Plate)) { Text("Flip") }
                    Button(onClick = vm::generateFullReport, colors = ButtonDefaults.buttonColors(containerColor = Lamp.Ember)) { Text("Game Report") }
                }
            }
        }
    }
}

@Composable
fun ReviewScreen(ui: UiState, vm: ChessVm) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::exitReview) { Text("← Back", color = Lamp.Gold) }
            Spacer(Modifier.weight(1f))
            Text("Game Report", color = Lamp.Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(ui.fullReport.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    pair.forEachIndexed { index, report ->
                        Column(Modifier.weight(1f).padding(end = if(index==0) 8.dp else 0.dp)) {
                            Text(report.uci, color = Lamp.Text, fontWeight = FontWeight.Bold)
                            Text(report.badge.label, color = report.badge.color, fontSize = 12.sp)
                            Text("CPL: ${report.cpl}", color = Lamp.TextDim, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

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
            val fade = sin(cycle * PI).toFloat()
            drawCircle(Color(0xFFFFE8B4), m.r, Offset(x, y), alpha = m.alpha * fade * .8f * flick)
        }
        drawRect(brush = Brush.radialGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = .65f)), center = Offset(w * .5f, h * .45f), radius = hypot(w.toDouble(), h.toDouble()).toFloat() * .62f))
    }
}
private data class Mote(val x: Float, val y0: Float, val speed: Float, val amp: Float, val phase: Float, val r: Float, val alpha: Float)

@Composable
fun ChessBoard(ui: UiState, onEvent: (BoardEvent) -> Unit, modifier: Modifier = Modifier) {
    val flipped = ui.orientation == Side.BLACK
    var dragPos by remember { mutableStateOf<Offset?>(null) }; var dragFrom by remember { mutableStateOf<Square?>(null) }
    Canvas(modifier.aspectRatio(1f).shadow(28.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x66FFB476), spotColor = Color(0x33FFB476)).clip(RoundedCornerShape(14.dp))
        .pointerInput(flipped) { detectTapGestures(onTap = { onEvent(BoardEvent.Press(squareAt(it, flipped))) }) }
        .pointerInput(flipped) { detectDragGestures(onDragStart = { start -> val sq = squareAt(start, flipped); dragFrom = sq; dragPos = start; onEvent(BoardEvent.Press(sq)) }, onDrag = { change, _ -> change.consume(); dragPos = change.position }, onDragEnd = { onEvent(BoardEvent.Drop(dragPos?.let { squareAt(it, flipped) })); dragPos = null; dragFrom = null }, onDragCancel = { dragPos = null; dragFrom = null }) }
    ) {
        val cell = minOf(size.width, size.height) / 8f
        for (sq in Square.values()) { val r = rectOf(sq, cell, flipped); val f = sq.ordinal % 8; val rk = sq.ordinal / 8; val light = (f + rk) % 2 == 1; drawRect(if (light) Lamp.SquareLight else Lamp.SquareDark, r.topLeft, r.size) }
        ui.lastMove?.let { (a, b) -> drawRect(Lamp.LastMove, rectOf(a, cell, flipped).topLeft, rectOf(a, cell, flipped).size); drawRect(Lamp.LastMove, rectOf(b, cell, flipped).topLeft, rectOf(b, cell, flipped).size) }
        ui.selected?.let { sq -> val c = rectOf(sq, cell, flipped).center; glow(c, cell * 1.6f, Lamp.Amber.copy(alpha = .30f)); glow(c, cell * 0.95f, Lamp.Ember.copy(alpha = .22f)) }
        for (sq in ui.legalTargets) { val c = rectOf(sq, cell, flipped).center; val occupied = ui.pieces[sq] != null; if (occupied) drawCircle(Lamp.Amber, radius = cell * .46f, center = c, style = Stroke(width = cell * .07f), alpha = .85f) else { glow(c, cell * .34f, Lamp.Ember.copy(alpha = .25f)); drawCircle(Lamp.Amber, radius = cell * .13f, center = c, alpha = .9f) } }
        for ((sq, piece) in ui.pieces) { if (sq == dragFrom && dragPos != null) continue; drawPieceGlyph(piece, rectOf(sq, cell, flipped).center, cell) }
        dragPos?.let { pos -> val piece = dragFrom?.let { ui.pieces[it] }; if (piece != null) { glow(pos, cell * 1.9f, Lamp.Amber.copy(alpha = .38f)); drawOval(Color(0x55000000), topLeft = Offset(pos.x - cell*.32f, pos.y + cell*.42f), size = Size(cell*.64f, cell*.18f)); drawPieceGlyph(piece, pos, cell, scale = 1.16f) } }
        drawRoundRect(Color(0x33FFC46E), Offset.Zero, size, androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()), style = Stroke(2f))
    }
}

private fun rectOf(sq: Square, cell: Float, flipped: Boolean): Rect { val f0 = sq.ordinal % 8; val r0 = sq.ordinal / 8; val f = if (flipped) 7 - f0 else f0; val row = if (flipped) r0 else 7 - r0; return Rect(f * cell, row * cell, (f + 1) * cell, (row + 1) * cell) }
private fun androidx.compose.ui.input.pointer.PointerInputScope.squareAt(pos: Offset, flipped: Boolean): Square? { val cell = minOf(size.width, size.height) / 8f; val f = (pos.x / cell).toInt(); val row = (pos.y / cell).toInt(); if (f !in 0..7 || row !in 0..7) return null; val file = if (flipped) 7 - f else f; val rank = if (flipped) row else 7 - row; return Square.values()[rank * 8 + file] }
private fun DrawScope.glow(center: Offset, radius: Float, color: Color) { val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb(); maskFilter = BlurMaskFilter(radius * .45f, BlurMaskFilter.Blur.NORMAL) }; drawContext.canvas.nativeCanvas.drawCircle(center.x, center.y, radius, p) }

private val GLYPH = mapOf(Piece.WHITE_KING to "♔", Piece.WHITE_QUEEN to "♕", Piece.WHITE_ROOK to "♖", Piece.WHITE_BISHOP to "♗", Piece.WHITE_KNIGHT to "♘", Piece.WHITE_PAWN to "♙", Piece.BLACK_KING to "♚", Piece.BLACK_QUEEN to "♛", Piece.BLACK_ROOK to "♜", Piece.BLACK_BISHOP to "♝", Piece.BLACK_KNIGHT to "♞", Piece.BLACK_PAWN to "♟")
private fun DrawScope.drawPieceGlyph(piece: Piece, center: Offset, cell: Float, scale: Float = 1f) {
    val glyph = GLYPH[piece] ?: return; val white = piece.pieceSide == Side.WHITE; val canvas = drawContext.canvas.nativeCanvas; val y = center.y + cell * 0.27f * scale
    if (!white) { val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = cell * .8f * scale; textAlign = Paint.Align.CENTER; style = Paint.Style.STROKE; strokeWidth = cell * .045f; color = 0x40FFB476.toInt() }; canvas.drawText(glyph, center.x, y, rim) }
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = cell * .78f * scale; textAlign = Paint.Align.CENTER; color = if (white) 0xFFF3E6C4.toInt() else 0xFF17110B.toInt(); setShadowLayer(cell * .09f, 0f, cell * .07f, 0xB0000000.toInt()); typeface = Typeface.DEFAULT }
    canvas.drawText(glyph, center.x, y, p)
}

@Composable
fun EvalBar(cp: Int, inverted: Boolean, modifier: Modifier = Modifier) {
    val whiteWin = 1f / (1f + kotlin.math.exp(-cp / 330f)); val target = if (inverted) 1f - whiteWin else whiteWin
    val fill by animateFloatAsState(target, tween(700), label = "eval")
    Column(modifier.width(13.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF101015))) {
        Spacer(Modifier.weight((1f - fill).coerceAtLeast(0.001f)))
        Box(Modifier.weight(fill.coerceAtLeast(0.001f)).background(Brush.verticalGradient(listOf(Lamp.Amber, Lamp.Ember))))
    }
}

@Composable
fun Header(ui: UiState) { Column { Text("L U M E N", color = Lamp.Gold, fontSize = 18.sp, fontWeight = FontWeight.W200, letterSpacing = .4.sp * 8); Text(ui.status, color = Lamp.TextDim, fontSize = 11.sp, letterSpacing = 1.5.sp) } }
