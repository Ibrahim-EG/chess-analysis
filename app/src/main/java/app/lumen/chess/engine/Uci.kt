package app.lumen.chess.engine

data class PvLine(val multipv: Int, val depth: Int, val scoreCp: Int?, val scoreMate: Int?, val nps: Long, val moves: List<String>)
sealed interface EngineEvent {
    data object Ready : EngineEvent
    data class Info(val line: PvLine) : EngineEvent
    data class Error(val msg: String) : EngineEvent
}
fun parseInfo(raw: String): PvLine? {
    val t = raw.split(" ").filter { it.isNotBlank() }
    if (t.getOrNull(1) == "string") return null
    var depth = 0; var multipv = 1; var cp: Int? = null; var mate: Int? = null
    var nps = 0L; var pvStart = -1; var i = 1
    while (i < t.size) {
        when (t[i]) {
            "depth" -> depth = t.getOrNull(i + 1)?.toIntOrNull() ?: 0
            "multipv" -> multipv = t.getOrNull(i + 1)?.toIntOrNull() ?: 1
            "nps" -> nps = t.getOrNull(i + 1)?.toLongOrNull() ?: 0L
            "score" -> {
                val kind = t.getOrNull(i + 1); var k = i + 2
                if (t.getOrNull(k) == "lowerbound" || t.getOrNull(k) == "upperbound") k++
                when (kind) { "cp" -> cp = t.getOrNull(k)?.toIntOrNull(); "mate" -> mate = t.getOrNull(k)?.toIntOrNull() }
                i = k
            }
            "pv" -> { pvStart = i + 1; break }
        }
        i++
    }
    if (depth <= 0 || (cp == null && mate == null)) return null
    return PvLine(multipv, depth, cp, mate, nps, if (pvStart > 0) t.subList(pvStart, t.size) else emptyList())
}
