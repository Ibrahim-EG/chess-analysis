package app.lumen.chess

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// --- LAMP ROOM PALETTE ---
object Lamp {
    val NightTop = Color(0xFF020204)
    val NightBottom = Color(0xFF0A0A0F)
    val Plate = Color(0xFF15151B)
    val TextDim = Color(0xFF767D8C)
    val Text = Color(0xFFCFCFD6)
    val Ember = Color(0xFFFFB64D)
    val Amber = Color(0xFFFFD98A)
    val Gold = Color(0xFFFFE6BD)
    val SquareLight = Color(0xFFB99968)
    val SquareDark = Color(0xFF5A4632)
    val LastMove = Color(0x55FFC46E)
}

enum class Badge(val label: String, val color: Color) {
    BRILLIANT("Brilliant", Color(0xFF26A69A)), BEST("Best Move", Color(0xFF4CAF50)),
    GOOD("Good", Color(0xFF8BC34A)), INACCURACY("Inaccuracy", Color(0xFFFFEB3B)),
    MISTAKE("Mistake", Color(0xFFFF9800)), BLUNDER("Blunder", Color(0xFFF44336))
}

data class MoveReport(val uci: String, val badge: Badge, val cpl: Int, val explanation: String)

// --- NATIVE KOTLIN EVALUATOR (Guarantees instant analysis without external apps) ---
object Evaluator {
    fun evaluate(board: Board): Int {
        var score = 0
        for (sq in Square.values()) {
            val p = board.getPiece(sq)
            if (p == Piece.NONE) continue
            val v = when (p.pieceType) {
                PieceType.PAWN -> 100; PieceType.KNIGHT -> 320; PieceType.BISHOP -> 330
                PieceType.ROOK -> 500; PieceType.QUEEN -> 900; PieceType.KING -> 20000
                else -> 0
            }
            score += if (p.pieceSide == Side.WHITE) v else -v
        }
        // Add mobility bonus
        val mobility = board.legalMoves().size
        score += if (board.sideToMove == Side.WHITE) mobility * 3 else -mobility * 3
        return score
    }
}

data class UiState(
    val fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    val pieces: Map<Square, Piece> = emptyMap(), val orientation: Side = Side.WHITE,
    val selected: Square? = null, val legalTargets: Set<Square> = emptySet(),
    val lastMove: Pair<Square, Square>? = null, val history: List<String> = emptyList(),
    val topLineCp: Int = 0, val searching: Boolean = false, val status: String = "Lamp Room Ready",
    val lastBadge: MoveReport? = null, val gameOver: Boolean = false,
    val reviewMode: Boolean = false, val fullReport: List<MoveReport> = emptyList()
)

class ChessVm(app: Application) : AndroidViewModel(app) {
    private val board = Board()
    private val _ui = MutableStateFlow(UiState(pieces = snapshotPieces()))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init { analyzeCurrent() }

    private fun snapshotPieces(): Map<Square, Piece> {
        val m = HashMap<Square, Piece>(32)
        for (sq in Square.values()) { val p = board.getPiece(sq); if (p != Piece.NONE) m[sq] = p }
        return m
    }

    fun onEvent(ev: BoardEvent) { when (ev) { is BoardEvent.Press -> onPress(ev.square); is BoardEvent.Drop -> onDrop(ev.square) } }

    private fun onPress(sq: Square?) {
        if (sq == null) return; val st = _ui.value
        if (st.selected != null && sq in st.legalTargets) { doMove(st.selected, sq); return }
        val p = board.getPiece(sq)
        if (p != Piece.NONE && p.pieceSide == board.sideToMove) {
            _ui.update { it.copy(selected = sq, legalTargets = board.legalMoves().filter { it.from == sq }.map { it.to }.toSet()) }
        } else { _ui.update { it.copy(selected = null, legalTargets = emptySet()) } }
    }

    private fun onDrop(sq: Square?) {
        val st = _ui.value; val sel = st.selected ?: return
        if (sq != null && sq != sel && sq in st.legalTargets) doMove(sel, sq)
        else _ui.update { it.copy(selected = null, legalTargets = emptySet()) }
    }

