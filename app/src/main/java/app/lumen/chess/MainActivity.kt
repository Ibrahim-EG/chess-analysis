package app.lumen.chess
import android.os.Bundle; import androidx.activity.ComponentActivity; import androidx.activity.compose.setContent
import androidx.compose.foundation.background; import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed; import androidx.compose.material3.MaterialTheme; import androidx.compose.material3.Text
import androidx.compose.material3.TextButton; import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier; import androidx.compose.ui.graphics.Brush; import androidx.compose.ui.graphics.Color; import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp; import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel; import app.lumen.chess.ui.*; import app.lumen.chess.vm.ChessVm; import app.lumen.chess.vm.UiState
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val wsc = calculateWindowSizeClass(this)
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = Hearth.NightTop, surface = Hearth.Plate, onSurface = Hearth.Text)) {
                LumenRoot(wide = wsc.widthSizeClass >= WindowWidthSizeClass.EXPANDED)
            }
        }
    }
}
@Composable
fun LumenRoot(wide: Boolean) {
    val vm: ChessVm = viewModel(); val ui by vm.ui.collectAsState()
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Hearth.NightTop, Hearth.NightBottom)))) {
        HearthAmbient(ui.searching, Modifier.matchParentSize())
        if (wide) WideLayout(ui, vm) else PhoneLayout(ui, vm)
    }
}
@Composable
fun PhoneLayout(ui: UiState, vm: ChessVm) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(10.dp)) {
        Header(ui); Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            EvalBar(ui.topLine?.scoreCp, ui.topLine?.scoreMate, inverted = ui.orientation == com.github.bhlangonijr.chesslib.Side.BLACK, modifier = Modifier.fillMaxHeight())
            Spacer(Modifier.width(10.dp)); ChessBoard(ui, vm::onBoardEvent, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        ui.lastBadge?.let { b -> Text("${b.badge.label} | CPL: ${b.cpl} | ${b.explanation}", color = Color(b.badge.colorHex), fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp)) }
        Controls(ui, vm); MoveStrip(ui.history)
    }
}
@Composable
fun WideLayout(ui: UiState, vm: ChessVm) {
    Row(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).height(IntrinsicSize.Max), verticalAlignment = Alignment.CenterVertically) {
            EvalBar(ui.topLine?.scoreCp, ui.topLine?.scoreMate, inverted = ui.orientation == com.github.bhlangonijr.chesslib.Side.BLACK, modifier = Modifier.fillMaxHeight())
            Spacer(Modifier.width(14.dp)); ChessBoard(ui, vm::onBoardEvent, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(28.dp))
        Column(Modifier.width(340.dp)) {
            Header(ui); Spacer(Modifier.height(12.dp))
            ui.lastBadge?.let { b -> Text("${b.badge.label} | CPL: ${b.cpl}", color = Color(b.badge.colorHex), fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(b.explanation, color = Hearth.TextDim, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp)) }
            Controls(ui, vm); Spacer(Modifier.height(12.dp)); MoveStrip(ui.history)
        }
    }
}
@Composable
fun Header(ui: UiState) { Column { Text("L U M E N", color = Hearth.Gold, fontSize = 18.sp, fontWeight = FontWeight.W200, letterSpacing = .4.sp * 8); Text(ui.status, color = Hearth.TextDim, fontSize = 11.sp, letterSpacing = 1.5.sp) } }
@Composable
fun Controls(ui: UiState, vm: ChessVm) { Row { TextButton(onClick = vm::newGame) { Text("new") }; TextButton(onClick = vm::undo) { Text("undo") }; TextButton(onClick = vm::flip) { Text("flip") }; TextButton(onClick = vm::analyse) { Text("analyze") } } }
@Composable
fun MoveStrip(history: List<String>) { LazyRow(contentPadding = PaddingValues(vertical = 8.dp)) { itemsIndexed(history.chunked(2)) { i, pair -> Text("${i + 1}. ${pair.joinToString(" ")}   ", color = Hearth.TextDim, fontSize = 13.sp) } } }
