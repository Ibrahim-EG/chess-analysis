package app.lumen.chess.chess
import com.github.bhlangonijr.chesslib.Board; import com.github.bhlangonijr.chesslib.Piece; import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square; import com.github.bhlangonijr.chesslib.move.Move
class Game {
    val board = Board(); val history = ArrayDeque<String>()
    init { board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") }
    fun reset() { board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"); history.clear() }
    val fen: String get() = board.getFen()
    val sideToMove: Side get() = board.sideToMove
    fun legalMovesFrom(sq: Square): List<Move> = try { board.legalMoves().filter { it.from == sq } } catch (_: Exception) { emptyList() }
    fun parseUci(uci: String): Move? {
        if (uci.length < 4) return null
        return try {
            val from = Square.fromValue(uci.take(2).uppercase()) ?: return null
            val to = Square.fromValue(uci.substring(2, 4).uppercase()) ?: return null
            if (uci.length >= 5) Move(from, to, when (uci[4].lowercaseChar()) { 'q' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN; 'r' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK; 'b' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP; 'n' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT; else -> return null })
            else Move(from, to)
        } catch (_: Exception) { null }
    }
}
fun sqName(sq: Square): String { val f = "abcdefgh"[sq.ordinal % 8]; val r = sq.ordinal / 8 + 1; return "$f$r" }
fun uciOf(m: Move): String = sqName(m.from) + sqName(m.to) + when (m.promotion) { Piece.WHITE_QUEEN, Piece.BLACK_QUEEN -> "q"; Piece.WHITE_ROOK, Piece.BLACK_ROOK -> "r"; Piece.WHITE_BISHOP, Piece.BLACK_BISHOP -> "b"; Piece.WHITE_KNIGHT, Piece.BLACK_KNIGHT -> "n"; else -> "" }
