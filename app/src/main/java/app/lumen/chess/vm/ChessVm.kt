package app.lumen.chess.vm

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lumen.chess.chess.Game
import app.lumen.chess.chess.sqName
import app.lumen.chess.chess.uciOf
import app.lumen.chess.engine.EngineSession
import app.lumen.chess.engine.GameAnalyzer
import app.lumen.chess.engine.OpenExchange
import app.lumen.chess.engine.OpenExchangeEngine
import app.lumen.chess.engine.PvLine
import app.lumen.chess.ui.Hearth
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BoardEvent {
    data class Press(val square: Square?) : BoardEvent
    data class Drop(val square: Square?) : BoardEvent
}

data class ArrowUi(val from: Square, val to: Square, val color: Color = Hearth.ArrowGold)

data class UiState(
    val fen: String = Game().fen,
    val pieces: Map<Square, Piece> = emptyMap(),
    val orientation: Side = Side.WHITE,
    val selected: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val lastMove: Pair<Square, Square>? = null,
    val history: List<String> = emptyList(),
    val topLine: PvLine? = null,
    val arrows: List<ArrowUi> = emptyList(),
    val searching: Boolean = false,
    val status: String = "find engine…",
    val lastBadge: app.lumen.chess.engine.MoveAnalysis? = null,
    val gameOver: Boolean = false
)

class ChessVm(app: Application) : AndroidViewModel(app) {
    private val game = Game()
    private val _ui = MutableStateFlow(UiState(pieces = snapshotPieces()))
    val ui: StateFlow<UiState> = _ui.asStateFlow()
    private var session: EngineSession? = null

    init {
        connectEngine()
    }

    private fun connectEngine() {
        val engines = OpenExchange.discoverEngines(getApplication<Application>().packageManager)
        val pick = engines.firstOrNull { it.label.contains("stockfish", true) } ?: engines.firstOrNull()
        if (pick == null) {
            _ui.update { it.copy(status = "No engine found") }
            return
        }
        val conn = OpenExchangeEngine(getApplication(), pick.component)
        try {
            conn.start()
        } catch (e: Exception) {
            _ui.update { it.copy(status = "engine failed") }
            return
        }
        val s = EngineSession(conn, viewModelScope)
        session = s
        s.start()
        _ui.update { it.copy(status = "connecting to ${pick.label}…") }
        
        viewModelScope.launch {
            while (!s.isReady) {
                delay(100)
            }
            _ui.update { it.copy(status = "Engine Ready") }
            analyse()
        }
    }

    fun analyse() {
        val s = session ?: return
        if (!s.isReady || _ui.value.gameOver) return
        _ui.update { it.copy(searching = true) }
        viewModelScope.launch {
            val eval = s.getEval(game.fen, 14)
            _ui.update { st ->
                st.copy(
                    searching = false,
                    topLine = eval,
                    arrows = eval?.moves?.take(1)?.mapNotNull { arrowFromUci(it) } ?: emptyList()
                )
            }
        }
    }

    private fun arrowFromUci(uci: String): ArrowUi? {
        val m = game.parseUci(uci) ?: return null
        return ArrowUi(m.from, m.to)
    }

    fun onBoardEvent(ev: BoardEvent) {
        when (ev) {
            is BoardEvent.Press -> onPress(ev.square)
            is BoardEvent.Drop -> onDrop(ev.square)
        }
    }

    private fun onPress(sq: Square?) {
        if (sq == null) return
        val st = _ui.value
        if (st.selected != null && sq in st.legalTargets) {
            doMove(st.selected, sq)
            return
        }
        val p = game.board.getPiece(sq)
        if (p != Piece.NONE && p.pieceSide == game.sideToMove) {
            _ui.update { it.copy(selected = sq, legalTargets = game.legalMovesFrom(sq).map { m -> m.to }.toSet()) }
        } else {
            _ui.update { it.copy(selected = null, legalTargets = emptySet()) }
        }
    }

    private fun onDrop(sq: Square?) {
        val st = _ui.value
        val sel = st.selected ?: return
        if (sq != null && sq != sel && sq in st.legalTargets) {
            doMove(sel, sq)
        } else {
            _ui.update { it.copy(selected = null, legalTargets = emptySet()) }
        }
    }

    private fun doMove(from: Square, to: Square) {
        val candidates = game.legalMovesFrom(from).filter { it.to == to }
        if (candidates.isEmpty()) return
        
        val mv = candidates.firstOrNull { 
            it.promotion != null && it.promotion != Piece.NONE && it.promotion.pieceType == PieceType.QUEEN 
        } ?: candidates.first()
        
        val stmIsWhite = game.sideToMove == Side.WHITE
        val fenBefore = game.fen
        
        game.board.doMove(mv)
        game.history.addLast(uciOf(mv))
        
        _ui.update { st ->
            st.copy(
                fen = game.fen,
                pieces = snapshotPieces(),
                selected = null,
                legalTargets = emptySet(),
                lastMove = from to to,
                history = st.history + uciOf(mv),
                gameOver = game.board.isMated || game.board.isDraw,
                arrows = emptyList(),
                topLine = null,
                lastBadge = null
            )
        }
        
        viewModelScope.launch {
            val s = session ?: return@launch
            val evalBefore = s.getEval(fenBefore, 12)
            val evalAfter = s.getEval(game.fen, 12)
            
            if (evalBefore != null && evalAfter != null && evalBefore.scoreCp != null && evalAfter.scoreCp != null) {
                val badge = GameAnalyzer.classifyMove(evalBefore.scoreCp, evalAfter.scoreCp, stmIsWhite)
                _ui.update { it.copy(lastBadge = badge) }
            }
            analyse()
        }
    }

    fun undo() {
        if (game.history.isEmpty()) return
        game.board.undoMove()
        game.history.removeLast()
        _ui.update { st ->
            st.copy(
                fen = game.fen,
                pieces = snapshotPieces(),
                lastMove = null,
                history = st.history.dropLast(1),
                selected = null,
                legalTargets = emptySet(),
                gameOver = false,
                lastBadge = null
            )
        }
        analyse()
    }

    fun flip() {
        _ui.update { it.copy(orientation = if (it.orientation == Side.WHITE) Side.BLACK else Side.WHITE) }
    }

    fun newGame() {
        game.reset()
        _ui.update { UiState(pieces = snapshotPieces(), status = it.status) }
        analyse()
    }

    private fun snapshotPieces(): Map<Square, Piece> {
        val m = HashMap<Square, Piece>(32)
        for (sq in Square.values()) {
            val p = game.board.getPiece(sq)
            if (p != Piece.NONE) m[sq] = p
        }
        return m
    }

    override fun onCleared() {
        super.onCleared()
        session?.shutdown()
    }
}
