package app.lumen.chess.engine
import android.content.ComponentName; import android.content.Context; import android.content.Intent; import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor; import kotlinx.coroutines.channels.Channel; import kotlinx.coroutines.flow.Flow; import kotlinx.coroutines.flow.receiveAsFlow
import java.io.Closeable; import java.io.IOException
interface EngineConnection : Closeable { val lines: Flow<String>; fun send(uciLine: String) }
data class EngineInfo(val label: String, val component: ComponentName)
object OpenExchange {
    const val ACTION = "android.intent.action.RUN"
    const val EXTRA_INPUT = "com.androidchess.ENGINE_INPUT"; const val EXTRA_OUTPUT = "com.androidchess.ENGINE_OUTPUT"
    fun discoverEngines(pm: PackageManager): List<EngineInfo> =
        pm.queryIntentServices(Intent(ACTION), PackageManager.MATCH_ALL).mapNotNull { ri ->
            val si = ri.serviceInfo ?: return@mapNotNull null
            EngineInfo(ri.loadLabel(pm).toString(), ComponentName(si.packageName, si.name))
        }
}
class OpenExchangeEngine(private val context: Context, val component: ComponentName) : EngineConnection {
    private val _lines = Channel<String>(Channel.UNLIMITED)
    override val lines: Flow<String> = _lines.receiveAsFlow()
    private var toEngine: ParcelFileDescriptor.AutoCloseOutputStream? = null
    fun start() {
        val inPipe = ParcelFileDescriptor.createPipe(); val outPipe = ParcelFileDescriptor.createPipe()
        val intent = Intent(OpenExchange.ACTION).apply {
            component = this@OpenExchangeEngine.component
            putExtra(OpenExchange.EXTRA_INPUT, inPipe[0]); putExtra(OpenExchange.EXTRA_OUTPUT, outPipe[1])
        }
        context.startService(intent)
        inPipe[0].close(); outPipe[1].close()
        toEngine = ParcelFileDescriptor.AutoCloseOutputStream(inPipe[1])
        Thread({ try { ParcelFileDescriptor.AutoCloseInputStream(outPipe[0]).bufferedReader().useLines { it.forEach { l -> _lines.trySend(l.trim()) } } } catch (_: IOException) {} }, "oe").start()
    }
    override fun send(uciLine: String) { try { toEngine!!.write((uciLine + "\n").toByteArray()); toEngine!!.flush() } catch (_: IOException) {} }
    override fun close() { try { send("quit") } catch (_: Exception) {}; try { toEngine?.close() } catch (_: Exception) {} }
}