    private fun doMove(from: Square, to: Square) {
        val candidates = board.legalMoves().filter { it.from == from && it.to == to }
        if (candidates.isEmpty()) return
        val mv = candidates.firstOrNull { it.promotion != null && it.promotion != Piece.NONE } ?: candidates.first()
        val fenBefore = board.fen; val stmIsWhite = board.sideToMove == Side.WHITE
        
        // Evaluate BEFORE move
        val evalBefore = Evaluator.evaluate(board)
        
        board.doMove(mv)
        val uci = uciOf(mv); val newHistory = _ui.value.history + uci
        
        // Evaluate AFTER move
        val evalAfter = Evaluator.evaluate(board)
        
        // Calculate Centipawn Loss (CPL)
        val loss = if (stmIsWhite) (evalBefore - evalAfter).coerceAtLeast(0) else (evalAfter - evalBefore).coerceAtLeast(0)
        val badge = when { loss <= 15 -> Badge.BEST; loss <= 50 -> Badge.GOOD; loss <= 150 -> Badge.INACCURACY; loss <= 300 -> Badge.MISTAKE; else -> Badge.BLUNDER }
        val exp = if (loss > 300) "You hung a piece or missed a forced mate." else if (loss > 150) "You lost material or ruined your structure." else "Solid positional play."
        
        _ui.update { st -> st.copy(fen = board.fen, pieces = snapshotPieces(), selected = null, legalTargets = emptySet(),
            lastMove = from to to, history = newHistory, gameOver = board.isMated || board.isDraw, 
            lastBadge = MoveReport(uci, badge, loss, exp)) }
        
        analyzeCurrent()
    }

    private fun analyzeCurrent() {
        if (_ui.value.gameOver) return; _ui.update { it.copy(searching = true) }
        viewModelScope.launch {
            delay(300) // Simulate engine "thinking" for the Lamp Room flicker effect
            val cp = Evaluator.evaluate(board)
            _ui.update { it.copy(searching = false, topLineCp = cp) }
        }
    }

    fun generateFullReport() {
        viewModelScope.launch {
            _ui.update { it.copy(status = "Generating Game Report...", reviewMode = true) }
            val tempBoard = Board(); val reports = mutableListOf<MoveReport>()
            for (uci in _ui.value.history) {
                val stmIsWhite = tempBoard.sideToMove == Side.WHITE
                val evalBefore = Evaluator.evaluate(tempBoard)
                val mv = parseUci(uci, tempBoard) ?: continue; tempBoard.doMove(mv)
                val evalAfter = Evaluator.evaluate(tempBoard)
                val loss = if (stmIsWhite) (evalBefore - evalAfter).coerceAtLeast(0) else (evalAfter - evalBefore).coerceAtLeast(0)
                val badge = when { loss <= 15 -> Badge.BEST; loss <= 50 -> Badge.GOOD; loss <= 150 -> Badge.INACCURACY; loss <= 300 -> Badge.MISTAKE; else -> Badge.BLUNDER }
                val exp = if (loss > 300) "Major tactical oversight." else if (loss > 150) "Positional inaccuracy." else "Best or near-best move."
                reports.add(MoveReport(uci, badge, loss, exp))
            }
            _ui.update { it.copy(fullReport = reports, status = "Game Report Ready") }
        }
    }

    fun exitReview() { _ui.update { it.copy(reviewMode = false) } }
    fun flip() = _ui.update { it.copy(orientation = if (it.orientation == Side.WHITE) Side.BLACK else Side.WHITE) }
    fun newGame() {
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        _ui.update { UiState(pieces = snapshotPieces(), status = "Lamp Room Ready") }; analyzeCurrent()
    }

    private fun parseUci(uci: String, b: Board): Move? {
        if (uci.length < 4) return null
        return try {
            val from = Square.fromValue(uci.take(2).uppercase()) ?: return null
            val to = Square.fromValue(uci.substring(2, 4).uppercase()) ?: return null
            if (uci.length >= 5) Move(from, to, when (uci[4].lowercaseChar()) {
                'q' -> if (b.sideToMove == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
                'r' -> if (b.sideToMove == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
                'b' -> if (b.sideToMove == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                'n' -> if (b.sideToMove == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                else -> return null
            }) else Move(from, to)
        } catch (_: Exception) { null }
    }
}

fun sqName(sq: Square): String { val f = "abcdefgh"[sq.ordinal % 8]; val r = sq.ordinal / 8 + 1; return "$f$r" }
fun uciOf(m: Move): String = sqName(m.from) + sqName(m.to) + when (m.promotion) {
    Piece.WHITE_QUEEN, Piece.BLACK_QUEEN -> "q"; Piece.WHITE_ROOK, Piece.BLACK_ROOK -> "r"
    Piece.WHITE_BISHOP, Piece.BLACK_BISHOP -> "b"; Piece.WHITE_KNIGHT, Piece.BLACK_KNIGHT -> "n"; else -> ""
}
sealed interface BoardEvent { data class Press(val square: Square?) : BoardEvent; data class Drop(val square: Square?) : BoardEvent }
