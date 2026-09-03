package app.lumen.chess.ui

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.aspectRatio
import androidx.compose.ui.unit.dp
import app.lumen.chess.vm.ArrowUi
import app.lumen.chess.vm.BoardEvent
import app.lumen.chess.vm.UiState
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

private val GLYPH = mapOf(
    Piece.WHITE_KING to "♔", Piece.WHITE_QUEEN to "♕", Piece.WHITE_ROOK to "♖",
    Piece.WHITE_BISHOP to "♗", Piece.WHITE_KNIGHT to "♘", Piece.WHITE_PAWN to "♙",
    Piece.BLACK_KING to "♚", Piece.BLACK_QUEEN to "♛", Piece.BLACK_ROOK to "♜",
    Piece.BLACK_BISHOP to "♝", Piece.BLACK_KNIGHT to "♞", Piece.BLACK_PAWN to "♟"
)

@Composable
fun ChessBoard(ui: UiState, onEvent: (BoardEvent) -> Unit, modifier: Modifier = Modifier) {
    val flipped = ui.orientation == Side.BLACK
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    var dragFrom by remember { mutableStateOf<Square?>(null) }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(28.dp, RoundedCornerShape(14.dp),
                ambientColor = Color(0x66FFB476), spotColor = Color(0x33FFB476))
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(flipped) {
                detectTapGestures(onTap = { onEvent(BoardEvent.Press(squareAt(it, flipped))) })
            }
            .pointerInput(flipped) {
                detectDragGestures(
                    onDragStart = { start ->
                        val sq = squareAt(start, flipped)
                        dragFrom = sq; dragPos = start
                        onEvent(BoardEvent.Press(sq))
                    },
                    onDrag = { change, _ -> change.consume(); dragPos = change.position },
                    onDragEnd = {
                        onEvent(BoardEvent.Drop(dragPos?.let { squareAt(it, flipped) }))
                        dragPos = null; dragFrom = null
                    },
                    onDragCancel = { dragPos = null; dragFrom = null }
                )
            }
    ) {
        val cell = minOf(size.width, size.height) / 8f

        for (sq in Square.values()) {
            val r = rectOf(sq, cell, flipped)
            val f = sq.ordinal % 8
            val rk = sq.ordinal / 8
            val light = (f + rk) % 2 == 1
            drawRect(if (light) Hearth.SquareLight else Hearth.SquareDark, r.topLeft, r.size)
        }

        ui.lastMove?.let { (a, b) ->
            drawRect(Hearth.LastMove, rectOf(a, cell, flipped).topLeft, rectOf(a, cell, flipped).size)
            drawRect(Hearth.LastMove, rectOf(b, cell, flipped).topLeft, rectOf(b, cell, flipped).size)
        }

        ui.selected?.let { sq ->
            val c = rectOf(sq, cell, flipped).center
            glow(c, cell * 1.6f, Hearth.Amber.copy(alpha = .30f))
            glow(c, cell * 0.95f, Hearth.Ember.copy(alpha = .22f))
            drawRect(Color(0x2EFFD98A), rectOf(sq, cell, flipped).topLeft, rectOf(sq, cell, flipped).size)
        }

        for (sq in ui.legalTargets) {
            val c = rectOf(sq, cell, flipped).center
            val occupied = ui.pieces[sq] != null
            if (occupied) {
                drawCircle(Hearth.Amber, radius = cell * .46f, center = c,
                    style = Stroke(width = cell * .07f), alpha = .85f)
            } else {
                glow(c, cell * .34f, Hearth.Ember.copy(alpha = .25f))
                drawCircle(Hearth.Amber, radius = cell * .13f, center = c, alpha = .9f)
            }
        }

        for ((sq, piece) in ui.pieces) {
            if (sq == dragFrom && dragPos != null) continue
            drawPieceGlyph(piece, rectOf(sq, cell, flipped).center, cell)
        }

        for (a in ui.arrows) drawUciArrow(a, cell, flipped)

        dragPos?.let { pos ->
            val piece = dragFrom?.let { ui.pieces[it] }
            if (piece != null) {
                glow(pos, cell * 1.9f, Hearth.Amber.copy(alpha = .38f))
                drawOval(Color(0x55000000), topLeft = Offset(pos.x - cell*.32f, pos.y + cell*.42f),
                    size = Size(cell*.64f, cell*.18f))
                drawPieceGlyph(piece, pos, cell, scale = 1.16f)
            }
        }

        drawRoundRect(Color(0x33FFC46E), Offset.Zero, size,
            androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()), style = Stroke(2f))
    }
}

private fun rectOf(sq: Square, cell: Float, flipped: Boolean): Rect {
    val f0 = sq.ordinal % 8
    val r0 = sq.ordinal / 8
    val f = if (flipped) 7 - f0 else f0
    val row = if (flipped) r0 else 7 - r0
    return Rect(f * cell, row * cell, (f + 1) * cell, (row + 1) * cell)
}

private fun androidx.compose.ui.input.pointer.PointerInputScope.squareAt(pos: Offset, flipped: Boolean): Square? {
    val cell = minOf(size.width, size.height) / 8f
    val f = (pos.x / cell).toInt()
    val row = (pos.y / cell).toInt()
    if (f !in 0..7 || row !in 0..7) return null
    val file = if (flipped) 7 - f else f
    val rank = if (flipped) row else 7 - row
    return Square.values()[rank * 8 + file]
}

private fun DrawScope.glow(center: Offset, radius: Float, color: Color) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(radius * .45f, BlurMaskFilter.Blur.NORMAL)
    }
    drawContext.canvas.nativeCanvas.drawCircle(center.x, center.y, radius, p)
}

private fun DrawScope.drawPieceGlyph(piece: Piece, center: Offset, cell: Float, scale: Float = 1f) {
    val glyph = GLYPH[piece] ?: return
    val white = piece.pieceSide == Side.WHITE
    val canvas = drawContext.canvas.nativeCanvas
    val y = center.y + cell * 0.27f * scale

    if (!white) {
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cell * .8f * scale
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = cell * .045f
            color = 0x40FFB476.toInt()
        }
        canvas.drawText(glyph, center.x, y, rim)
    }
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = cell * .78f * scale
        textAlign = Paint.Align.CENTER
        color = if (white) 0xFFF3E6C4.toInt() else 0xFF17110B.toInt()
        setShadowLayer(cell * .09f, 0f, cell * .07f, 0xB0000000.toInt())
        typeface = Typeface.DEFAULT
    }
    canvas.drawText(glyph, center.x, y, p)
}

private fun DrawScope.drawUciArrow(a: ArrowUi, cell: Float, flipped: Boolean) {
    val from = rectOf(a.from, cell, flipped).center
    val to = rectOf(a.to, cell, flipped).center
    val v = to - from
    val len = v.getDistance()
    if (len < 1f) return
    val u = v / len
    val shaftEnd = to - u * (cell * .36f)
    drawLine(a.color.copy(alpha = a.color.alpha * .30f), from, to,
        strokeWidth = cell * .34f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(a.color, from, shaftEnd, strokeWidth = cell * .15f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round)
    val perp = Offset(-u.y, u.x) * (cell * .18f)
    drawPath(Path().apply {
        moveTo(to.x, to.y)
        lineTo(shaftEnd.x + perp.x, shaftEnd.y + perp.y)
        lineTo(shaftEnd.x - perp.x, shaftEnd.y - perp.y)
        close()
    }, a.color)
}
