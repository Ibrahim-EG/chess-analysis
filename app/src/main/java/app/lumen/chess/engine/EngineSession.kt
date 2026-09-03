package app.lumen.chess.engine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
class EngineSession(private val conn: EngineConnection, private val scope: CoroutineScope) {
    private val _infoFlow = MutableSharedFlow<PvLine>(replay = 1)
    var isReady = false; private set
    fun start() {
        scope.launch { conn.lines.collect { raw ->
            when {
                raw == "uciok" -> conn.send("isready")
                raw == "readyok" -> { isReady = true }
                raw.startsWith("info") -> parseInfo(raw)?.let { _infoFlow.emit(it) }
            }
        }}
        conn.send("uci")
    }
    suspend fun getEval(fen: String, depth: Int = 14): PvLine? {
        if (!isReady) return null
        conn.send("stop"); conn.send("position fen $fen"); conn.send("go depth $depth")
        return withTimeoutOrNull(4000) { _infoFlow.first { it.depth >= depth } }
    }
    fun shutdown() { conn.close() }
}
