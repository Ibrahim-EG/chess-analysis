package app.lumen.chess.engine
import kotlin.math.exp; import kotlin.math.max
enum class MoveBadge(val label: String, val colorHex: Long) {
    BRILLIANT("Brilliant", 0xFF26A69A), BEST("Best Move", 0xFF4CAF50), GOOD("Good", 0xFF8BC34A),
    INACCURACY("Inaccuracy", 0xFFFFEB3B), MISTAKE("Mistake", 0xFFFF9800), BLUNDER("Blunder", 0xFFF44336)
}
data class MoveAnalysis(val badge: MoveBadge, val cpl: Int, val explanation: String)
object GameAnalyzer {
    fun classifyMove(bestEvalCp: Int, playedEvalCp: Int, stmIsWhite: Boolean): MoveAnalysis {
        val wBest = if (stmIsWhite) bestEvalCp else -bestEvalCp
        val wPlayed = if (stmIsWhite) playedEvalCp else -playedEvalCp
        val loss = max(0, wBest - wPlayed)
        val badge = when { loss <= 15 -> MoveBadge.BEST; loss <= 50 -> MoveBadge.GOOD; loss <= 150 -> MoveBadge.INACCURACY; loss <= 300 -> MoveBadge.MISTAKE; else -> MoveBadge.BLUNDER }
        val exp = if (loss > 300) "You missed a tactic or hung a piece." else if (loss > 150) "You lost a pawn or ruined your structure." else "Solid positional play."
        return MoveAnalysis(badge, loss, exp)
    }
}
